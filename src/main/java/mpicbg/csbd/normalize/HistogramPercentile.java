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

import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;

import java.util.ArrayList;
import java.util.List;


public class HistogramPercentile< T extends RealType< T > & NativeType<T>> implements Percentile< T > {

	T min, max;

	public List<T> computePercentiles(RandomAccessibleInterval<T> src, final double[] percentiles, OpService opService) {

		List<T> resValues = new ArrayList<>();

		computeMinMax(opService, (IterableInterval<T>) src);

		if(min.compareTo(max) == 0) {
			resValues.add(min);
			resValues.add(max);
			return resValues;
		}

		int numBins = (int) Math.pow(2,16);
		List<HistogramBin> bins = createHistogram(numBins, (IterableInterval<T>) src);
		long pixelCount = ((IterableInterval<T>) src).size();

		for(double percentile : percentiles) {
			resValues.add(getPercentile(percentile, bins, pixelCount));
		}

		return resValues;

	}

	private void computeMinMax(OpService opService, IterableInterval<T> src) {
		Pair<T, T> minMax = opService.stats().minMax(src);
		min = minMax.getA();
		max = minMax.getB();
	}

	private List<HistogramBin> createHistogram(int numBins, IterableInterval<T> src) {
		List<HistogramBin> bins = new ArrayList<>(numBins);
		double binLength = getBinLength(numBins);

		for (int i = 0; i < numBins; i++) bins.add(new HistogramBin());

		Cursor<T> cursor = src.cursor();
		while(cursor.hasNext()) {
			cursor.fwd();
			T val = cursor.get();
			int binId = Math.min(numBins-1, getBin(val, min, binLength));
			HistogramBin bin = bins.get(binId);
			bin.count++;
			if(bin.min == null) bin.min = val.copy();
			else if(bin.min.compareTo(val) > 0) bin.min.set(val.copy());
			if(bin.max == null) bin.max = val.copy();
			else if(bin.max.compareTo(val) < 0) bin.max.set(val.copy());
		}
		return bins;
	}

	private double getBinLength(int numBins) {
		return (max.getRealDouble() - min.getRealDouble())/(double)numBins;
	}

	private int getBin(T val, T min, double binLength) {
		T res = val.copy();
		res.sub(min);
		res.mul(1./binLength);
		return (int)res.getRealFloat();
	}

	private T getPercentile(double percentile, List<HistogramBin> bins, long pixelCount) {
		double border = pixelCount * percentile / 100.;
		double cur = 0;
		int i;
		double percentileMax, percentileMin;
		if(percentile > 50) {
			border = pixelCount - border;
			for (i = bins.size()-1; i >= 0 && cur < border; i--) {
				cur += bins.get(i).count;
			}
			i++;
			percentileMin = (1-cur / pixelCount) * 100;
			percentileMax = (1-(cur-bins.get(i).count) / pixelCount) * 100;
		} else {
			for (i = 0; i < bins.size() && cur < border; i++) {
				cur += bins.get(i).count;
			}
			i--;
			percentileMax = cur / pixelCount * 100;
			percentileMin = (cur-bins.get(i).count) / pixelCount * 100;
		}
		HistogramBin bin = bins.get(i);
		if(percentile < percentileMax && percentile > percentileMin) {
			return getY(percentile, percentileMin, bin.min, percentileMax, bin.max);
		} else {
			if(percentile < percentileMin) {
				HistogramBin prevBin = getPreviousBin(bins, i);
				if(prevBin != null)
					return getY(percentile, percentileMin, bin.min, percentileMin-1./(double)pixelCount, prevBin.max);
				else return bin.min.copy();
			} else {
				HistogramBin nextBin = getNextBin(bins, i);
				if(nextBin != null)
					return getY(percentile, percentileMax, bin.max, percentileMax+1./(double)pixelCount, nextBin.min);
				else return bin.max.copy();
			}
		}
	}

	private T getY (double x, double x1, T y1, double x2, T y2) {
		double m = (y2.getRealDouble() - y1.getRealDouble()) / (x2 - x1);
		double n = (y1.getRealDouble() * x2 - y2.getRealDouble() * x1) / (x2 - x1);
		T mT = y1.copy();
		mT.setReal(m);
		T nT = mT.copy();
		nT.setReal(n);
		T y = mT.copy();
		y.mul(x);
		y.add(nT);
		return y;
	}

	private HistogramBin getPreviousBin(List<HistogramBin> bins, int i) {
		while(i >= 0) {
			if(bins.get(--i).count > 0) return bins.get(i);
		}
		return null;
	}
	private HistogramBin getNextBin(List<HistogramBin> bins, int i) {
		while(i < bins.size()) {
			if(bins.get(++i).count > 0) return bins.get(i);
		}
		return null;
	}

	private class HistogramBin {
		int count;
		T min;
		T max;
	}


}
