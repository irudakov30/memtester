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
    private final RulesConfig config;

    public MethodStarter(ExecutorService executorService, RulesConfig config) {
        this.executorService = executorService;
        this.config = config;
        this.futures = new ArrayList<Future>();
    }

    public void start(final Statement statement, final Description description, final RunNotifier notifier) throws Exception {
        for (int i = 0; i < config.getThreadsCount(); i++) {
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

            int delay = config.getThreadsStartDelay();
            if(delay > 0) {
                Thread.sleep(delay);
            }
        }
    }

    public boolean isComplete() {
        int doneCount = 0;
        for (Future future : futures) {
            doneCount += future.isDone() ? 1 : 0;
        }

        return config.getThreadsCount() == doneCount;
    }
}
