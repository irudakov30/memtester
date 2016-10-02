package com.github.memtester.junit.runner;

/**
 * Created by irudakov on 25.09.2016.
 */
public abstract class GcPredicate {
    public abstract boolean doHit(Metric metric);

    final boolean validate(Metric metric) {
        boolean is = doHit(metric);
        if(is) {
            System.gc();
            metric.setGcInvoke(1);
        }
        return is;
    }
}
