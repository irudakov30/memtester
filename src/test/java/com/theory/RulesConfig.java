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

    private RulesConfig() {
    }

    public static RulesConfig create(FrameworkMethod frameworkMethod) {
        RulesConfig rulesConfig = new RulesConfig();

        Rules rules = frameworkMethod.getAnnotation(Rules.class);

        rulesConfig.threadsCount = rules.threadsCount();
        rulesConfig.threadsStartDelay = rules.threadsStartDelay();

        Class<? extends GcPredicate> gcPredicateClass =  rules.hitGc();
        rulesConfig.gcPredicate = initGcPredicate(gcPredicateClass);
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
