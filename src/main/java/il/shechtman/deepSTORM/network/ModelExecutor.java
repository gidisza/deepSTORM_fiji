
package il.shechtman.deepSTORM.network;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.scijava.Cancelable;

import il.shechtman.deepSTORM.network.model.Network;
import il.shechtman.deepSTORM.task.Task;
import il.shechtman.deepSTORM.tiling.AdvancedTiledView;
import net.imglib2.type.numeric.RealType;

public interface ModelExecutor<T extends RealType<T>> extends Task, Cancelable {

	List<AdvancedTiledView<T>> run(List<AdvancedTiledView<T>> input,
		Network network) throws ExecutionException;

}
