package mpicbg.csbd.tiling.task;

import java.util.List;
import java.util.stream.Collectors;

import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import mpicbg.csbd.task.DefaultTask;
import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.tiling.Tiling;

public class DefaultInputTiler< T extends RealType< T >> extends DefaultTask implements InputTiler<T> {

	@Override
	public List< AdvancedTiledView< T > > run(
			final List< RandomAccessibleInterval< T > > input,
			final Dataset dataset,
			final Tiling prediction ) {

		setStarted();

		final List< AdvancedTiledView< T > > output =
				input.stream().map( image -> prediction.preprocess( image, dataset, this ) )//
						.collect( Collectors.toList() );

		setFinished();

		return output;

	}

}
