package com.github.memtester.junit.runner;

import lombok.extern.slf4j.Slf4j;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.io.File;
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

    private static final List<Metric> metrics = new ArrayList<>();
    private final int testsAmount;
    private int testsProcessed;

    public MemoryAnalizer(Class<?> klass) throws InitializationError {
        super(klass);
        this.testsAmount = getChildren().size();
    }

    @Override
    protected void runChild(final FrameworkMethod method, final RunNotifier notifier) {
        final Statement statement = super.methodBlock(method);
        final Description description = this.describeChild(method);
        MemoryAnalizerConfig memoryAnalizerConfig = MemoryAnalizerConfig.create(method);

        testsProcessed++;

        try {
            notifier.fireTestStarted(description);
            runChild_(memoryAnalizerConfig, statement, description);
            notifier.fireTestFinished(description);

            if(testsProcessed == testsAmount) {
                Report report = Report.builder().memoryAnalizerConfig(memoryAnalizerConfig).metrics(metrics).build();
                log.info("About to generate a report {}", report.getReportPath());

                report.generate("Memory test report " + getTestClass());
            }

        } catch (Exception e) {
            notifier.fireTestFailure(new Failure(description, e));
        }
    }

    private void runChild_(MemoryAnalizerConfig memoryAnalizerConfig , Statement statement, Description description) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(memoryAnalizerConfig.getThreadsCount());
        MethodStarter methodStarter = new MethodStarter(executor, memoryAnalizerConfig);

        methodStarter.start(statement);

        GcPredicate gcPredicate = memoryAnalizerConfig.getGcPredicate();
        Calendar calendar = Calendar.getInstance();

        List<Metric> metricList = new ArrayList<>();

        int loopCount = 0;
        while (!methodStarter.isComplete()) {
            Runtime runtime = Runtime.getRuntime();
            double memorySnapshot = toMb(runtime.totalMemory() - runtime.freeMemory());

            String dumpHeapFile = dumpHeap(description, loopCount);

            log.debug("Heap dump save {}", dumpHeapFile);

            Metric metric = Metric.builder()
                    .testName(description.getMethodName())
                    .loopCount(loopCount)
                    .memory(memorySnapshot)
                    .timestamp(calendar.getTimeInMillis())
                    .build();

            if(gcPredicate.validate(metric)) {
                log.debug("Hitting GC. Current memory {}", memorySnapshot);
            }

            metricList.add(metric);

            loopCount++;

            Thread.sleep(memoryAnalizerConfig.getSnapshotDelayMs());
        }

        metrics.addAll(metricList);

        log.info("GC was invoked {} times", metricList.stream().mapToInt(Metric::getGcInvoke).sum());
    }

    private String dumpHeap(Description description, int loopCount) {
        String reportPath = MemoryAnalizerConfig.reportPath;
        String folderPath = reportPath + File.separator + description.getMethodName();

        boolean isMkDir = new File(folderPath).mkdirs();

        String file = reportPath + File.separator + description.getMethodName() + File.separator + loopCount + ".hprof";
        HeapDump.dumpHeap(file, true);
        return file;
    }

    private double toMb(long memory) {
        return (double)(memory) / (double)(1024 * 1024);
    }

}
