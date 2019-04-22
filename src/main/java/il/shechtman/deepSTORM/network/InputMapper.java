
package il.shechtman.deepSTORM.network;

import il.shechtman.deepSTORM.network.model.Network;
import il.shechtman.deepSTORM.task.Task;
import net.imagej.Dataset;
import net.imagej.axis.AxisType;

public interface InputMapper extends Task {

	void setMapping(final AxisType[] mapping);

	void run(Dataset input, Network network);

	AxisType[] getMapping();
}
