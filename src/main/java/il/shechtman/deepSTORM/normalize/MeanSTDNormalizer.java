/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2018 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package il.shechtman.deepSTORM.normalize;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.Axis;
import net.imagej.axis.AxisType;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class MeanSTDNormalizer<T extends RealType<T> & NativeType<T>>
	implements Normalizer
{
	private float STD;
	private float mean;

	public float normalize( final T val ) {

		return (val.getRealFloat() - mean) / STD;
	}

	public float project01(final T val, final T min, final T max) {
		return (val.getRealFloat() - min.getRealFloat()) / (max.getRealFloat() - min.getRealFloat());
	}
	@Override
	public Dataset normalize(final Dataset im, OpService opService,
		DatasetService datasetService)
	{
		long[] dims = new long[im.numDimensions()];
		im.dimensions(dims);
		AxisType[] axes = new AxisType[im.numDimensions()];
		for (int i = 0; i < axes.length; i++) {
			axes[i] = im.axis(i).type();
		}

		final Dataset output = datasetService.create(new FloatType(), dims,
			"normalized input", axes);


		//final Cursor<FloatType> out = (Cursor<FloatType>) output.getImgPlus()
		//	.localizingCursor();


		GenericMinMax<ByteType> minMax = new GenericMinMax<>();

		for (int i = 0; i <= im.getImgPlus().dimension(2); i++) {
			IntervalView viewInput = Views.hyperSlice(im.getImgPlus(), 2, i);
			IntervalView viewOutput = Views.hyperSlice(output.getImgPlus(), 2, i);
			net.imglib2.util.Pair minMaxtmp = minMax.calculate(viewInput);
			T max = (T) minMaxtmp.getB();
			T min = (T) minMaxtmp.getA();
			Cursor<FloatType> outCursor = viewOutput.localizingCursor();
			Cursor<T> inCursor = viewInput.localizingCursor();
			while (outCursor.hasNext()) {
				outCursor.fwd();
				inCursor.fwd();
				outCursor.get().set(project01(inCursor.get(), min, max));
				outCursor.get().set(normalize(inCursor.get()));
			}
		}
//		while (out.hasNext()) {
//			out.fwd();
//			in.setPosition(out);
//			out.get().set(normalize(in.get()));
//		}

		return output;
	}

	@Override
	public void setup(float mean, float std) {

		this.mean = mean;
		this.STD = std;
	}

}
