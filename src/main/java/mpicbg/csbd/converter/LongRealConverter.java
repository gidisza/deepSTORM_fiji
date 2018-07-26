
package mpicbg.csbd.converter;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author Stephan Saalfeld
 * @author Stephan Preibisch
 */
public class LongRealConverter<R extends RealType<R>> implements
	Converter<LongType, R>
{

	@Override
	public void convert(final LongType input, final R output) {
		output.setReal(input.getIntegerLong());
	}
}
