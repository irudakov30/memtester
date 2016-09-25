package com.theory;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by irudakov on 24.09.2016.
 */
@Slf4j
public class MyRunner extends BlockJUnit4ClassRunner {

    private static final int THREADS_COUNT = 5;
    private static final ExecutorService executor = Executors.newFixedThreadPool(THREADS_COUNT);

    public MyRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected void runChild(final FrameworkMethod method, final RunNotifier notifier) {
        final Statement statement = super.methodBlock(method);
        final Description description = this.describeChild(method);

        Rules rules = method.getAnnotation(Rules.class);
        int threadsCount = rules.threadsCount();

        log.info("Start");

        Class<? extends GcPredicate> gcPredicateClass =  rules.hitGc();
        GcPredicate gcPredicate = null;
        try {
            gcPredicate = gcPredicateClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        notifier.fireTestStarted(description);

        MethodStarter methodStarter = new MethodStarter(executor, THREADS_COUNT);
        try {
            methodStarter.start(statement, description, notifier);
        } catch (Exception e) {
            notifier.fireTestFailure(new Failure(description, e));
            return;
        }

        StringWriter out = new StringWriter();
        CSVPrinter csvPrinter = createCsvPrinter(out, getDescription(), notifier);

        int i = 0;
        while (!methodStarter.isComplete()) {
            Runtime runtime = Runtime.getRuntime();
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            String mem = String.valueOf(memoryAfter);
            i++;

            try {
                csvPrinter.printRecord(i, mem);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(gcPredicate.doHit(memoryAfter)) {
                System.gc();
            }
        }

        System.out.println(out.toString());

        notifier.fireTestFinished(description);
    }

    private boolean isAllComplete(List<Future> futures) {
        int size = futures.size();
        int doneCount = 0;
        for (Future future : futures) {
            doneCount += future.isDone() ? 1 : 0;
        }

        return size == doneCount;
    }

    private CSVPrinter createCsvPrinter(Appendable appendable, Description description, RunNotifier runNotifier) {
        try {
            return CSVFormat.DEFAULT.withHeader("Loop", "Memory").print(appendable);
        } catch (IOException e) {
            runNotifier.fireTestFailure(new Failure(description, e));
            return null;
        }
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
