/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package mpicbg.csbd.commands;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import javax.swing.JOptionPane;

import net.imagej.Dataset;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.type.numeric.RealType;

import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.io.http.HTTPLocation;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;
import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;

import mpicbg.csbd.normalize.PercentileNormalizer;
import mpicbg.csbd.tensorflow.DatasetConverter;
import mpicbg.csbd.tensorflow.DatasetTensorBridge;
import mpicbg.csbd.tensorflow.DefaultDatasetConverter;
import mpicbg.csbd.tensorflow.TensorFlowRunner;

public class CSBDeepCommand< T extends RealType< T > > extends PercentileNormalizer {

	@Parameter( visibility = ItemVisibility.MESSAGE )
	protected String header;

	@Parameter( label = "input data", type = ItemIO.INPUT, initializer = "processDataset" )
	protected Dataset input;

	@Parameter
	protected TensorFlowService tensorFlowService;

	@Parameter
	protected LogService log;

	@Parameter
	protected UIService uiService;

	@Parameter( type = ItemIO.OUTPUT )
	protected Dataset outputImage;

	protected String modelfileUrl;
	protected String modelName;
	protected String modelfileName;
	protected String inputNodeName;
	protected String outputNodeName;

	protected SignatureDef sig;

	protected Graph graph;
	protected SavedModelBundle model;
	protected TensorFlowRunner tfRunner;
	protected DatasetTensorBridge bridge;
	protected boolean hasSavedModel = true;
	protected boolean processedDataset = false;
	
	private DatasetConverter datasetConverter = new DefaultDatasetConverter();

	// Same as
	// tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
	// in Python. Perhaps this should be an exported constant in TensorFlow's Java
	// API.
	protected static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY = "serving_default";

	public CSBDeepCommand() {
		System.loadLibrary( "tensorflow_jni" );
	}

	protected boolean loadModelInputShape( final String inputName ) {

//		System.out.println("loadModelInputShape");

		if ( getGraph() != null ) {
			final Operation input_op = getGraph().operation( inputName );
			if ( input_op != null ) {
				bridge.setInputTensorShape( input_op.output( 0 ).shape() );
				bridge.setMappingDefaults();
				return true;
			}
			System.out.println( "input node with name " + inputName + " not found" );
		}
		return false;
	}

	/*
	 * model can be imported via graphdef or savedmodel, depending on that the
	 * execution graph has different origins
	 */
	protected Graph getGraph() {
		if ( hasSavedModel && ( model == null ) ) { return null; }
		return hasSavedModel ? model.graph() : graph;
	}

	/** Executed whenever the {@link #input} parameter changes. */
	protected void processDataset() {

		if ( !processedDataset ) {
			if ( input != null ) {
				bridge = new DatasetTensorBridge( input );
				processedDataset = true;
			}
		}

	}

	/*
	 * model can be imported via graphdef or savedmodel
	 */
	protected boolean loadGraph() throws MalformedURLException, URISyntaxException {

//		System.out.println("loadGraph");

		final HTTPLocation source = new HTTPLocation( modelfileUrl );
		hasSavedModel = false;
		try {
			graph = tensorFlowService.loadGraph( source, modelName, modelfileName );
		} catch ( TensorFlowException | IOException e ) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	protected void modelChanged() {

//		System.out.println("modelChanged");

		processDataset();

		if ( input == null ) { return; }

		try {
			if ( loadGraph() ) {

				if ( hasSavedModel ) {
					// Extract names from the model signature.
					// The strings "input", "probabilities" and "patches" are meant to be
					// in sync with the model exporter (export_saved_model()) in Python.
					try {
						sig = MetaGraphDef.parseFrom( model.metaGraphDef() ).getSignatureDefOrThrow(
								DEFAULT_SERVING_SIGNATURE_DEF_KEY );
					} catch ( final InvalidProtocolBufferException e ) {
//					e.printStackTrace();
						hasSavedModel = false;
					}
					if ( sig != null && sig.isInitialized() ) {
						if ( sig.getInputsCount() > 0 ) {
							inputNodeName = sig.getInputsMap().keySet().iterator().next();
						}
						if ( sig.getOutputsCount() > 0 ) {
							outputNodeName = sig.getOutputsMap().keySet().iterator().next();
						}
					}
				}

				loadModelInputShape( inputNodeName );

			}
		} catch ( MalformedURLException | URISyntaxException exc ) {
			exc.printStackTrace();
		}
	}

	public void run() {

		if ( input == null ) { return; }
		modelChanged();

		if ( bridge != null ) {
			if ( bridge.getInitialInputTensorShape() != null ) {
				if ( !bridge.isMappingInitialized() ) {
					bridge.setMappingDefaults();
				}
			}
		}
		_run();
	}

	public void runWithMapping( final int[] mapping ) {

		if ( input == null ) { return; }
		modelChanged();

		if ( bridge != null ) {
			if ( bridge.getInitialInputTensorShape() != null ) {
				for ( int i = 0; i < mapping.length; i++ ) {
					bridge.setMapping( i, mapping[ i ] );
				}
			}
		}
		_run();
	}

	private void _run() {

		preprocessing(input);
		testNormalization(input, uiService);

		try (
				final Tensor image = datasetConverter.datasetToTensor(input, bridge, this);) {
			outputImage = datasetConverter.tensorToDataset(
					TensorFlowRunner.executeGraph( getGraph(), image, inputNodeName, outputNodeName ), 
					bridge);
			if ( outputImage != null ) {
				outputImage.setName( "CSBDeepened_" + input.getName() );
				uiService.show( outputImage );
			}
		}

//		uiService.show(arrayToDataset(datasetToArray(input)));

	}


	public void showError( final String errorMsg ) {
		JOptionPane.showMessageDialog(
				null,
				errorMsg,
				"Error",
				JOptionPane.ERROR_MESSAGE );
	}

}