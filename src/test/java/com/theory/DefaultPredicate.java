package com.theory;

/**
 * Created by irudakov on 25.09.2016.
 */
public class DefaultPredicate implements GcPredicate {
    public boolean doHit(Metric metric) {
        return false;
    }
}