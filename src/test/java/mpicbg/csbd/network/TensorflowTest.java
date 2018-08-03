package mpicbg.csbd.network;

import mpicbg.csbd.network.tensorflow.TensorFlowNetwork;
import net.imagej.ImageJ;
import org.junit.Test;

public class TensorflowTest {

    @Test
    public void testNetworkAndModelLoader() {

    }

    @Test
    public void testTensorflowService() {
        ImageJ ij = new ImageJ();
        ij.ui().setHeadless(true);
        ij.command().run(TensorflowCommand.class, false);
    }

}
