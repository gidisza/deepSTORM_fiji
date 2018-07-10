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
package mpicbg.csbd.normalize;

import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SortPercentile< T extends RealType< T > & NativeType<T>> implements Percentile< T > {

	private static < T extends RealType< T > > List<T>
			percentiles( final IterableInterval< T > d, final float[] percentiles, ImageJ ij ) {

		final List<T> res = new ArrayList<>();
		for (float percentile : percentiles) {
			T resi = d.firstElement().copy();
			ij.op().stats().percentile(resi, d, (double) percentile);
			res.add(resi);
		}

		return res;
	}

	public List<T> computePercentiles(RandomAccessibleInterval<T> src, final double[] percentiles, OpService opService) {

		int dimensions = 1;
		for(int i = 0; i < src.numDimensions(); i++) {
			dimensions *= src.dimension(i);
		}

		final Img<T> dst = new CellImgFactory<>(((IterableInterval<T>)src).firstElement())
				.create( dimensions );

		Cursor<T> srcCursor = ((IterableInterval<T>)src).cursor();
		Cursor<T> dstCursor = dst.cursor();

		while(srcCursor.hasNext()) {
			srcCursor.fwd();
			dstCursor.fwd();
			dstCursor.get().set(srcCursor.get());
		}

		MyPercentile<T> percentile = new MyPercentile<>();
		percentile.setData(dst);
		T p1 = percentile.evaluate(percentiles[0]);
		T p2 = percentile.evaluate(percentiles[1]);

		List<T> resValues = new ArrayList<>();
		resValues.add(p1);
		resValues.add(p2);
		return resValues;
	}

}
