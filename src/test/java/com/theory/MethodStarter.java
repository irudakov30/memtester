package com.theory;

import lombok.extern.slf4j.Slf4j;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.Statement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created by irudakov on 25.09.2016.
 */
@Slf4j
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
        int delay = config.getThreadsStartDelay();
        for (int i = 0; i < config.getThreadsCount(); i++) {
            Future future = executorService.submit(new Runnable() {
                public void run() {
                    try {
                        statement.evaluate();
                    } catch (Throwable throwable) {
                        log.error("", throwable);
                        notifier.fireTestFinished(description);
                    }
                }
            });
            futures.add(future);

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
