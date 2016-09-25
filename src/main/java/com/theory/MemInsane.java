package com.theory;

import java.util.concurrent.ForkJoinPool;

/**
 * Created by irudakov on 24.09.2016.
 */
public class MemInsane {

    public void doS() {
        ForkJoinPool pool = new ForkJoinPool();

        for(int i = 0; i < 100; i++) {
            pool.execute(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(25000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
