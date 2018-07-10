package mpicbg.csbd.normalize.task;

import java.util.List;

import mpicbg.csbd.task.Task;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface InputNormalizer< T extends RealType< T > & NativeType<T>> extends Task {

	List<RandomAccessibleInterval<T>>
			run(List<RandomAccessibleInterval<T>> input, OpService opService);

}
