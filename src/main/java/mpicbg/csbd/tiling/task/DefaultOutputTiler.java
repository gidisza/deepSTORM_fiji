package mpicbg.csbd.tiling.task;

import java.util.List;
import java.util.stream.Collectors;

import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import mpicbg.csbd.task.DefaultTask;
import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.tiling.Tiling;

public class DefaultOutputTiler< T extends RealType< T >> extends DefaultTask implements OutputTiler<T> {

	@Override
	public List< RandomAccessibleInterval< T > > run(
			final List< AdvancedTiledView< T > > input,
			final Tiling tiling,
			final AxisType[] axisTypes ) {

		setStarted();

		final List< RandomAccessibleInterval< T > > output =
				input.stream().map( image -> tiling.postprocess( this, image, axisTypes ) ).collect(
						Collectors.toList() );

		setFinished();

		return output;
	}

}
