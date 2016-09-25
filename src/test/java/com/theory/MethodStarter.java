package com.theory;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created by irudakov on 25.09.2016.
 */
public class MethodStarter {

    private final ExecutorService executorService;
    private final List<Future> futures;
    private final int threadsCount;

    public MethodStarter(ExecutorService executorService, int threadsCount) {
        this.executorService = executorService;
        this.threadsCount = threadsCount;
        this.futures = new ArrayList<Future>();
    }

    public void start(final Statement statement , final Description description, final RunNotifier notifier) {
        for (int i = 0; i < threadsCount; i++) {
            Future future = executorService.submit(new Runnable() {
                public void run() {
                    try {
                        statement.evaluate();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                        notifier.fireTestFinished(description);
                    }
                }
            });
            futures.add(future);
        }
    }

    public boolean isComplete() {
        int size = futures.size();
        int doneCount = 0;
        for (Future future : futures) {
            doneCount += future.isDone() ? 1 : 0;
        }

        return size == doneCount;
    }
}
