package com.theory;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.runners.model.FrameworkMethod;

/**
 * Created by irudakov on 25.09.2016.
 */
@Slf4j
@Getter
public class RulesConfig {
    private int threadsCount;
    private GcPredicate gcPredicate;
    private int threadsStartDelay;
    private int snapshotDelayMs;
    private String reportPath;

    private RulesConfig() {
    }

    public static RulesConfig create(FrameworkMethod frameworkMethod) {
        RulesConfig rulesConfig = new RulesConfig();

        MemoryAnalizerParams memoryAnalizerParams = frameworkMethod.getAnnotation(MemoryAnalizerParams.class);

        rulesConfig.threadsCount = memoryAnalizerParams.threadsCount();
        rulesConfig.threadsStartDelay = memoryAnalizerParams.threadsStartDelay();
        rulesConfig.snapshotDelayMs = memoryAnalizerParams.snapshotDelayMs();

        Class<? extends GcPredicate> gcPredicateClass =  memoryAnalizerParams.hitGc();
        rulesConfig.gcPredicate = initGcPredicate(gcPredicateClass);

        rulesConfig.reportPath = System.getProperty("reportPath");

        return rulesConfig;
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
