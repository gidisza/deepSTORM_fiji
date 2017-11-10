package mpicbg.csbd.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.IntStream;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import mpicbg.csbd.ui.CSBDeepProgress;

@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>Isotropic Reconstruction - Retina", headless = true )
public class NetIso< T extends RealType< T > > extends CSBDeepCommand< T > implements Command {

	@Parameter( label = "Scale Z", min = "1", max = "15" )
	protected float scale = 10.2f;

	@Parameter( label = "Batch size", min = "1" )
	protected int batchSize = 10;

	@Override
	public void initialize() {

		super.initialize();

		modelFileUrl = "http://csbdeep.bioimagecomputing.com/model-iso.zip";
		modelName = "net_iso";

	}

	public static void main( final String[] args ) throws IOException {
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		ij.launch( args );

		// ask the user for a file to open
		final File file = ij.ui().chooseFile( null, "open" );

		if ( file != null && file.exists() ) {
			// load the dataset
			final Dataset dataset = ij.scifio().datasetIO().open( file.getAbsolutePath() );

			// show the image
			ij.ui().show( dataset );

			// invoke the plugin
			ij.command().run( NetIso.class, true );
		}
	}

	@Override
	public void run() {
		try {
			validateInput(
					input,
					"4D image with dimension order X-Y-C-Z and two channels",
					OptionalLong.empty(),
					OptionalLong.empty(),
					OptionalLong.of( 2 ),
					OptionalLong.empty() );
			runModel();
		} catch ( final IOException e ) {
			showError( e.getMessage() );
		}
	}

	private void runModel() {
		if ( input == null ) { return; }
		modelChanged();

		final AxisType[] mapping = { Axes.Z, Axes.Y, Axes.X, Axes.CHANNEL };
		setMapping( mapping );

		final int dimChannel = input.dimensionIndex( Axes.CHANNEL );
		final int dimX = input.dimensionIndex( Axes.X );
		final int dimY = input.dimensionIndex( Axes.Y );
		final int dimZ = input.dimensionIndex( Axes.Z );

		initGui();

		initModel();
		progressWindow.setNumRounds( 2 );
		progressWindow.setStepStart( CSBDeepProgress.STEP_PREPROCRESSING );

		progressWindow.addLog( "Normalize input.. " );

		final int n = input.numDimensions();

		// ========= NORMALIZATION
		// TODO maybe there is a better solution than splitting the image, normalizing each channel and combining it again.
		final IntervalView< T > channel0 =
				Views.hyperSlice( input.typedImg( ( T ) input.firstElement() ), dimChannel, 0 );
		final IntervalView< T > channel1 =
				Views.hyperSlice( input.typedImg( ( T ) input.firstElement() ), dimChannel, 1 );

		prepareNormalization( channel0 );
		final Img< FloatType > normalizedChannel0 = normalizeImage( channel0 );

		prepareNormalization( channel1 );
		final Img< FloatType > normalizedChannel1 = normalizeImage( channel1 );

		final RandomAccessibleInterval< FloatType > normalizedInput = Views.permute(
				Views.stack( normalizedChannel0, normalizedChannel1 ),
				n - 1,
				dimChannel );

		progressWindow.addLog( "Upsampling.." );

		// ========= UPSAMPLING
		final RealRandomAccessible< FloatType > interpolated =
				Views.interpolate(
						Views.extendBorder( normalizedInput ),
						new NLinearInterpolatorFactory<>() );

		// Affine transformation to scale the Z axis
		final double s = scale;
		final double[] scales = IntStream.range( 0, n ).mapToDouble( i -> i == dimZ ? s : 1 ).toArray();
		final AffineGet scaling = new Scale( scales );

		// Scale min and max to create an interval afterwards
		final double[] targetMin = new double[ n ];
		final double[] targetMax = new double[ n ];
		scaling.apply( Intervals.minAsDoubleArray( normalizedInput ), targetMin );
		scaling.apply( Intervals.maxAsDoubleArray( normalizedInput ), targetMax );

		// Apply the transformation
		final RandomAccessible< FloatType > scaled = RealViews.affine( interpolated, scaling );
		final RandomAccessibleInterval< FloatType > upsampled = Views.interval(
				scaled,
				Arrays.stream( targetMin ).mapToLong( d -> ( long ) Math.ceil( d ) ).toArray(),
				Arrays.stream( targetMax ).mapToLong( d -> ( long ) Math.floor( d ) ).toArray() );

		// ========== ROTATION

		progressWindow.addLog( "Rotate around Y.." );

		// Create the first rotated image
		final RandomAccessibleInterval< FloatType > rotated0 = Views.permute( upsampled, dimX, dimZ );
		progressWindow.addLog( "Rotate around X.." );

		// Create the second rotated image
		final RandomAccessibleInterval< FloatType > rotated1 = Views.permute( rotated0, dimY, dimZ );

		final List< RandomAccessibleInterval< FloatType > > result0 = new ArrayList<>();
		final List< RandomAccessibleInterval< FloatType > > result1 = new ArrayList<>();

		runBatches( rotated0, rotated1, result0, result1 );

		resultDatasets = new ArrayList<>();
		for ( int i = 0; i + 1 < result0.size() && i + 1 < result1.size(); i += 2 ) {
			//prediction for ZY rotation
			RandomAccessibleInterval< FloatType > res0_pred =
					Views.stack( result0.get( i ), result0.get( i + 1 ) );

			//prediction for ZX rotation
			RandomAccessibleInterval< FloatType > res1_pred =
					Views.stack( result1.get( i ), result1.get( i + 1 ) );

			// rotate output stacks back
			//TODO the rotation dimensions are not dynamic yet, we should use variables
			res0_pred = Views.permute( res0_pred, 0, 2 );
			res1_pred = Views.permute( res1_pred, 1, 2 );
			res1_pred = Views.permute( res1_pred, 0, 2 );

			printDim( "res0_pred rotated back", res0_pred );
			printDim( "res1_pred rotated back", res1_pred );

			progressWindow.addLog( "Merge output stacks.." );

			// Calculate the geometric mean of the two predictions
			final RandomAccessibleInterval< FloatType > prediction =
					ArrayImgs.floats( Intervals.dimensionsAsLongArray( res0_pred ) );
			pointwiseGeometricMean(
					Views.iterable( res0_pred ),
					res1_pred,
					prediction );
			printDim( "prediction", prediction );

			final String name = OUTPUT_NAMES.length > i / 2 ? OUTPUT_NAMES[ i / 2 ] : GENERIC_OUTPUT_NAME + i / 2;
			resultDatasets.add( wrapIntoDataset( name, Views.permute( prediction, 2, 3 ) ) );
		}
		if ( !resultDatasets.isEmpty() ) {
			progressWindow.addLog( "All done!" );
			progressWindow.setCurrentStepDone();
		} else {
			progressWindow.setCurrentStepFail();
		}
	}

