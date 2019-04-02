/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demetra.x11;

import demetra.data.DoubleSequence;
import demetra.timeseries.TsData;
import demetra.timeseries.TsPeriod;
import java.util.Arrays;

/**
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
@lombok.Getter
public class X11Kernel implements X11.Processor {

    private X11BStep bstep;
    private X11CStep cstep;
    private X11DStep dstep;
    private TsData input;
    private X11Context context;

    public static double[] table(int n, double value) {
        double[] x = new double[n];
        Arrays.fill(x, value);
        return x;
    }

    @Override
    public X11Results process(@lombok.NonNull TsData timeSeries, @lombok.NonNull X11Spec spec) {
        clear();

        int frequency = timeSeries.getAnnualFrequency();
        if (frequency == -1) {
            throw new IllegalArgumentException("Frequency of the time series must be compatible with years");
        }
        input = timeSeries;
        DoubleSequence data = input.getValues();
        context = X11Context.of(spec, input);
        if (context.isLogAdd()) {
            data = data.log();
        }
        bstep = new X11BStep();
        bstep.process(data, context);
        cstep = new X11CStep();
        cstep.process(data, context.remove(data, bstep.getB20()), context);
        dstep = new X11DStep();
        dstep.process(data, context.remove(data, cstep.getC20()), context);
        return buildResults(timeSeries.getStart(), spec);
    }

    private void clear() {
        bstep = null;
        cstep = null;
        dstep = null;
        input = null;
        context = null;
    }

    private X11Results buildResults(TsPeriod start, X11Spec spec) {
        return X11Results.builder()
                .spec(spec)
                //B-Tables
                .b1(input)
                .b2(TsData.ofInternal(start.plus(bstep.getB2drop()), prepare(bstep.getB2())))
                .b3(TsData.ofInternal(start.plus(bstep.getB2drop()), prepare(bstep.getB3())))
                .b4(TsData.ofInternal(start.plus(bstep.getB2drop()), prepare(bstep.getB4())))
                .b5(TsData.ofInternal(start, prepare(bstep.getB5())))
                .b6(TsData.ofInternal(start, prepare(bstep.getB6())))
                .b7(TsData.ofInternal(start, prepare(bstep.getB7())))
                .b8(TsData.ofInternal(start, prepare(bstep.getB8())))
                .b9(TsData.ofInternal(start, prepare(bstep.getB9())))
                .b10(TsData.ofInternal(start, prepare(bstep.getB10())))
                .b11(TsData.ofInternal(start, prepare(bstep.getB11())))
                .b13(TsData.ofInternal(start, prepare(bstep.getB13())))
                .b17(TsData.ofInternal(start, bstep.getB17()))
                .b20(TsData.ofInternal(start, prepare(bstep.getB20())))
                //C-Tables
                .c1(TsData.ofInternal(start, prepare(cstep.getC1())))
                .c2(TsData.ofInternal(start.plus(cstep.getC2drop()), prepare(cstep.getC2())))
                .c4(TsData.ofInternal(start.plus(cstep.getC2drop()), prepare(cstep.getC4())))
                .c5(TsData.ofInternal(start, prepare(cstep.getC5())))
                .c6(TsData.ofInternal(start, prepare(cstep.getC6())))
                .c7(TsData.ofInternal(start, prepare(cstep.getC7())))
                .c9(TsData.ofInternal(start, prepare(cstep.getC9())))
                .c10(TsData.ofInternal(start, prepare(cstep.getC10())))
                .c11(TsData.ofInternal(start, prepare(cstep.getC11())))
                .c13(TsData.ofInternal(start, prepare(cstep.getC13())))
                .c17(TsData.ofInternal(start, cstep.getC17()))
                .c20(TsData.ofInternal(start, prepare(cstep.getC20())))
                //D-Tables
                .d1(TsData.ofInternal(start, prepare(dstep.getD1())))
                .d2(TsData.ofInternal(start.plus(dstep.getD2drop()), prepare(dstep.getD2())))
                .d4(TsData.ofInternal(start.plus(dstep.getD2drop()), prepare(dstep.getD4())))
                .d5(TsData.ofInternal(start, prepare(dstep.getD5())))
                .d6(TsData.ofInternal(start, prepare(dstep.getD6())))
                .d7(TsData.ofInternal(start, prepare(dstep.getD7())))
                .d8(TsData.ofInternal(start, prepare(dstep.getD8())))
                .d9(TsData.ofInternal(start, prepare(dstep.getD9())))
                .d10(TsData.ofInternal(start, prepare(dstep.getD10())))
                .d11(TsData.ofInternal(start, prepare(dstep.getD11())))
                .d12(TsData.ofInternal(start, prepare(dstep.getD12())))
                .d13(TsData.ofInternal(start, prepare(dstep.getD13())))
                //Final
                .iCRatio(dstep.getICRatio())
                .mode(spec.getMode())
                .finalHendersonFilterLength(dstep.getFinalHendersonFilterLength())
                .build();
    }

    private DoubleSequence prepare(final DoubleSequence in) {
        DoubleSequence ds = in;
        if (context.isLogAdd()) {
            ds = ds.exp();
        }
        return ds;
    }
}