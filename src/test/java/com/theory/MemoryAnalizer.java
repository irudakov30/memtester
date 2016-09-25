package com.theory;

import lombok.extern.slf4j.Slf4j;
import net.sf.dynamicreports.report.exception.DRException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by irudakov on 24.09.2016.
 */
@Slf4j
public class MemoryAnalizer extends BlockJUnit4ClassRunner {

    public MemoryAnalizer(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected void runChild(final FrameworkMethod method, final RunNotifier notifier) {
        final Statement statement = super.methodBlock(method);
        final Description description = this.describeChild(method);
        MemoryAnalizerConfig memoryAnalizerConfig = MemoryAnalizerConfig.create(method);

        notifier.fireTestStarted(description);

        ExecutorService executor = Executors.newFixedThreadPool(memoryAnalizerConfig.getThreadsCount());
        MethodStarter methodStarter = new MethodStarter(executor, memoryAnalizerConfig);

        if (!startTestMethod(notifier, statement, description, methodStarter)) {
            return;
        }

        GcPredicate gcPredicate = memoryAnalizerConfig.getGcPredicate();
        Calendar calendar = Calendar.getInstance();

        List<Metric> metricList = new ArrayList<Metric>();

        int loopCount = 0;
        while (!methodStarter.isComplete()) {
            Runtime runtime = Runtime.getRuntime();
            double memorySnapshot = toMb(runtime.totalMemory() - runtime.freeMemory());

            String dumpHeapFile = dumpHeap(description, memoryAnalizerConfig, loopCount);

            Metric metric = Metric.builder()
                    .loopCount(loopCount)
                    .memory(memorySnapshot)
                    .timestamp(calendar.getTimeInMillis())
                    .heapDumpFile(dumpHeapFile)
                    .build();

            if(gcPredicate.doHit(metric)) {
                log.info("Hitting GC. Current memory {}", memorySnapshot);
                System.gc();
                metric.setGcInvoke(1);
            }

            metricList.add(metric);

            loopCount++;

            if (!delay(notifier, description, memoryAnalizerConfig)) {
                return;
            }
        }

        Report report = Report.builder().memoryAnalizerConfig(memoryAnalizerConfig).metrics(metricList).build();
        try {
            report.generate();
        } catch (DRException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        notifier.fireTestFinished(description);
    }

    private String dumpHeap(Description description, MemoryAnalizerConfig memoryAnalizerConfig, int loopCount) {
        String reportPath = memoryAnalizerConfig.getReportPath();
        String file = reportPath + File.separator + description.getDisplayName() + "_" + loopCount + ".hprof";
        HeapDump.dumpHeap(file, true);
        return file;
    }

    private boolean delay(RunNotifier notifier, Description description, MemoryAnalizerConfig memoryAnalizerConfig) {
        try {
            Thread.sleep(memoryAnalizerConfig.getSnapshotDelayMs());
        } catch (InterruptedException e) {
            notifier.fireTestFailure(new Failure(description, e));
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }

    private boolean startTestMethod(RunNotifier notifier, Statement statement, Description description, MethodStarter methodStarter) {
        try {
            methodStarter.start(statement, description, notifier);
        } catch (Exception e) {
            notifier.fireTestFailure(new Failure(description, e));
            return false;
        }
        return true;
    }

    private double toMb(long memory) {
        return (double)(memory) / (double)(1024 * 1024);
    }

}
