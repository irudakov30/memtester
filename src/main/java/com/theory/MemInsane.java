package com.theory;

import java.util.concurrent.ForkJoinPool;

/**
 * Created by irudakov on 24.09.2016.
 */
public class MemInsane {

    public void doS() {
        for(int i = 0; i < 10000; i++) {
            ForkJoinPool pool = new ForkJoinPool();
            pool.execute(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(1000000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
