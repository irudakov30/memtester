package com.github.memtester.junit.runner;

import lombok.Builder;
import lombok.Data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by irudakov on 25.09.2016.
 */
@Builder
@Data
public class Metric {
    private String testName;
    private long timestamp;
    private int loopCount;
    private double memory;
    private int gcInvoke;

    private static final ThreadLocal<DateFormat> t = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("YYYY-mm-dd HH:MM:ss");
        }
    };

    public String getTime() {
        return t.get().format(new Date(timestamp));
    }
}
