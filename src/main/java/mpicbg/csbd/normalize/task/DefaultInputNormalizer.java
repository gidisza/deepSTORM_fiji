
package mpicbg.csbd.normalize.task;

import java.util.List;
import java.util.stream.Collectors;

import mpicbg.csbd.normalize.Normalizer;
import mpicbg.csbd.normalize.PercentileNormalizer;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import mpicbg.csbd.task.DefaultTask;
import net.imglib2.type.numeric.real.FloatType;

public class DefaultInputNormalizer<T extends RealType<T> & NativeType<T>>
	extends DefaultTask implements InputNormalizer<T>
{

	private Normalizer normalizer = new PercentileNormalizer<>();

	@Override
	public Dataset run(Dataset input, OpService opService,
		DatasetService datasetService)
	{

		setStarted();

		log("Normalize .. ");

		final Dataset output = normalizer.normalize(input, opService,
			datasetService);

		setFinished();

		return output;

	}

	public Normalizer getNormalizer() {
		return normalizer;
	}

}
