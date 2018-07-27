
package mpicbg.csbd.tiling.task;

import java.util.List;

import mpicbg.csbd.task.Task;
import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.tiling.Tiling;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public interface InputTiler<T extends RealType<T>> extends Task {

	List<AdvancedTiledView<T>> run(List<RandomAccessibleInterval<T>> input,
		Dataset dataset, Tiling prediction,
		final Tiling.TilingAction[] tilingActions);

}