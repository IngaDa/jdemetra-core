/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demetra.regarima;

import demetra.arima.IArimaModel;
import demetra.arima.StationaryTransformation;
import demetra.data.DataBlock;
import demetra.data.DataBlockIterator;
import demetra.data.DoubleSequence;
import demetra.design.Immutable;
import demetra.eco.EcoException;
import demetra.linearmodel.LinearModel;
import demetra.maths.linearfilters.BackFilter;
import demetra.maths.matrices.Matrix;
import java.util.List;
import demetra.maths.MatrixType;

/**
 * Linear model with stationary ARMA process
 * The regression variables correspond to 
 * 1. The missing values (additive outliers approach)
 * 2. The mean correction
 * 3. The other regression variables
 *
 * @author Jean Palate <jean.palate@nbb.be>
 * @param <M>
 */
@Immutable
@lombok.Value
public class RegArmaModel<M extends IArimaModel> {

    /**
     * Creates a new RegArma model from an existing one, with a new stationary ARMA
     * @param <M>
     * @param model
     * @param newarma
     * @return 
     */
    public static <M extends IArimaModel> RegArmaModel<M> of(RegArmaModel<M> model, M newarma){
        return new RegArmaModel(model.y, newarma, model.x, model.missingCount);
    }

    static <M extends IArimaModel> RegArmaModel<M> of(RegArimaModel<M> regarima) {
        StationaryTransformation<M> st = (StationaryTransformation<M>) regarima.arima().stationaryTransformation();
        M arma = st.getStationaryModel();
        BackFilter ur = st.getUnitRoots();
        int d = ur.getDegree();
        int n = regarima.getObservationsCount();
        int ndy = n - d;
        if (ndy <= 0) {
            throw new EcoException(EcoException.NOT_ENOUGH_OBS);
        }

        DoubleSequence y = regarima.getY();
        boolean mean = regarima.isMean();
        List<DoubleSequence> x = regarima.getX();
        int[] missing = regarima.missing();
        int nx = regarima.getMissingValuesCount() + regarima.getVariablesCount();
        Matrix dx = Matrix.make(ndy, nx);
        double[] dy;
        // dy
        if (d > 0) {
            dy = new double[y.length() - d];
            ur.apply(y, DataBlock.ofInternal(dy));
        } else {
            dy = y.toArray();
        }
        // dx
        if (nx > 0) {
            DataBlockIterator cols = dx.columnsIterator();
            if (d > 0) {
                if (missing.length > 0) {
                    DoubleSequence coeff = ur.asPolynomial().coefficients().reverse();
                    for (int i = 0; i < missing.length; ++i) {
                        DataBlock col = cols.next();
                        if (missing[i] >= dy.length) {
                            col.range(missing[i] - d, dy.length).copy(coeff.drop(0, y.length() - missing[i]));
                        } else if (missing[i] >= d) {
                            col.range(missing[i] - d, missing[i] + 1).copy(coeff);
                        } else {
                            col.range(0, missing[i] + 1).copy(coeff.drop(d - missing[i], 0));
                        }
                    }
                }
                if (mean) {
                    cols.next().set(1);
                }
                for (DoubleSequence var : x) {
                    ur.apply(var, cols.next());
                }
            } else {
                for (int i = 0; i < missing.length; ++i) {
                    cols.next().set(missing[i], 1);
                }
                if (mean) {
                    cols.next().set(1);
                }
                for (DoubleSequence var : x) {
                    ur.apply(var, cols.next());
                }
            }
        }
        return new RegArmaModel<>(DoubleSequence.ofInternal(dy), arma, dx, missing.length);

    }

    /**
     * The differenced (interpolated) observations. Should not contain missing values
     * (handled by additive outliers)
     */
    @lombok.NonNull
    DoubleSequence y;
    /**
     * The stationary model
     */
    @lombok.NonNull
    M arma;
    /**
     * The differenced regression variables. Contains successively
     * the additive outliers corresponding to the missing values,
     * the constant
     * the other regression variables
     */
    MatrixType x;
    /**
     * Number of missing observations (additive outliers at the beginning of x)
     */
    int missingCount;
    
    public LinearModel asLinearModel(){
        return new LinearModel(y.toArray(), false, Matrix.of(x));
    }
    
}