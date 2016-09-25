package com.theory;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by irudakov on 24.09.2016.
 */
@RunWith(MemoryAnalizer.class)
public class MemTest {

    @Test
    @MemoryAnalizerParams(threadsCount = 5, hitGc = MyPred.class, snapshotDelayMs = 100)
    public void test() throws InterruptedException {
        MemInsane memInsane = new MemInsane();
        memInsane.doS();
    }

    public static final class MyPred implements GcPredicate {
        public boolean doHit(Metric metric) {
            return metric.getMemory() > 12;
        }
    }
}
