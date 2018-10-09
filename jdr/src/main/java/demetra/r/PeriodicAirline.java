/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demetra.r;

import demetra.arima.ArimaModel;
import demetra.data.DataBlock;
import demetra.data.DoubleSequence;
import demetra.fractionalairline.MultiPeriodicAirlineMapping;
import demetra.information.InformationMapping;
import demetra.likelihood.ConcentratedLikelihood;
import demetra.likelihood.LikelihoodStatistics;
import demetra.likelihood.mapping.LikelihoodInfo;
import demetra.linearmodel.LinearModel;
import demetra.maths.MatrixType;
import demetra.maths.matrices.Matrix;
import demetra.modelling.regression.AdditiveOutlier;
import demetra.modelling.regression.IOutlier.IOutlierFactory;
import demetra.modelling.regression.LevelShift;
import demetra.modelling.regression.SwitchOutlier;
import demetra.processing.IProcResults;
import demetra.regarima.GlsArimaProcessor;
import demetra.regarima.RegArimaEstimation;
import demetra.regarima.RegArimaModel;
import demetra.regarima.ami.OutliersDetectionModule;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author palatej
 */
@lombok.experimental.UtilityClass
public class PeriodicAirline {

    @lombok.Value
    @lombok.Builder
    public static class Results implements IProcResults {

        RegArimaModel<ArimaModel> regarima;
        ConcentratedLikelihood concentratedLogLikelihood;
        LikelihoodStatistics statistics;
        OutlierDescriptor[] outliers;
        Matrix parametersCovariance;
        double[] score;
        double[] parameters;
        double[] linearized;

        private static final String PARAMETERS = "parameters", LL = "likelihood", PCOV = "pcov", SCORE = "score",
                B = "b", T = "t", UNSCALEDBVAR = "unscaledbvar", MEAN = "mean", OUTLIERS = "outliers"
                , LIN="lin", REGRESSORS="regressors";
        private static final InformationMapping<Results> MAPPING = new InformationMapping<>(Results.class);

        static {
            MAPPING.delegate(LL, LikelihoodInfo.getMapping(), r -> r.statistics);
            MAPPING.set(PCOV, MatrixType.class, source -> source.getParametersCovariance());
            MAPPING.set(SCORE, double[].class, source -> source.getScore());
            MAPPING.set(PARAMETERS, double[].class, source -> source.getParameters());
            MAPPING.set(B, double[].class, source
                    -> {
                DoubleSequence b = source.getConcentratedLogLikelihood().coefficients();
                return b.toArray();
            });
            MAPPING.set(T, double[].class, source
                    -> {
                int nhp = source.getParameters().length;
                return source.getConcentratedLogLikelihood().tstats(nhp, true);
            });
            MAPPING.set(MEAN, Double.class, source
                    -> {
                if (source.getRegarima().isMean()) {
                    DoubleSequence b = source.getConcentratedLogLikelihood().coefficients();
                    int mpos = source.getRegarima().getMissingValuesCount();
                    return b.get(mpos);
                } else {
                    return 0.0;
                }
            });
            MAPPING.set(UNSCALEDBVAR, MatrixType.class, source -> source.getConcentratedLogLikelihood().unscaledCovariance());
            MAPPING.set(OUTLIERS, String[].class, source -> {
                OutlierDescriptor[] o = source.getOutliers();
                if (o == null) {
                    return null;
                }
                String[] no = new String[o.length];
                for (int i = 0; i < o.length; ++i) {
                    no[i] = o[i].toString();
                }
                return no;
            });
            MAPPING.set(REGRESSORS, MatrixType.class, source
                    -> {
                List<DoubleSequence> x = source.regarima.getX();
                int n=source.regarima.getY().length(), m=x.size();
                double[] all=new double[n*m];
                int pos=0;
                for (DoubleSequence xcur: x){
                    xcur.copyTo(all, pos);
                    pos+=n;
                }
                return MatrixType.ofInternal(all, n, m);
            });
            
            MAPPING.set(LIN, double[].class, source
                    -> {
                return source.getLinearized();
            });
            
        }

        @Override
        public boolean contains(String id) {
            return MAPPING.contains(id);
        }

        @Override
        public Map<String, Class> getDictionary() {
            Map<String, Class> dic = new LinkedHashMap<>();
            MAPPING.fillDictionary(null, dic, true);
            return dic;
        }

        @Override
        public <T> T getData(String id, Class<T> tclass) {
            return MAPPING.getData(this, id, tclass);
        }

        public static final InformationMapping<Results> getMapping() {
            return MAPPING;
        }

    }

    public Results process(double[] y, MatrixType x, boolean mean, double[] periods, String[] outliers, double cv) {
        final MultiPeriodicAirlineMapping mapping = new MultiPeriodicAirlineMapping(periods, true, false);
        RegArimaModel.Builder builder = RegArimaModel.builder(ArimaModel.class)
                .y(DoubleSequence.ofInternal(y))
                .addX(Matrix.of(x))
                .arima(mapping.getDefault())
                .meanCorrection(mean);
        OutlierDescriptor[] o = null;
        if (outliers != null) {
            GlsArimaProcessor<ArimaModel> processor = GlsArimaProcessor.builder(ArimaModel.class)
                    .mapping(mapping)
                    .precision(1e-5)
                    .build();
            IOutlierFactory[] factories = factories(outliers);
            OutliersDetectionModule od = OutliersDetectionModule.build(ArimaModel.class)
                    .maxOutliers(100)
                    .addFactories(factories)
                    .processor(processor)
                    .build();
            od.setCriticalValue(cv);
            RegArimaModel regarima = builder.build();
            od.prepare(regarima.getObservationsCount());
            od.process(regarima);
            int[][] io = od.getOutliers();
            o=new OutlierDescriptor[io.length];
            for (int i = 0; i < io.length; ++i) {
                int[] cur = io[i];
                DataBlock xcur = DataBlock.make(y.length);
                factories[cur[1]].fill(cur[0], xcur);
                o[i]=new OutlierDescriptor(factories[cur[1]].getCode(), cur[0]);
                builder.addX(xcur);
            }
        }
        GlsArimaProcessor<ArimaModel> finalProcessor = GlsArimaProcessor.builder(ArimaModel.class)
                .mapping(mapping)
                .precision(1e-9)
                .build();
        RegArimaEstimation rslt = finalProcessor.process(builder.build());
        return Results.builder()
                .concentratedLogLikelihood(rslt.getConcentratedLikelihood())
                .parameters(mapping.parametersOf((ArimaModel) rslt.getModel().arima()).toArray())
                .regarima(rslt.getModel())
                .parametersCovariance(rslt.getMax().getHessian())
                .score(rslt.getMax().getGradient())
                .statistics(rslt.statistics(0))
                .outliers(o)
                .linearized(rslt.linearizedSeries().toArray())
                .build();
    }

    private IOutlierFactory[] factories(String[] code) {
        List<IOutlierFactory> fac = new ArrayList<>();
        for (int i = 0; i < code.length; ++i) {
            switch (code[i]) {
                case "ao":
                case "AO":
                    fac.add(AdditiveOutlier.FACTORY);
                    break;
                case "wo":
                case "WO":
                    fac.add(SwitchOutlier.FACTORY);
                    break;
                case "ls":
                case "LS":
                    fac.add(LevelShift.FACTORY_ZEROSTARTED);
                    break;
            }
        }
        return fac.toArray(new IOutlierFactory[fac.size()]);
    }
}
