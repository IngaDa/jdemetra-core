/*
* Copyright 2013 National Bank of Belgium
*
* Licensed under the EUPL, Version 1.1 or – as soon they will be approved 
* by the European Commission - subsequent versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the Licence.
* You may obtain a copy of the Licence at:
*
* http://ec.europa.eu/idabc/eupl
*
* Unless required by applicable law or agreed to in writing, software 
* distributed under the Licence is distributed on an "AS IS" basis,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the Licence for the specific language governing permissions and 
* limitations under the Licence.
 */
package demetra.maths;

import demetra.design.Development;

/**
 * Uitlities on integer numbers
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Release)
@lombok.experimental.UtilityClass
public final class IntUtility {

    /**
     *
     * @param n
     * @return
     */
    public int[] divisors(final int n) {
        int[] tmp = new int[1 + n / 2];
        int nd = divisors(n, tmp);
        int[] rslt = new int[nd];
        for (int i = 0; i < nd; ++i) {
            rslt[i] = tmp[i];
        }
        return rslt;
    }

    /**
     *
     * @param n
     * @param buffer
     * @return
     */
    public int divisors(final int n, final int[] buffer) {
        if (n == 1) {
            return 0;
        }
        int d = 1;
        int idx = 0;
        while (d * 2 <= n) {
            if (n % d == 0) {
                buffer[idx++] = d;
            }
            ++d;
        }
        return idx;
    }

//    /**
//     * 
//     * @param a
//     * @param b
//     * @return
//     */
//    public static int PGCD(final int a, final int b) {
//	return (b == 0) ? a : PGCD(b, a % b);
//    }
//
//    /**
//     * 
//     * @param a
//     * @param b
//     * @return
//     */
//    public static int PPCM(int a, int b)
//    {
//	int r = 1;
//	int div = 2;
//	while ((a != 1) || (b != 1)) {
//	    boolean ok = false;
//	    if (a % div == 0) {
//		a /= div;
//		ok = true;
//	    }
//	    if (b % div == 0) {
//		b /= div;
//		ok = true;
//	    }
//	    if (ok)
//		r *= div;
//	    if (!ok)
//		++div;
//	}
//	return r;
//    }
    /**
     * Computes the greatest common divisor of two integers.
     *
     * @param a
     * @param b
     * @return
     */
    public int gcd(int a, int b) {
        while (b > 0) {
            int temp = b;
            b = a % b; // % is remainder  
            a = temp;
        }
        return a;
    }

    /**
     * Computes the least common multiple of two integers.
     *
     * @param a
     * @param b
     * @return
     */
    public int lcm(int a, int b) {
        return a * (b / gcd(a, b));
    }

    /**
     * Computes the sum of the powers of the first n integers
     * = 1 + 2^k + 3^k + ... + (n)^k
     *
     * @param k
     * @param dn
     * @return
     */
    public double sumOfPowers(int k, long n) {
        double dn=n;
        switch (k) {
            case 1:
                return dn * (dn + 1) / 2;
            case 2:
                return dn * (dn + 1) * (2 * dn + 1) / 6;
            case 3:
                return dn * dn * (dn + 1) * (dn + 1) / 4;
            case 4:
                return dn * (dn + 1) * (2 * dn + 1) * (3 * dn * dn + 3 * dn - 1) / 30;
            case 5:
                return dn * dn * (dn + 1) * (dn + 1) * (2 * dn * dn + 2 * dn - 1) / 12;
            case 6: {
                double n2 = dn * dn, n3 = n2 * dn, n4 = n3 * dn;
                return dn * (dn + 1) * (2 * dn + 1) * (3 * n4 + 6 * n3 - 3 * dn + 1) / 42;
            }
            case 7: {
                double n2 = dn * dn, n3 = n2 * dn, n4 = n3 * dn;
                return dn * dn * (dn + 1) * (dn + 1) * (3 * n4 + 6 * n3 - n2 - 4 * dn + 2) / 24;
            }
            case 8: {
                double n2 = dn * dn, n3 = n2 * dn, n4 = n3 * dn, n5 = n4 * dn, n6 = n5 * dn;
                return dn * (dn + 1) * (2 * dn + 1) * (5 * n6 + 15 * n5 + 5 * n4 - 15 * n3 - n2 + 9 * dn - 3) / 90;
            }
            case 9: {
                double n2 = dn * dn, n3 = n2 * dn, n4 = n3 * dn;
                return n2 * (dn + 1) * (dn + 1) * (n2 + dn - 1) * (2 * n4 + 4 * n3 - n2 - 3 * dn + 3) / 20;
            }
            case 10: {
                double n2 = dn * dn, n3 = n2 * dn, n4 = n3 * dn, n5 = n4 * dn, n6 = n5 * dn;
                return dn * (dn + 1) * (2 * dn + 1) * (n2 + dn - 1) * (3 * n6 + 9 * n5 + 2 * n4 - 11 * n3 + 3 * n2 + 10 * dn - 5) / 66;
            }
            default: // should use the Bernoulli formula //
                long s = 1;
                for (int i = 2; i <= dn; ++i) {
                    long c = i;
                    for (int j = 2; j <= k; ++j) {
                        c *= i;
                    }
                    s += c;
                }
                return s;
        }
    }

}