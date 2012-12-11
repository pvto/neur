
package neur.util;

import java.math.BigDecimal;

/** Functions for working with ranges of real valued numbers.
 *
 * @author Paavo Toivanen
 */
public final class Ranges {         private Ranges(){}


    /** @return floor [ (end - start + quantiser) / quantiser ] */
    public static BigDecimal        quantisedSize(BigDecimal[] range, BigDecimal quantiser)
    {
        return range[1].subtract(range[0]).add(quantiser).divide(quantiser);
    }
    
    
    /** @return (member - start) / range_size * quantiser */
    public static int               quantumIndex(BigDecimal member, BigDecimal[] range, BigDecimal quantiser)
    {
        return member
                .subtract(range[0])
                .divide( range[1].subtract(range[0]) )
                .multiply(quantiser)
                .intValue();        
    }

}
