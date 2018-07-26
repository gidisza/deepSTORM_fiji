package mpicbg.csbd.network;

import mpicbg.csbd.imglib2.TiledView;
import mpicbg.csbd.task.Task;
import net.imagej.Dataset;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.Disposable;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.Callable;

public interface Network< T extends RealType< T >> extends Callable< List< RandomAccessibleInterval< T > > >, Disposable {

	void loadLibrary();

	boolean loadModel( String pathOrURL, String modelName ) throws FileNotFoundException;

	void preprocess();

	RandomAccessibleInterval< T >
			execute( RandomAccessibleInterval< T > tile ) throws Exception;

	Task getStatus();

	ImageTensor getInputNode();

	ImageTensor getOutputNode();

	boolean isSupportingGPU();

	void loadInputNode( String defaultName, Dataset dataset );

	void loadOutputNode( String defaultName );

	void initMapping();

	boolean isInitialized();

	void resetTileCount();

	void setTiledView(TiledView< T > tiledView );

	void cancel();

	/**
	 * Set if singleton dimensions of the output image should be dropped. If the
	 * tile size in one dimension is only one this could remove an important
	 * dimension. Default value is true.
	 */
	void setDropSingletonDims( final boolean dropSingletonDims );

	void setDoDimensionReduction( boolean doDimensionReduction );

	void setDoDimensionReduction( boolean doDimensionReduction, AxisType axisToRemove );

	void doDimensionReduction();

}
