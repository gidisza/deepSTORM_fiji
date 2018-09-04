
package mpicbg.csbd.network.task;

import com.google.protobuf.InvalidProtocolBufferException;
import mpicbg.csbd.network.DefaultNetwork;
import mpicbg.csbd.network.tensorflow.DatasetTensorflowConverter;
import mpicbg.csbd.network.tensorflow.TensorFlowNetwork;
import mpicbg.csbd.network.tensorflow.TensorFlowRunner;
import mpicbg.csbd.task.Task;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.AxisType;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.io.location.Location;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;
import org.tensorflow.framework.TensorShapeProto;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public abstract class TestNetwork<T extends RealType<T>> extends
		TensorFlowNetwork<T>
{

	protected long[] inputShape;
	protected long[] outputShape;
	protected int inputCount = 1;
	protected int outputCount = 1;

	public TestNetwork(TensorFlowService tensorFlowService,
	                   DatasetService datasetService)
	{
		super(tensorFlowService, datasetService, null);
	}

	@Override
	public void loadInputNode(final Dataset dataset) {
		super.loadInputNode( dataset);
		if (inputCount > 0) {
			inputNode.setName("input");
			inputNode.setNodeShape(inputShape);
			inputNode.initializeNodeMapping();
		}
	}

	@Override
	public void loadOutputNode(Dataset dataset) {
		super.loadOutputNode(dataset);
		if (outputCount > 0) {
			outputNode.setName("output");
			outputNode.setNodeShape(outputShape);
			outputNode.initializeNodeMapping();
		}
	}

	@Override
	protected void logTensorShape(String title, final TensorInfo tensorInfo) {
		log("cannot log tensorinfo shape of test networks");
	}

}
