package com.theory;

import com.sun.management.HotSpotDiagnosticMXBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import sun.security.action.GetPropertyAction;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by irudakov on 24.09.2016.
 */
@RunWith(MemoryAnalizer.class)
public class MemTest {

    @Test
    @MemoryAnalizerParams(threadsCount = 5, hitGc = MyPred.class, snapshotDelayMs = 100)
    public void test() throws InterruptedException {
        for(int i = 0; i < 10; i++) {
            Thread.sleep(500);
        }
        System.out.println("Test");
    }

    private static final File tmpdir = new File(AccessController
            .doPrivileged(new GetPropertyAction("java.io.tmpdir")));

    @Test
    public void test3() throws IOException {
        String file = tmpdir + File.separator + "123.bin";

        HotSpotDiagnosticMXBean mxBean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        mxBean.dumpHeap(file, true);

//        HeapDump.dumpHeap(file, true);

        System.out.println(file);
    }

    @Test
    public void test2() {
        Runtime runtime = Runtime.getRuntime();

        System.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        final AtomicBoolean finished = new AtomicBoolean(false);

        new Thread(new Runnable() {
            public void run() {
                MemInsane memInsane = new MemInsane();
                memInsane.doS();
                System.out.println(">>>>>>>> Finished");
                finished.getAndSet(true);
            }
        }).start();

        System.out.println(">>>> Before: " + humanReadableByteCount(memoryBefore, true));
        while (!finished.get()) {
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            System.out.println(humanReadableByteCount(memoryAfter, true));
            if(memoryAfter > 20000000) {
                System.out.println(">>>>>>>>>>>>>>>> GC");
                System.gc();
            }
        }
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static final class MyPred implements GcPredicate {
        public boolean doHit(Metric metric) {
            return metric.getMemory() > 12;
        }
    }
}
