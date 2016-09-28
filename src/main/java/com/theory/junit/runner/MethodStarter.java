package com.theory.junit.runner;

import lombok.extern.slf4j.Slf4j;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created by irudakov on 25.09.2016.
 */
@Slf4j
public class MethodStarter {

    private final ExecutorService executorService;
    private final List<Future> futures;
    private final MemoryAnalizerConfig config;

    public MethodStarter(ExecutorService executorService, MemoryAnalizerConfig config) {
        this.executorService = executorService;
        this.config = config;
        this.futures = new ArrayList<>();
    }

    public void start(final Statement statement) throws Exception {
        int delay = config.getThreadsStartDelay();
        for (int i = 0; i < config.getThreadsCount(); i++) {
            Future future = executorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        statement.evaluate();
                        return null;
                    } catch (Throwable throwable) {
                        log.error("", throwable);
                        throw new Exception(throwable);
                    }
                }
            });
            futures.add(future);

            if(delay > 0) {
                Thread.sleep(delay);
            }
        }
    }

    public boolean isComplete() throws Exception {
        int doneCount = 0;
        for (Future future : futures) {
            boolean isDone = future.isDone();
            if(isDone) {
                future.get(); //Check if any exceptions happened
            }

            doneCount += isDone ? 1 : 0;
        }

        return config.getThreadsCount() == doneCount;
    }
}
