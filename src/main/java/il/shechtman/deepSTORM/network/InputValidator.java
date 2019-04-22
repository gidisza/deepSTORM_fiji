
package il.shechtman.deepSTORM.network;

import il.shechtman.deepSTORM.network.model.Network;
import il.shechtman.deepSTORM.task.Task;
import net.imagej.Dataset;
import net.imglib2.exception.IncompatibleTypeException;

public interface InputValidator extends Task {

	void run(Dataset input, Network network) throws IncompatibleTypeException;

}
