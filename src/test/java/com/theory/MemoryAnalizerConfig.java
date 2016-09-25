package com.theory;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.runners.model.FrameworkMethod;

/**
 * Created by irudakov on 25.09.2016.
 */
@Slf4j
@Getter
public class MemoryAnalizerConfig {
    private int threadsCount;
    private GcPredicate gcPredicate;
    private int threadsStartDelay;
    private int snapshotDelayMs;
    private String reportPath;

    private MemoryAnalizerConfig() {
    }

    public static MemoryAnalizerConfig create(FrameworkMethod frameworkMethod) {
        MemoryAnalizerConfig memoryAnalizerConfig = new MemoryAnalizerConfig();

        MemoryAnalizerParams memoryAnalizerParams = frameworkMethod.getAnnotation(MemoryAnalizerParams.class);

        memoryAnalizerConfig.threadsCount = memoryAnalizerParams.threadsCount();
        memoryAnalizerConfig.threadsStartDelay = memoryAnalizerParams.threadsStartDelay();
        memoryAnalizerConfig.snapshotDelayMs = memoryAnalizerParams.snapshotDelayMs();

        Class<? extends GcPredicate> gcPredicateClass =  memoryAnalizerParams.hitGc();
        memoryAnalizerConfig.gcPredicate = initGcPredicate(gcPredicateClass);

        memoryAnalizerConfig.reportPath = System.getProperty("reportPath");

        return memoryAnalizerConfig;
    }

    private static GcPredicate initGcPredicate(Class<? extends GcPredicate> gcPredicateClass) {
        try {
            return gcPredicateClass.newInstance();
        } catch (InstantiationException e) {
            log.error("", e);
        } catch (IllegalAccessException e) {
            log.error("", e);
        }
        return null;
    }


}
