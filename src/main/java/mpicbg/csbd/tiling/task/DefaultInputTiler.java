
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

public class DefaultInputTiler<T extends RealType<T>> extends DefaultTask
	implements InputTiler<T>
{

	@Override
	public List<AdvancedTiledView<T>> run(
		final List<RandomAccessibleInterval<T>> input, final Dataset dataset,
		final Tiling tiling, final Tiling.TilingAction[] tilingActions)
	{

		setStarted();

		final List output = input.stream().map(image -> tiling.preprocess(image,
			dataset, tilingActions, this)).collect(Collectors.toList());

		setFinished();

		return output;

	}

}
