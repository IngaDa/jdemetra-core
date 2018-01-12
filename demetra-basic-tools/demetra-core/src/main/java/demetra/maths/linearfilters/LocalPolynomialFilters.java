/*
 * Copyright 2017 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package demetra.maths.linearfilters;

import demetra.data.DataBlock;
import demetra.data.DoubleSequence;
import demetra.linearsystem.ILinearSystemSolver;
import demetra.maths.matrices.Matrix;
import demetra.maths.matrices.SymmetricMatrix;
import demetra.maths.matrices.internal.Householder;
import java.util.function.IntToDoubleFunction;

/**
 * The local polynomial filter is defined as follows: h is the number of lags
 * (-> length of the filter is 2*h+1) d is the order of the local polynomial ki
 * (local weight); we suppose that k(i) = k(-i) (symmetric filters; other
 * filters could be considered)
 *
 * @author Jean Palate
 */
@lombok.experimental.UtilityClass
public class LocalPolynomialFilters {

    public enum EndPoints {
        NONE,
        DAF,
        LC,
        QL,
        CQ;

    };

    public DoubleSequence filter(DoubleSequence input, final SymmetricFilter filter, final FiniteFilter[] afilters) {
        double[] x = input.toArray();
        int h = filter.getUpperBound();
        DataBlock out = DataBlock.ofInternal(x, h, x.length - h);
        filter.apply(i -> input.get(i), IFilterOutput.of(out, h));

        // apply the endpoints filters
        if (afilters != null) {
            for (int i = 0; i < h; ++i) {
                int len = h + i + 1;
                x[i] = afilters[i].apply(input.extract(0, len).reverse());
                x[x.length - i - 1] = afilters[i].apply(input.extract(x.length - len, len));
            }
        } else {
            for (int i = 0; i < h; ++i) {
                x[i] = Double.NaN;
                x[x.length - i - 1] = Double.NaN;
            }
        }
        return DoubleSequence.ofInternal(x);
    }

    /**
     *
     * @param h the number of lags (-> length of the filter is 2*h+1)
     * @param d d is the order of the local polynomial
     * @param k weight of y(t+i); we suppose that k(i) = k(-i) (symmetric
     * filters; other filters could be considered)
     * @return The corresponding filter
     */
    public SymmetricFilter of(final int h, final int d, final IntToDoubleFunction k) {
        switch (d) {
            case 0:
            case 1:
                return of0_1(h, k);
            case 2:
            case 3:
                return of2_3(h, k);
            default:
                return ofDefault(h, d, k);
        }
    }

    public FiniteFilter directAsymmetricFilter(final int h, final int q, final int d, final IntToDoubleFunction k) {
        return defaultDirectAsymmetricFilter(h, q, d, k);
    }

    private static SymmetricFilter of0_1(int h, IntToDoubleFunction k) {
        double[] w = new double[h + 1];
        double s0 = S_h0(h, k);
        for (int i = 0; i <= h; ++i) {
            w[i] = k.applyAsDouble(i) / s0;
        }
        return SymmetricFilter.ofInternal(w);
    }

    private static SymmetricFilter of2_3(int h, IntToDoubleFunction k) {
        double[] w = new double[h + 1];
        double s0 = 0, s2 = 0, s4 = 0;
        for (int i = 1; i <= h; ++i) {
            long j2 = i * i;
            long j4 = j2 * j2;
            double ki = k.applyAsDouble(i);
            s0 += ki;
            s2 += j2 * ki;
            s4 += j4 * ki;
        }
        s0 = k.applyAsDouble(0) + 2 * s0;
        s2 *= 2;
        s4 *= 2;

        double sr = s2 / s4;
        for (int i = 0; i <= h; ++i) {
            double n = 1 - i * i * sr, d = s0 - s2 * sr;
            w[i] = k.applyAsDouble(i) * n / d;
        }
        return SymmetricFilter.ofInternal(w);
    }

