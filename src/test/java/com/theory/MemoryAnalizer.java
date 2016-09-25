package com.theory;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
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
        RulesConfig rulesConfig = RulesConfig.create(method);

        notifier.fireTestStarted(description);

        ExecutorService executor = Executors.newFixedThreadPool(rulesConfig.getThreadsCount());
        MethodStarter methodStarter = new MethodStarter(executor, rulesConfig);

        if (!startTestMethod(notifier, statement, description, methodStarter)) {
            return;
        }

        StringWriter out = new StringWriter();
        CSVPrinter csvPrinter = createCsvPrinter(out, getDescription(), notifier);

        GcPredicate gcPredicate = rulesConfig.getGcPredicate();
        Calendar calendar = Calendar.getInstance();

        int loopCount = 0;
        while (!methodStarter.isComplete()) {
            Runtime runtime = Runtime.getRuntime();
            double memorySnapshot = toMb(runtime.totalMemory() - runtime.freeMemory());
            String mem = String.valueOf(memorySnapshot);

            Metric metric = Metric.builder()
                    .loopCount(loopCount)
                    .memory(memorySnapshot)
                    .timestamp(calendar.getTimeInMillis())
                    .build();

            try {
                csvPrinter.printRecord(loopCount, mem);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!delay(notifier, description, rulesConfig)) {
                return;
            }

            if(gcPredicate.doHit(metric)) {
                log.info("Hitting GC. Current memory {}", memorySnapshot);
                System.gc();
            }

            String reportPath = rulesConfig.getReportPath();
            HeapDump.dumpHeap(reportPath + File.separator + description.getDisplayName() + "_" + loopCount + ".hprof", true);

            loopCount++;
        }

        notifier.fireTestFinished(description);
    }

    private boolean delay(RunNotifier notifier, Description description, RulesConfig rulesConfig) {
        try {
            Thread.sleep(rulesConfig.getSnapshotDelayMs());
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
        double usedMemory = (double)(memory) / (double)(1024 * 1024);
        return usedMemory;
    }

    private CSVPrinter createCsvPrinter(Appendable appendable, Description description, RunNotifier runNotifier) {
        try {
            return CSVFormat.DEFAULT.withHeader("Loop", "Memory").print(appendable);
        } catch (IOException e) {
            runNotifier.fireTestFailure(new Failure(description, e));
            return null;
        }
    }

}
