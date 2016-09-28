package com.theory;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.runners.model.FrameworkMethod;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    public static String reportPath;

    private static DateFormat dateFormat = new SimpleDateFormat("YYYYmmdd_HHMMss");

    static {
        reportPath = System.getProperty("reportPath") + File.separator + dateFormat.format(new Date());
    }

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