    SymmetricFilter ofDefault(int h, int d, IntToDoubleFunction k) {
        // w = KX (X'K X)^-1 e1
        // (X'K X)^-1 e1 = u <-> (X'K X) u = e1
        Matrix xkx = Matrix.square(d + 1);
        for (int i = 0; i <= d; ++i) {
            xkx.set(i, i, S_hd(h, 2 * i, k));
            for (int j = 0; j < i; ++j) {
                double x = S_hd(h, i + j, k);
                if (x != 0) {
                    xkx.set(i, j, x);
                    xkx.set(j, i, x);
                }
            }
        }
        double[] u = new double[d + 1];
        u[0] = 1;
        ILinearSystemSolver.robustSolver().solve(xkx, DataBlock.ofInternal(u));
        double[] w = new double[h + 1];
        for (int i = 0; i <= h; ++i) {
            double s = u[0];
            long q = 1;
            for (int j = 1; j <= d; ++j) {
                q *= i;
                s += q * u[j];
            }
            w[i] = s * k.applyAsDouble(i);
        }
        return SymmetricFilter.ofInternal(w);
    }

    private double S_h0(int h, IntToDoubleFunction k) {
        double s = 0;
        for (int i = 1; i <= h; ++i) {
            s += k.applyAsDouble(i);
        }
        return 2 * s + k.applyAsDouble(0);
    }

    private double S_h2(int h, IntToDoubleFunction k) {
        double s = 0;
        for (int i = 1; i <= h; ++i) {
            int j = i * i;
            s += j * k.applyAsDouble(i);
        }
        return 2 * s;
    }

    private double S_h4(int h, IntToDoubleFunction k) {
        double s = 0;
        for (int i = 1; i <= h; ++i) {
            long j = i * i;
            j *= j;
            s += j * k.applyAsDouble(i);
        }
        return 2 * s;
    }

    private double S_hd(int h, int d, IntToDoubleFunction k) {
        switch (d) {
            case 0:
                return S_h0(h, k);
            case 2:
                return S_h2(h, k);
            case 4:
                return S_h4(h, k);
        }
        if (d % 2 != 0) {
            return 0;
        }
        int hd = d / 2;
        double s = 0;
        for (int i = 1; i <= h; ++i) {
            long ii = i * i;
            long j = ii;
            for (int l = 2; l <= hd; ++l) {
                j *= ii;
            }
            s += j * k.applyAsDouble(i);
        }
        return 2 * s;
    }

    private double S_hqd(int h, int q, long d, IntToDoubleFunction k) {
        if (d == 0) {
            return S_hq0(h, q, k);
        }
        double s = 0;
        if (d % 2 == 0) {
            for (int i = 1; i <= h; ++i) {
                long j = i;
                for (int l = 1; l < d; ++l) {
                    j *= i;
                }
                if (i <= q) {
                    s += 2 * j * k.applyAsDouble(i);
                } else {
                    s += j * k.applyAsDouble(i);
                }
            }
        } else {
            for (int i = q + 1; i <= h; ++i) {
                long j = i;
                for (int l = 1; l < d; ++l) {
                    j *= i;
                }
                s -= j * k.applyAsDouble(i);
            }
        }
        return s;
    }

    private double S_hq0(int h, int q, IntToDoubleFunction k) {
        double s = k.applyAsDouble(0);
        for (int i = 1; i <= q; ++i) {
            s += 2 * k.applyAsDouble(i);
        }
        for (int i = q + 1; i <= h; ++i) {
            s += k.applyAsDouble(i);
        }
        return s;
    }

    FiniteFilter defaultDirectAsymmetricFilter(int h, int q, int d, IntToDoubleFunction k) {
        // w = KpXp (Xp'Kp Xp)^-1 e1
        // (Xp'Kp Xp)^-1 e1 = u <-> (Xp'Kp Xp) u = e1
        Matrix xkx = Matrix.square(d + 1);
        for (int i = 0; i <= d; ++i) {
            xkx.set(i, i, S_hqd(h, q, 2 * i, k));
            for (int j = 0; j < i; ++j) {
                double x = S_hqd(h, q, i + j, k);
                if (x != 0) {
                    xkx.set(i, j, x);
                    xkx.set(j, i, x);
                }
            }
        }
        double[] u = new double[d + 1];
        u[0] = 1;
        Householder hous=new Householder();
        hous.decompose(xkx);
        hous.solve(DataBlock.ofInternal(u));
        double[] w = new double[h + q + 1];
        w[h] = u[0] * k.applyAsDouble(0);
        for (int i = 1; i <= q; ++i) {
            double s = u[0];
            long l = 1;
            for (int j = 1; j <= d; ++j) {
                l *= i;
                s += l * u[j];
            }
            double wc = s * k.applyAsDouble(i);
            w[h + i] = wc;
        }
        for (int i = -1; i >= -h; --i) {
            double s = u[0];
            long l = 1;
            for (int j = 1; j <= d; ++j) {
                l *= i;
                s += l * u[j];
            }
            double wc = s * k.applyAsDouble(i);
            w[h + i] = wc;
        }
        return FiniteFilter.ofInternal(w, -h);
    }

