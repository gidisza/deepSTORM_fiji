/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2018 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package mpicbg.csbd.commands;

import mpicbg.csbd.network.ImageTensor;
import mpicbg.csbd.network.Network;
import mpicbg.csbd.network.task.*;
import mpicbg.csbd.network.tensorflow.TensorFlowNetwork;
import mpicbg.csbd.normalize.task.DefaultInputNormalizer;
import mpicbg.csbd.normalize.task.InputNormalizer;
import mpicbg.csbd.task.Task;
import mpicbg.csbd.task.TaskForceManager;
import mpicbg.csbd.task.TaskManager;
import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.tiling.DefaultTiling;
import mpicbg.csbd.tiling.Tiling;
import mpicbg.csbd.tiling.task.DefaultInputTiler;
import mpicbg.csbd.tiling.task.DefaultOutputTiler;
import mpicbg.csbd.tiling.task.InputTiler;
import mpicbg.csbd.tiling.task.OutputTiler;
import mpicbg.csbd.ui.MappingDialog;
import mpicbg.csbd.util.IOHelper;
import mpicbg.csbd.util.task.DefaultInputProcessor;
import mpicbg.csbd.util.task.DefaultOutputProcessor;
import mpicbg.csbd.util.task.InputProcessor;
import mpicbg.csbd.util.task.OutputProcessor;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.ops.OpService;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.Cancelable;
import org.scijava.Disposable;
import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Plugin(type = Command.class, menuPath = "Plugins>CSBDeep>Run your network")
public class GenericNetwork implements
		Command, Cancelable, Initializable, Disposable
{

	@Parameter(type = ItemIO.INPUT, initializer = "input")
	public Dataset input;

	@Parameter
	protected boolean normalizeInput = true;
	@Parameter
	protected float percentileBottom = 3.0f;
	@Parameter
	protected float percentileTop = 99.8f;

	protected float min = 0;
	protected float max = 1;

	@Parameter(label = "Clip normalization")
	protected boolean clip = false;

	@Parameter(label = "Number of tiles", min = "1")
	protected int nTiles = 8;

	@Parameter(label = "Overlap between tiles", min = "0", stepSize = "16")
	protected int overlap = 32;

	@Parameter(label = "Batch axis", choices = {"no batching", "X", "Y", "Z", "TIME", "CHANNEL"})
	protected String batchAxis = "no batching";

	@Parameter(label = "Batch size", min = "1")
	protected int batchSize = 1;

	@Parameter(label = "Import model (.zip)", callback = "modelFileChanged",
			initializer = "modelFileInitialized", persist = false, required = false)
	private File modelFile;

	@Parameter(label = "Import model (.zip) from URL", callback = "modelUrlChanged",
			initializer = "modelUrlChanged", persist = false, required = false)
	protected String modelUrl;

	@Parameter(label = "Adjust mapping of TF network input",
			callback = "openTFMappingDialog")
	private Button changeTFMapping;

	@Parameter(type = ItemIO.OUTPUT)
	protected List<Dataset> output = new ArrayList<>();

	@Parameter
	protected LogService log;

	@Parameter
	protected TensorFlowService tensorFlowService;

	@Parameter
	protected DatasetService datasetService;

	@Parameter
	protected UIService uiService;

	@Parameter
	protected OpService opService;

	@Parameter
	private PrefService prefService;

	protected String modelName;
	protected String inputNodeName = "input";
	protected String outputNodeName = "output";
	protected int blockMultiple = 8;

	protected TaskManager taskManager;

	protected Network network;
	protected Tiling tiling;

	protected InputProcessor inputProcessor;
	protected InputMapper inputMapper;
	protected InputNormalizer inputNormalizer;
	protected InputTiler inputTiler;
	protected ModelLoader modelLoader;
	protected ModelExecutor modelExecutor;
	protected OutputTiler outputTiler;
	protected OutputProcessor outputProcessor;

	protected boolean initialized = false;

	private ExecutorService pool = null;
	private Future<?> future;

	protected String cacheName;
	protected String modelFileKey;
	private String modelFileUrl;

	private boolean modelChangeCallbackCalled = false;

	private ExecutorService modelLoadingPool = null;
	private Future<?> modelLoadingFuture = null;

	protected void openTFMappingDialog() {
		System.out.println("openTFMappingDialog");
		finishModelLoading();
		MappingDialog.create(network.getInputNode(), network.getOutputNode());
	}

	/** Executed whenever the {@link #modelFile} parameter is initialized. */
	protected void modelFileInitialized() {
		final String p_modelfile = prefService.get(String.class, modelFileKey, "");
		if (p_modelfile != "") {
			modelFile = new File(p_modelfile);
			if(modelFile != null) {
				modelFileChanged();
			}
		}
	}

	private void updateCacheName() {
		if(modelFile != null) {
			try {
				FileInputStream fis = new FileInputStream(modelFile);
				String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
				cacheName = this.getClass().getSimpleName() + "_" + md5;
				savePreferences();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected void modelFileChanged() {
		if(!modelChangeCallbackCalled) {
			modelChangeCallbackCalled = true;
			modelUrl = null;
			if (modelFile != null && modelFile.exists()) {
				modelFileUrl = modelFile.getAbsolutePath();
				updateCacheName();
				modelChanged();
			}else {
				modelChangeCallbackCalled = false;
			}
		}
	}

	protected void modelUrlChanged() {
		if(!modelChangeCallbackCalled) {
			modelChangeCallbackCalled = true;
			modelFile = null;
			if(modelUrl.length() > new String("https://").length()) {
				if (IOHelper.urlExists(modelUrl)) {
					modelFileUrl = modelUrl;
					modelChanged();
					return;
				}
			}
		}
		modelChangeCallbackCalled = false;
	}

	protected void modelChanged() {
		System.out.println("modelChanged");
		restartPool();
		modelLoadingFuture = modelLoadingPool.submit(() -> {
			savePreferences();
			if (initialized) {
				network.dispose();
			} else {
				tryToInitialize();
			}
			prepareInputAndNetwork();
			modelChangeCallbackCalled = false;
		});
	}

	private void restartPool() {
		if(modelLoadingPool != null) {
			modelLoadingPool.shutdownNow();
		}
		modelLoadingPool = Executors.newSingleThreadExecutor();
	}

	protected void finishModelLoading() {
		if(modelLoadingFuture != null) {
			try {
				modelLoadingFuture.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		if(modelLoadingPool != null) {
			modelLoadingPool.shutdown();
			modelLoadingPool = null;
		}
	}

	@Override
	public void initialize() {
		initialized = true;
		cacheName = this.getClass().getSimpleName();
		modelFileKey = this.getClass().getSimpleName() + "_modelfile";
		initTasks();
		initTaskManager();
		initNetwork();
	}

	protected void tryToInitialize() {
		if (!initialized) {
			initialize();
		}
	}

	protected boolean initNetwork() {
		network = new TensorFlowNetwork(tensorFlowService, datasetService,
			modelExecutor);
		if(network.libraryLoaded()) {
			network.testGPUSupport();
			if(!network.supportsGPU()) taskManager.noGPUFound();
		}else {
			return false;
		}
		return true;
	}

	protected void initTasks() {
		inputMapper = initInputMapper();
		inputProcessor = initInputProcessor();
		inputNormalizer = initInputNormalizer();
		inputTiler = initInputTiler();
		modelLoader = initModelLoader();
		modelExecutor = initModelExecutor();
		outputTiler = initOutputTiler();
		outputProcessor = initOutputProcessor();
	}

	protected void initTaskManager() {
		final TaskForceManager tfm = new TaskForceManager(isHeadless(), log);
		tfm.initialize();
		tfm.createTaskForce("Preprocessing", modelLoader, inputMapper,
			inputProcessor, inputNormalizer);
		tfm.createTaskForce("Tiling", inputTiler);
		tfm.createTaskForce("Execution", modelExecutor);
		tfm.createTaskForce("Postprocessing", outputTiler, outputProcessor);
		taskManager = tfm;
	}

	protected InputMapper initInputMapper() {
		return new DefaultInputMapper();
	}

	protected InputProcessor initInputProcessor() {
		return new DefaultInputProcessor();
	}

	protected InputNormalizer initInputNormalizer() {
		return new DefaultInputNormalizer();
	}

	protected InputTiler initInputTiler() {
		return new DefaultInputTiler();
	}

	protected ModelLoader initModelLoader() {
		return new DefaultModelLoader();
	}

	protected ModelExecutor initModelExecutor() {
		return new DefaultModelExecutor();
	}

	protected OutputTiler initOutputTiler() {
		return new DefaultOutputTiler();
	}

	protected OutputProcessor initOutputProcessor() {
		return new DefaultOutputProcessor();
	}

	public void run() {

		if (noInputData()) return;

		pool = Executors.newSingleThreadExecutor();

		future = pool.submit(() -> {

			updateCacheName();
			savePreferences();
			tryToInitialize();

			taskManager.finalizeSetup();

			prepareInputAndNetwork();

			//find out what to do about different dimensions
//			checkAndResolveDimensionReduction();

			final Dataset normalizedInput;
			if (doInputNormalization()) {
				setupNormalizer();
				normalizedInput = inputNormalizer.run(getInput(), opService,
						datasetService);
			} else {
				normalizedInput = getInput();
			}

			final List<RandomAccessibleInterval> processedInput = inputProcessor.run(
					normalizedInput);

			log("INPUT NODE: ");
			network.getInputNode().printMapping();
			log("OUTPUT NODE: ");
			network.getOutputNode().printMapping();

			initTiling();
			try {
				final List<AdvancedTiledView<FloatType>> tiledOutput =
						tryToTileAndRunNetwork(processedInput);
				if(tiledOutput != null) {
					final List<RandomAccessibleInterval<FloatType>> output = outputTiler.run(
							tiledOutput, tiling, getAxesArray(network.getOutputNode()));
					for (AdvancedTiledView obj : tiledOutput) {
						obj.dispose();
					}
					this.output.clear();
					this.output.addAll(outputProcessor.run(output, getInput(), network,
							datasetService));
				}
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			}

		});

		try {
			if(future != null) future.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		dispose();

	}

	protected void setupNormalizer() {
		((DefaultInputNormalizer) inputNormalizer).getNormalizer().setup(
				new float[] { percentileBottom, percentileTop }, new float[] { min,
						max }, clip);
	}

	protected boolean doInputNormalization() {
		return normalizeInput;
	}

	protected void prepareInputAndNetwork() {

		if(modelFile != null) {
			modelFileUrl = modelFile.getAbsolutePath();
		}
		modelName = cacheName;
		modelLoader.run(modelName, network, modelFileUrl, inputNodeName,
			outputNodeName, getInput());
		inputMapper.run(getInput(), network);
		checkAndResolveDimensionReduction();
	}

	private void checkAndResolveDimensionReduction() {
		for (int i = 0; i < getInput().numDimensions(); i++) {
			AxisType axis = getInput().axis(i).type();
			if (!network.getOutputNode().getNodeAxes().contains(axis)) {
				// log("Network input node axis " + axis.getLabel() + " not present in
				// output node, will be reduced");
				network.setDoDimensionReduction(true, axis);
			}
		}
		network.doDimensionReduction();
	}

	private void savePreferences() {
		if(modelFile != null) {
			prefService.put(String.class, modelFileKey, modelFile.getAbsolutePath());
		}
	}

	@Override
	public void dispose() {
		if (taskManager != null) {
			taskManager.close();
		}
		if (network != null) {
			network.dispose();
		}
		if(pool != null) {
			pool.shutdown();
		}
		pool = null;
	}

	private AxisType[] getAxesArray(final ImageTensor outputNode) {
		int numDim = outputNode.numDimensions();
		boolean addChannel = false;
		if (numDim < outputNode.getNodeShape().length && outputNode
			.getNodeShape()[outputNode.getNodeShape().length - 1] > 1)
		{
			addChannel = true;
			numDim++;
		}
		final AxisType[] res = new AxisType[numDim];
		for (int i = 0; i < outputNode.numDimensions(); i++) {
			res[i] = outputNode.getAxisByDatasetDim(i);
		}
		if (addChannel) {
			res[res.length - 1] = Axes.CHANNEL;
		}
		return res;
	}

	protected void initTiling() {
		tiling = new DefaultTiling(nTiles, batchSize, blockMultiple, overlap);
	}

	private List<AdvancedTiledView<FloatType>> tryToTileAndRunNetwork(
		final List<RandomAccessibleInterval> normalizedInput)
		throws OutOfMemoryError
	{
		List<AdvancedTiledView<FloatType>> tiledOutput = null;

		boolean isOutOfMemory = true;
		boolean canHandleOutOfMemory = true;

		while (isOutOfMemory && canHandleOutOfMemory) {
			try {
				final List<AdvancedTiledView> tiledInput = inputTiler.run(
					normalizedInput, getAxesArray(getInput()), tiling, getTilingActions());
				nTiles = tiling.getTilesNum();
				if(tiledInput != null) {
					tiledOutput = modelExecutor.run(tiledInput, network);
				}
				isOutOfMemory = false;

			}
			catch (final OutOfMemoryError e) {
				isOutOfMemory = true;
				canHandleOutOfMemory = tryHandleOutOfMemoryError();
			}
		}

		if (isOutOfMemory) throw new OutOfMemoryError(
			"Out of memory exception occurred. Plugin exit.");

		return tiledOutput;
	}

	private AxisType[] getAxesArray(Dataset input) {
		AxisType[] res = new AxisType[input.numDimensions()];
		for (int i = 0; i < input.numDimensions(); i++) {
			res[i] = input.axis(i).type();
		}
		return res;
	}

	protected Tiling.TilingAction[] getTilingActions() {
		Tiling.TilingAction[] actions = new Tiling.TilingAction[getInput()
			.numDimensions()];
		Arrays.fill(actions, Tiling.TilingAction.NO_TILING);
		for (int i = 0; i < actions.length; i++) {
			AxisType type = getInput().axis(i).type();
			if (type.isSpatial()) {
				actions[i] = Tiling.TilingAction.TILE_WITH_PADDING;
			}
		}
		return actions;
	}

	public void setMapping(final AxisType[] mapping) {
		inputMapper.setMapping(mapping);
	}

	public AxisType[] getMapping() {
		return inputMapper.getMapping();
	}

	private boolean noInputData() {
		return getInput() == null;
	}

	private boolean tryHandleOutOfMemoryError() {
		// We expect it to be an out of memory exception and
		// try it again with more tiles or smaller batches.
		final Task modelExecutorTask = modelExecutor;
		nTiles = tiling.getTilesNum();
		int oldNTiles = nTiles;
		int oldBatchesSize = batchSize;

		handleOutOfMemoryError();
		modelExecutorTask.logError(
			"Out of memory exception occurred. Trying with " + nTiles +
				" tiles, batch size " + batchSize + " and overlap " + overlap + "...");
		initTiling();
		nTiles = tiling.getTilesNum();

		if(oldNTiles == nTiles && oldBatchesSize == batchSize) {
			modelExecutorTask.setFailed();
			return false;
		}

		modelExecutorTask.startNewIteration();
		inputTiler.addIteration();
		return true;
	}

	protected void handleOutOfMemoryError() {
		batchSize /= 2;
		if (batchSize < 1) {
			nTiles *= 2;
//				if (overlap == 0) return false;
//				overlap *= 0.5;
//				if (overlap < 2) overlap = 0;
		}
	}

	protected static void showError(final String errorMsg) {
		JOptionPane.showMessageDialog(null, errorMsg, "Error",
			JOptionPane.ERROR_MESSAGE);
	}

	public Dataset getInput() {
		return input;
	}

	@Override
	public String getCancelReason() {
		return null;
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void cancel(final String reason) {
		if(future != null) {
			future.cancel(true);
		}
		if(pool != null) {
			pool.shutdownNow();
		}
		dispose();
	}

	protected void log(final String msg) {
		if (taskManager != null) {
			taskManager.log(msg);
		}
		else {
			System.out.println(msg);
		}
	}

	protected boolean isHeadless() {
		return uiService.isHeadless();
	}

	/**
	 * This main function serves for development purposes. It allows you to run
	 * the plugin immediately out of your integrated development environment
	 * (IDE).
	 *
	 * @param args whatever, it's ignored
	 * @throws Exception
	 */
	public static void main(final String... args) throws Exception {

		final ImageJ ij = new ImageJ();

		ij.launch(args);

		// ask the user for a file to open
		final File file = new File("/home/random/Development/imagej/plugins/CSBDeep-data/net_tubulin/input.tif");

		if (file != null && file.exists()) {
			// load the dataset
			final Dataset dataset = ij.scifio().datasetIO().open(file
					.getAbsolutePath());

			// show the image
			ij.ui().show(dataset);

			// invoke the plugin
			ij.command().run(GenericNetwork.class, true);
		}

	}

}