	private void runBatches(
			final RandomAccessibleInterval< FloatType > rotated0,
			final RandomAccessibleInterval< FloatType > rotated1,
			final List< RandomAccessibleInterval< FloatType > > result0,
			final List< RandomAccessibleInterval< FloatType > > result1 ) {

		result0.clear();
		result1.clear();

		try {

			result0.addAll( pool.submit(
					new BatchedTiledPrediction( rotated0, bridge, model, progressWindow, nTiles, 4, overlap, batchSize ) ).get() );

			progressWindow.setNextRound();

			result1.addAll( pool.submit(
					new BatchedTiledPrediction( rotated1, bridge, model, progressWindow, nTiles, 4, overlap, batchSize ) ).get() );

		} catch ( RejectedExecutionException | InterruptedException exc ) {
			return;
		} catch ( final ExecutionException exc ) {
			exc.printStackTrace();

			// We expect it to be an out of memory exception and
			// try it again with a smaller batch size.
			batchSize /= 2;
			// Check if the batch size is at 1 already
			if ( batchSize < 1 ) {
				progressWindow.setCurrentStepFail();
				return;
			}
			progressWindow.addError( "Out of memory exception occurred. Trying with batch size: " + batchSize );
			progressWindow.addRounds( 1 );
			progressWindow.setNextRound();
			runBatches( rotated0, rotated1, result0, result1 );
			return;
		}

	}

	private static < T extends RealType< T >, U extends RealType< U >, V extends RealType< V > > void pointwiseGeometricMean(
			final IterableInterval< T > in1,
			final RandomAccessibleInterval< U > in2,
			final RandomAccessibleInterval< V > out ) {
		final Cursor< T > i1 = in1.cursor();
		final RandomAccess< U > i2 = in2.randomAccess();
		final RandomAccess< V > o = out.randomAccess();

		while ( i1.hasNext() ) {
			i1.fwd();
			i2.setPosition( i1 );
			o.setPosition( i1 );
			o.get().setReal( Math.sqrt( i1.get().getRealFloat() * i2.get().getRealFloat() ) );
		}
	}

}