    /**
     * Provides an asymmetric filter [-h, p] based on the given symmetric
     * filter. The asymmetric filter minimizes the mean square revision error
     * relative to the symmetric filter. The series follows the model y=U*du +
     * Z*dz + e, std(e) = sigma/ki
     *
     * @param sw The symmetric filter
     * @param p The horizon of the asymmetric filter (from 0 to deg(w)/2)
     * @param u The degree of the constraints (U)
     * @param dz The given coefficients (usually a singleton)
     * @param k The weighting factors (null for no weighting)
     * @return
     */
    public FiniteFilter asymmetricFilter(SymmetricFilter sw, int p, int u, double[] dz, IntToDoubleFunction k) {
        double[] w = sw.weightsToArray();
        int h = w.length / 2;
        int nv = h + p + 1;
        DataBlock wp = DataBlock.ofInternal(w, 0, nv);
        DataBlock wf = DataBlock.ofInternal(w, nv, w.length);
        Matrix Zp = z(-h, p, u + 1, u + dz.length);
        Matrix Zf = z(p + 1, h, u + 1, u + dz.length);
        Matrix Up = z(-h, p, 0, u);
        Matrix Uf = z(p + 1, h, 0, u);
        DataBlock d = DataBlock.ofInternal(dz);

        Matrix H = SymmetricMatrix.XtX(Up);

        DataBlock a1 = DataBlock.make(u + 1);
        a1.product(Uf.columnsIterator(), wf); // U'f x wf

        DataBlock a2 = a1.deepClone();
        Householder hous=new Householder();
        hous.decompose(H);

        hous.solve(a2); // (U'p x Up)^-1 Uf x Wf

        DataBlock a3 = DataBlock.make(nv);
        a3.product(Up.rowsIterator(), a2); // Up x (U'p x Up)^-1 Uf x Wf

        DataBlock a4 = DataBlock.make(dz.length);
        a4.product(Zp.columnsIterator(), a3);
        a4.chs();
        a4.addProduct(Zf.columnsIterator(), wf); // (Zf' - Zp' x  Up x (U'p x Up)^-1 Uf ) Wf

        DataBlock a5 = DataBlock.make(nv);
        a5.product(Zp.rowsIterator(), d); // Zp x d

        DataBlock a6 = DataBlock.make(u + 1);
        a6.product(Up.columnsIterator(), a5); // Up' x Zp x d

        DataBlock a7 = a6.deepClone();
        hous.solve(a7);

        DataBlock a8 = DataBlock.make(nv);
        a8.product(Up.rowsIterator(), a7);

        DataBlock a9 = a5.deepClone();
        a9.sub(a8);

        DataBlock a10 = DataBlock.make(dz.length);
        a10.product(Zp.columnsIterator(), a9);
        Matrix C = Matrix.square(dz.length);
        for (int i = 0; i < dz.length; ++i) {
            for (int j = 0; j < dz.length; ++j) {
                double x = a10.get(i) * dz[j];
                if (i == j) {
                    x += 1;
                }
                C.set(i, j, x);
            }
        }
        DataBlock a11 = a4.deepClone();
        hous.decompose(C);
        hous.solve(a11);

        double s = a11.dot(d);
        a9.mul(s);

        wp.add(a9);
        wp.add(a3);

        return FiniteFilter.ofInternal(wp.toArray(), -h);
    }

    /**
     * @param d0 included
     * @param d1 included
     * @param l included (negative)
     * @param u included (positive)
     * @return
     */
    synchronized Matrix z(int l, int u, int d0, int d1) {
        int nh = Math.max(Math.abs(l), Math.abs(u));
        if (Z == null || Z.getRowsCount() / 2 < nh || Z.getColumnsCount() < d1+1) {
            Z = createZ(nh, d1);
        }
        return Z.extract(l + nh, u - l + 1, d0, d1 - d0 + 1);
    }

    private Matrix createZ(int h, int d) {
        Matrix M = Matrix.make(2 * h + 1, d + 1);
        M.column(0).set(1);
        if (d >= 1) {
            DataBlock c1 = M.column(1);
            c1.set(i -> i - h);
            for (int i = 2; i <= d; ++i) {
                M.column(i).set(c1, M.column(i - 1), (a, b) -> a * b);
            }
        }
        return M;
    }

    private Matrix Z;
}