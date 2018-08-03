package mpicbg.csbd.network;

import mpicbg.csbd.network.task.DefaultModelLoader;
import mpicbg.csbd.network.task.ModelLoader;
import mpicbg.csbd.network.tensorflow.TensorFlowNetwork;
import mpicbg.csbd.util.IOHelper;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.command.Command;
import org.scijava.io.location.Location;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.FileNotFoundException;
import java.io.IOException;

@Plugin(type = Command.class)
public class TensorflowCommand implements Command {

    @Parameter
    private TensorFlowService tensorFlowService;

    @Parameter
    private DatasetService datasetService;

    @Override
    public void run() {
        System.out.println("jo");
//        Location model3d = null, model2d = null;
//        try {
//            model3d = IOHelper.loadFileOrURL("/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise3D/model.zip");
//            model2d = IOHelper.loadFileOrURL("/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise2D/model.zip");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        try {
//            System.out.println("blub");
//            tensorFlowService.loadModel(model3d, "tmp_3dmodel", "serve");
//            tensorFlowService.loadModel(model2d, "tmp_2dmodel", "serve");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        final Dataset dataset = datasetService.create(new FloatType(), new long[] { 30, 80, 2,
                5 }, "", new AxisType[] { Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z });

        Network network = new TensorFlowNetwork(tensorFlowService, datasetService,
                null);
        ModelLoader modelLoader = new DefaultModelLoader();
        modelLoader.run("model2d", network, "/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise2D/model.zip", "input", "output", dataset);
        network.dispose();
        modelLoader.run("model3d", network, "/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise3D/model.zip", "input", "output", dataset);
    }

    public static void main(final String... args) throws Exception {

        final ImageJ ij = new ImageJ();

        ij.launch(args);

        ij.command().run(TensorflowCommand.class, false);

    }

}