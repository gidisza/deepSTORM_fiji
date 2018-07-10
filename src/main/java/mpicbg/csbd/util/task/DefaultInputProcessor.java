package mpicbg.csbd.util.task;

import mpicbg.csbd.task.DefaultTask;
import mpicbg.csbd.util.DatasetHelper;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.convert.ConvertService;

import java.util.ArrayList;
import java.util.List;

public class DefaultInputProcessor< T extends RealType< T >> extends DefaultTask implements InputProcessor {

	@Override
	public List< RandomAccessibleInterval< T > > run( final Dataset input ) {

		final List< RandomAccessibleInterval< T > > output = new ArrayList<>();

		setStarted();

		log("Dataset type: " + input.getTypeLabelLong() );
		DatasetHelper.logDim( this, "Dataset dimensions", input );

		RandomAccessibleInterval<T> rai = (RandomAccessibleInterval<T>) input.getImgPlus();

		output.add( rai );

		setFinished();

		return output;

	}

}
