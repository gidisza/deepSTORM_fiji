
package mpicbg.csbd.network.task;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.DefaultTask;
import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.util.DatasetHelper;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class DefaultModelExecutor<T extends RealType<T>> extends DefaultTask
	implements ModelExecutor<T>
{

	protected static String PROGRESS_CANCELED = "";
	protected ExecutorService pool = null;

	@Override
	public List<AdvancedTiledView<T>> run(final List<AdvancedTiledView<T>> input,
		final Network network) throws OutOfMemoryError
	{
		setStarted();
		if (input.size() > 0) {
			DatasetHelper.logDim(this, "Network input size", input.get(0)
				.randomAccess().get());
		}

		setCurrentStep(0);
		int numSteps = 0;
		for (AdvancedTiledView<T> tile : input) {
			int steps = 1;
			for (int i = 0; i < tile.numDimensions(); i++) {
				steps *= tile.dimension(i);
			}
			numSteps += steps;
		}
		network.resetTileCount();
		setNumSteps(numSteps);

		pool = Executors.newWorkStealingPool();
		final List<AdvancedTiledView<T>> output = new ArrayList<>();
		for (AdvancedTiledView<T> tile : input) {
			output.add(run(tile, network));
		}
		// TODO why does this not work?
		// final List< AdvancedTiledView< T > > output =
		// input.stream().map( tile -> run( tile, network ) ).collect(
		// Collectors.toList() );
		pool.shutdown();
		if (output.size() > 0) {
			DatasetHelper.logDim(this, "Network output size", output.get(0)
				.getProcessedTiles().get(0));
		}
		setFinished();
		return output;
	}

	private AdvancedTiledView<T> run(final AdvancedTiledView<T> input,
		final Network network) throws OutOfMemoryError
	{

		input.getProcessedTiles().clear();

		try {
			network.setTiledView(input);
			input.getProcessedTiles().addAll((List<RandomAccessibleInterval<T>>) pool
				.submit(network).get());
		}
		catch (final ExecutionException | IllegalStateException exc) {
			exc.printStackTrace();
			setIdle();
			throw new OutOfMemoryError();
		}
		catch (final InterruptedException exc) {
			logError(PROGRESS_CANCELED);
			setFailed();
			cancel();
			return null;
		}

		return input;
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void cancel(final String reason) {
		if (pool != null && !pool.isShutdown()) {
			pool.shutdownNow();
		}
	}

	@Override
	public String getCancelReason() {
		return null;
	}

}
