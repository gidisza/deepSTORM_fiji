
package org.csbdeep.commands;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Ignore;
import org.junit.Test;
import org.scijava.command.CommandModule;
import org.scijava.module.Module;

import org.csbdeep.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.real.FloatType;

public class GenericNetworkTest extends CSBDeepTest {

	@Test
	public void testMissingNetwork() throws ExecutionException, InterruptedException {
		launchImageJ();
		final Dataset input = createDataset(new FloatType(), new long[]{2,2}, new AxisType[]{Axes.X, Axes.Y});
		final Module module = ij.command().run(GenericNetwork.class,
				false, "input", input, "modelFile", new File(
						"/some/non/existing/path.zip")).get();
	}

	@Test
	public void testNonExistingNetworkPref() throws ExecutionException, InterruptedException {
		launchImageJ();
		String bla = new GenericNetwork().getModelFileKey();
		ij.prefs().put(GenericNetwork.class, bla, "/something/useless");
		final Dataset input = createDataset(new FloatType(), new long[]{2,2}, new AxisType[]{Axes.X, Axes.Y});
		final Module module = ij.command().run(GenericNetwork.class,
				true, "input", input, "modelUrl", "http://csbdeep.bioimagecomputing.com/model-tubulin.zip").get();
	}

	@Test
	@Ignore
	public void testGenericNetwork() {
		launchImageJ();
		for (int i = 0; i < 1; i++) {

			testDataset(new FloatType(), new long[] { 10, 10, 10 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });
			testDataset(new UnsignedIntType(), new long[] { 10, 10, 10 },
				new AxisType[] { Axes.X, Axes.Y, Axes.Z });
			testDataset(new ByteType(), new long[] { 10, 10, 10 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

			if (i % 10 == 0) System.out.println(i);
		}

	}

	public <T extends RealType<T> & NativeType<T>> void testDataset(final T type,
		final long[] dims, final AxisType[] axes) {

		final Dataset input = createDataset(type, dims, axes);
		try {
			final Module module = ij.command().run(GenericNetwork.class,
				false, "input", input, "modelFile", new File(
					"/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise2D/model.zip")).get();
			Dataset output = (Dataset) module.getOutput("output");
			assertNotNull(output);
			testResultAxesAndSize(input, output);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}

}
