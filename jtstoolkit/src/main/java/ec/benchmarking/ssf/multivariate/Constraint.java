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

package ec.benchmarking.ssf.multivariate;

import java.util.HashMap;

/**
 *
 * @author Jean Palate
 */
public class Constraint {

    /**
     *
     */
    public final int[] index;
    /**
     *
     */
    public final double[] weights;


    /**
     * 
     * @param cnt
     */
    public Constraint(HashMap<Integer, Double> cnt) {
        index = new int[cnt.size()];
        int j = 0;
        for (Integer i : cnt.keySet()) {
            index[j++] = i;
        }
        java.util.Arrays.sort(index);
        weights = new double[index.length];
        for (int i = 0; i < index.length; ++i) {
            weights[i] = cnt.get(index[i]);
        }
    }
}
