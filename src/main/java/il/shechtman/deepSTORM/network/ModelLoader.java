
package il.shechtman.deepSTORM.network;

import java.io.FileNotFoundException;

import il.shechtman.deepSTORM.network.model.Network;
import il.shechtman.deepSTORM.task.Task;
import net.imagej.Dataset;

public interface ModelLoader extends Task {

	void run(String modelName, Network network, String modelFileUrl, Dataset input) throws FileNotFoundException;

}
