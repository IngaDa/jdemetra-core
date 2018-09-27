/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rssf;

import demetra.arima.ArimaModel;
import demetra.arima.ssf.SsfArima;
import demetra.data.DoubleSequence;
import demetra.maths.MatrixType;
import demetra.maths.linearfilters.BackFilter;
import demetra.maths.matrices.Matrix;
import demetra.maths.matrices.SymmetricMatrix;
import demetra.maths.polynomials.Polynomial;
import demetra.modelling.regression.GenericTradingDaysVariables;
import demetra.modelling.regression.RegressionUtility;
import demetra.msts.ArParameters;
import demetra.msts.MstsMapping;
import demetra.msts.SarimaParameters;
import demetra.msts.StablePolynomial;
import demetra.msts.VarianceParameter;
import demetra.sarima.SarimaModel;
import demetra.sarima.SarimaSpecification;
import demetra.ssf.SsfComponent;
import demetra.ssf.StateComponent;
import demetra.ssf.implementations.RegSsf;
import demetra.ssf.models.SsfAr;
import demetra.ssf.models.LocalLevel;
import demetra.ssf.models.LocalLinearTrend;
import demetra.ssf.models.SsfAr2;
import demetra.ssf.univariate.ISsf;
import demetra.sts.CyclicalComponent;
import demetra.sts.Noise;
import demetra.sts.SeasonalComponent;
import demetra.sts.SeasonalModel;
import demetra.timeseries.TsDomain;
import demetra.timeseries.calendars.DayClustering;
import demetra.timeseries.calendars.GenericTradingDays;

