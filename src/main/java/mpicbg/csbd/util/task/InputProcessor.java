package mpicbg.csbd.util.task;

import mpicbg.csbd.task.Task;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import java.util.List;

public interface InputProcessor< T extends RealType< T >> extends Task {

	List< RandomAccessibleInterval< T > > run( Dataset input );

}
