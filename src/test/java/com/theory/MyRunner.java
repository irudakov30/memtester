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

import java.io.IOException;
import java.io.StringWriter;
import java.security.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by irudakov on 24.09.2016.
 */
@Slf4j
public class MyRunner extends BlockJUnit4ClassRunner {

    public MyRunner(Class<?> klass) throws InitializationError {
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

        int i = 0;
        while (!methodStarter.isComplete()) {
            Runtime runtime = Runtime.getRuntime();
            double memoryAfter = toMb(runtime.totalMemory() - runtime.freeMemory());
            String mem = String.valueOf(memoryAfter);
            i++;

            Metric metric = Metric.builder()
                    .loopCount(i)
                    .memory(memoryAfter)
                    .timestamp(calendar.getTimeInMillis())
                    .build();

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

            if(gcPredicate.doHit(metric)) {
                System.gc();
            }
        }

        System.out.println(out.toString());

        notifier.fireTestFinished(description);
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