/**
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
@lombok.experimental.UtilityClass
public class AtomicModels {

    public ModelItem arma(final String name, int nar, double[] ar, int nma, double[] ma, double var, boolean fixed) {
        return mapping -> {
            mapping.add(new StablePolynomial(name + "_ar", nar, ar, -.1));
            mapping.add(new StablePolynomial(name + "_ma", nma, ma, -.2));
            VarianceParameter v = new VarianceParameter(name + "_var", var, true, true);
            mapping.add(v);
            mapping.add((p, builder) -> {
                BackFilter bar = BackFilter.ONE, bma = BackFilter.ONE;
                int pos = 0;
                if (nar > 0) {
                    Polynomial par = Polynomial.valueOf(1, p.extract(0, nar).toArray());
                    bar = new BackFilter(par);
                    pos += nar;
                }
                if (nma > 0) {
                    Polynomial pma = Polynomial.valueOf(1, p.extract(0, nma).toArray());
                    bma = new BackFilter(pma);
                    pos += nma;
                }
                double n = p.get(pos++);
                ArimaModel arima = new ArimaModel(bar, BackFilter.ONE, bma, n);
                StateComponent cmp = SsfArima.componentOf(arima);
                builder.add(name, cmp, null);
                return pos;
            });
        };
    }

    public ModelItem sarima(final String name, int period, int[] orders, int[] seasonal, double[] parameters) {
        SarimaSpecification spec = new SarimaSpecification(period);
        spec.setP(orders[0]);
        spec.setD(orders[1]);
        spec.setQ(orders[2]);
        if (seasonal != null) {
            spec.setBp(seasonal[0]);
            spec.setBd(seasonal[1]);
            spec.setBq(seasonal[2]);
        }
        return mapping -> {
            mapping.add(new SarimaParameters(name, spec, parameters));
            mapping.add((p, builder) -> {
                int np = spec.getParametersCount();
                double[] c = p.extract(0, np).toArray();
                SarimaModel sarima = SarimaModel.builder(spec)
                        .parameters(DoubleSequence.ofInternal(c))
                        .build();
                StateComponent cmp = SsfArima.componentOf(sarima);
                builder.add(name, cmp, null);
                return np;
            });
        };
    }

    public ModelItem localLevel(String name, double lvar, boolean fixed) {
        return mapping -> {
            VarianceParameter v = new VarianceParameter(name + "_var", lvar, fixed, true);
            mapping.add(v);
            mapping.add((p, builder) -> {
                double var = p.get(0);
                SsfComponent cmp = LocalLevel.of(var);
                builder.add(name, cmp);
                return 1;
            });
        };
    }

    public ModelItem localLinearTrend(final String name, double lvar, double svar, boolean lfixed, boolean sfixed) {
        return mapping -> {
            VarianceParameter v1 = new VarianceParameter(name + "_lvar", lvar, lfixed, true);
            mapping.add(v1);
            VarianceParameter v2 = new VarianceParameter(name + "_svar", svar, sfixed, true);
            mapping.add(v2);
            mapping.add((p, builder) -> {
                double var1 = p.get(0);
                double var2 = p.get(1);
                SsfComponent cmp = LocalLinearTrend.of(var1, var2);
                builder.add(name, cmp);
                return 2;
            });
        };
    }

    public ModelItem seasonalComponent(String name, String smodel, int period, double seasvar, boolean fixed) {
        return mapping -> {
            VarianceParameter v = new VarianceParameter(name + "_var", seasvar, fixed, true);
            mapping.add(v);
            mapping.add((p, builder) -> {
                double var = p.get(0);
                SsfComponent cmp = SeasonalComponent.of(SeasonalModel.valueOf(smodel), period, var);
                builder.add(name, cmp);
                return 1;
            });
        };
    }

    public ModelItem noise(String name, double var, boolean fixed) {
        return mapping -> {
            VarianceParameter v = new VarianceParameter(name + "_var", var, fixed, true);
            mapping.add(v);
            mapping.add((p, builder) -> {
                double nv = p.get(0);
                SsfComponent cmp = Noise.of(nv);
                builder.add(name, cmp);
                return 1;
            });
        };
    }

    public ModelItem regression(String name, MatrixType x) {
        return mapping -> {
            mapping.add((p, builder) -> {
                SsfComponent cmp = RegSsf.of(Matrix.of(x));
                builder.add(name, cmp);
                return 0;
            });
        };
    }

    public ModelItem timeVaryingRegression(String name, MatrixType x, double var, boolean fixed) {
        return mapping -> {
            VarianceParameter v = new VarianceParameter(name + "_var", var, fixed, true);
            mapping.add(v);
            mapping.add((p, builder) -> {
                double nv = p.get(0);
                SsfComponent cmp = RegSsf.ofTimeVarying(Matrix.of(x), nv);
                builder.add(name, cmp);
                return 0;
            });
        };
    }

    public ModelItem timeVaryingRegression(String name, MatrixType x, final double[] vars, final boolean fixed) {
        return mapping -> {
            final int n = vars.length;
            for (int i = 0; i < n; ++i) {
                VarianceParameter v = new VarianceParameter(name + "_var" + (i + 1), vars[i], fixed, true);
            }
            mapping.add((p, builder) -> {
                DoubleSequence np = p.extract(0, n);
                SsfComponent cmp = RegSsf.ofTimeVarying(Matrix.of(x), np);
                builder.add(name, cmp);
                return 0;
            });
        };
    }

    public MatrixType tdContrasts(TsDomain domain, int[] groups) {
        DayClustering dc = DayClustering.of(groups);
        GenericTradingDays gtd = GenericTradingDays.contrasts(dc);
        Matrix x = RegressionUtility.data(domain, new GenericTradingDaysVariables(gtd));
        return x.unmodifiable();
    }

    public MatrixType rawTd(TsDomain domain, int[] groups) {
        DayClustering dc = DayClustering.of(groups);
        GenericTradingDays gtd = GenericTradingDays.of(dc);
        Matrix x = RegressionUtility.data(domain, new GenericTradingDaysVariables(gtd));
        return x.unmodifiable();
    }

    public ModelItem tdRegression(String name, TsDomain domain, int[] groups, final boolean contrast, final double var, final boolean fixed) {
        DayClustering dc = DayClustering.of(groups);
        GenericTradingDays gtd = GenericTradingDays.contrasts(dc);
        Matrix x = RegressionUtility.data(domain, new GenericTradingDaysVariables(gtd));
        Matrix mvar = generateVar(dc, contrast);
        return mapping -> {
            VarianceParameter v = new VarianceParameter(name + "_var", var, fixed, true);
            mapping.add(v);
            mapping.add((p, builder) -> {
                double pvar = p.get(0);
                Matrix xvar = mvar.deepClone();
                xvar.mul(pvar);
                SsfComponent cmp = RegSsf.ofTimeVarying(x, xvar);
                builder.add(name, cmp);
                return 1;
            });
        };
    }

    public ModelItem rawTdRegression(String name, TsDomain domain, int[] groups, final double[] vars, final boolean fixed) {
        DayClustering dc = DayClustering.of(groups);
        GenericTradingDays gtd = GenericTradingDays.of(dc);
        final Matrix x = RegressionUtility.data(domain, new GenericTradingDaysVariables(gtd));
        return mapping -> {
            final int n = vars.length;
            for (int i = 0; i < n; ++i) {
                VarianceParameter v = new VarianceParameter(name + "_var" + (i + 1), vars[i], fixed, true);
                mapping.add(v);
            }
            mapping.add((p, builder) -> {
                DoubleSequence np = p.extract(0, n);
                SsfComponent cmp = RegSsf.ofTimeVarying(x, np);
                builder.add(name, cmp);
                return n;
            });
        };
    }

    public ModelItem ar(String name, double[] ar, boolean fixedar, double var, boolean fixedvar, int nlags) {
        return mapping -> {
            mapping.add(new ArParameters(name + "_ar", ar, fixedar));
            VarianceParameter v = new VarianceParameter(name + "_var", var, fixedvar, true);
            mapping.add(v);
            mapping.add((p, builder) -> {
                double[] par = p.extract(0, ar.length).toArray();
                double w = p.get(ar.length);
                SsfComponent cmp = SsfAr.of(par, w, nlags);
                builder.add(name, cmp);
                return ar.length + 1;
            });
        };
    }

    public ModelItem ar(String name, double[] ar, boolean fixedar, double var, boolean fixedvar, int nlags, int nfcasts) {
        return mapping -> {
            mapping.add(new ArParameters(name + "_ar", ar, fixedar));
            VarianceParameter v = new VarianceParameter(name + "_var", var, fixedvar, true);
            mapping.add(v);
            mapping.add((p, builder) -> {
                double[] par = p.extract(0, ar.length).toArray();
                double w = p.get(ar.length);
                SsfComponent cmp = SsfAr2.of(par, w, nlags, nfcasts);
                builder.add(name, cmp);
                return ar.length + 1;
            });
        };
    }
    // TODO

    public ISsf arima(double[] ar, double[] diff, double[] ma, double var) {
        ArimaModel arima = new ArimaModel(BackFilter.ofInternal(ar), BackFilter.ofInternal(diff), BackFilter.ofInternal(ma), var);
        return SsfArima.of(arima);
    }

    public SsfComponent cycle(double dumpingFactor, double cyclicalPeriod, double cvar) {
        return CyclicalComponent.of(dumpingFactor, cyclicalPeriod, cvar);
    }

    private Matrix generateVar(DayClustering dc, boolean contrasts) {
        int groupsCount = dc.getGroupsCount();
        Matrix full = Matrix.square(7);
        if (!contrasts) {
            full.set(-1.0 / 7.0);
        }
        full.diagonal().add(1);
        Matrix Q = Matrix.make(groupsCount - 1, 7);
        int[] gdef = dc.getGroupsDefinition();
        for (int i = 1; i < groupsCount; ++i) {
            for (int j = 0; j < 7; ++j) {
                if (gdef[j] == i) {
                    Q.set(i - 1, j, 1);
                }
            }
        }
        return SymmetricMatrix.XSXt(full, Q);
    }
}
