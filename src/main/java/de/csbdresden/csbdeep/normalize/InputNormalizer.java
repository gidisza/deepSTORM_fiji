
package de.csbdresden.csbdeep.normalize;

import de.csbdresden.csbdeep.task.Task;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ops.OpService;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface InputNormalizer<T extends RealType<T> & NativeType<T>> extends
	Task
{

	Dataset run(Dataset input, OpService opService,
		DatasetService datasetService);

}
