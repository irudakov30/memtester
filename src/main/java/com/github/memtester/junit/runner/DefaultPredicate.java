package com.github.memtester.junit.runner;

/**
 * Created by irudakov on 25.09.2016.
 */
public class DefaultPredicate extends GcPredicate {
    public boolean doHit(Metric metric) {
        return false;
    }
}
