package mpicbg.csbd.normalize.task;

import java.util.List;
import java.util.stream.Collectors;

import mpicbg.csbd.normalize.Normalizer;
import mpicbg.csbd.normalize.PercentileNormalizer;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import mpicbg.csbd.task.DefaultTask;

public class DefaultInputNormalizer extends DefaultTask implements InputNormalizer {

	private Normalizer< FloatType > normalizer = new PercentileNormalizer<>();

	@Override
	public List< RandomAccessibleInterval< FloatType > >
			run(final List< RandomAccessibleInterval< FloatType > > input, OpService opService) {

		setStarted();

		final List< RandomAccessibleInterval< FloatType > > output =
				input.stream().map( image -> normalizeInput( image, opService) ).collect(
						Collectors.toList() );

		setFinished();

		return output;

	}

	protected RandomAccessibleInterval< FloatType >
			normalizeInput( final RandomAccessibleInterval< FloatType > input, OpService opService ) {

		log( "Normalize .. " );

		return normalizer.normalize( input, opService );
	}

	public Normalizer<FloatType> getNormalizer() {
		return normalizer;
	}

}
