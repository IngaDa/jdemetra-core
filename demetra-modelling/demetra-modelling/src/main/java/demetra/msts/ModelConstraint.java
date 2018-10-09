/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demetra.msts;

import demetra.msts.LoadingParameter;
import demetra.msts.MstsMapping;
import demetra.msts.VarianceParameter;
import demetra.ssf.ISsfLoading;
import demetra.ssf.implementations.MultivariateCompositeSsf;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author palatej
 */
public class ModelConstraint implements ModelItem {

    private final String name;
    private final List<Item> items = new ArrayList<>();
    private final double value;

    @lombok.Value
    public static class Item {

        String cmp;
        double c;
        boolean fixed;
        ISsfLoading loading;
    }

    public ModelConstraint(String name, double value) {
        this.name = name;
        this.value=value;
    }

    public String getName() {
        return name;
    }

    public void add(String item) {
        items.add(new Item(item, 1.0, true, null));
    }

    public void add(String item, double coeff, boolean fixed, ISsfLoading loading) {
        items.add(new Item(item, coeff, fixed, loading));
    }


    public double getValue() {
        return value;
    }

    public int getItemsCount() {
        return items.size();
    }

    public Item getItem(int pos) {
        return items.get(pos);
    }

    @Override
    public void addTo(MstsMapping mapping) {
        for (Item item : items) {
            if (!item.fixed) {
                mapping.add(new LoadingParameter(item.cmp + "_c", item.c, item.fixed));
            }
        }
        mapping.add((p, builder) -> {
            int pos = 0;
            double v = p.get(pos++);
            MultivariateCompositeSsf.Equation eq = new MultivariateCompositeSsf.Equation(v);
            for (Item item : items) {
                double c = item.c;
                if (!item.fixed) {
                    c = p.get(pos++);
                }
                eq.add(new MultivariateCompositeSsf.Item(item.cmp, c, item.loading));
            }
            builder.add(eq);
            return pos;
        });
    }

}
