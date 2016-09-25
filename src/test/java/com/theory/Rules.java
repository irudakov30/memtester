package com.theory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by irudakov on 25.09.2016.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Rules {
    int threadsCount() default 1;
    Class<? extends GcPredicate> hitGc();
    int threadsStartDelay() default 0;
}
