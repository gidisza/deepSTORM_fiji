package mpicbg.csbd.normalize.task;

import java.util.List;
import java.util.stream.Collectors;

import mpicbg.csbd.normalize.Normalizer;
import mpicbg.csbd.normalize.PercentileNormalizer;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import mpicbg.csbd.task.DefaultTask;

public class DefaultInputNormalizer< T extends RealType< T > & NativeType<T>> extends DefaultTask implements InputNormalizer<T> {

	private Normalizer< T > normalizer = new PercentileNormalizer<T>();

	@Override
	public List<RandomAccessibleInterval<T>> run(List<RandomAccessibleInterval<T>> input, OpService opService) {

		setStarted();

		final List< RandomAccessibleInterval<T> > output =
				input.stream().map( image -> normalizeInput( image, opService) ).collect(
						Collectors.toList() );

		setFinished();

		return output;

	}

	protected RandomAccessibleInterval< T >
			normalizeInput( final RandomAccessibleInterval< T > input, OpService opService ) {

		log( "Normalize .. " );

		return normalizer.normalize( input, opService );
	}

	public Normalizer<T> getNormalizer() {
		return normalizer;
	}

}
