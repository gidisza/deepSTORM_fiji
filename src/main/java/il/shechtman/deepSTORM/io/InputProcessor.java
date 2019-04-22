
package il.shechtman.deepSTORM.io;

import java.util.List;

import il.shechtman.deepSTORM.network.model.Network;
import il.shechtman.deepSTORM.task.Task;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public interface InputProcessor<T extends RealType<T>> extends Task {

	List<RandomAccessibleInterval<T>> run(Dataset input, Network network);

}
