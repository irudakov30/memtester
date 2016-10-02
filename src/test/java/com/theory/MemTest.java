package com.theory;

import com.github.memtester.junit.runner.GcPredicate;
import com.github.memtester.junit.runner.MemoryAnalizer;
import com.github.memtester.junit.runner.MemoryAnalizerParams;
import com.github.memtester.junit.runner.Metric;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by irudakov on 24.09.2016.
 */
@RunWith(MemoryAnalizer.class)
public class MemTest {

    @Test
    @MemoryAnalizerParams(threadsCount = 5, hitGc = MyPred.class, snapshotDelayMs = 100)
    public void newThreadPoolTest() throws InterruptedException {
        MemInsane memInsane = new MemInsane();
        memInsane.doS();
    }

    @Test
    @MemoryAnalizerParams(threadsCount = 5, hitGc = MyPred.class, snapshotDelayMs = 100)
    public void commonThreadPoolTest() throws InterruptedException {
        MemInsane memInsane = new MemInsane();
        memInsane.doS2();
    }

    public static final class MyPred extends GcPredicate {
        public boolean doHit(Metric metric) {
            return metric.getMemory() > 12;
        }
    }
}
