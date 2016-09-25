package com.theory;

import lombok.Builder;
import lombok.Data;

/**
 * Created by irudakov on 25.09.2016.
 */
@Builder
@Data
public class Metric {
    private long timestamp;
    private int loopCount;
    private double memory;
}
