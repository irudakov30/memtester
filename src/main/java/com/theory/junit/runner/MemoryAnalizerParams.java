package com.theory.junit.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by irudakov on 25.09.2016.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface MemoryAnalizerParams {
    int threadsCount() default 1;
    Class<? extends GcPredicate> hitGc() default DefaultPredicate.class;
    int threadsStartDelay() default 0;
    int snapshotDelayMs() default 500;
}
