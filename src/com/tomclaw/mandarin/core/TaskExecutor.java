package com.tomclaw.mandarin.core;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 31.10.13
 * Time: 10:56
 */
public class TaskExecutor {

    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private final BlockingQueue<Runnable> mDecodeWorkQueue = new LinkedBlockingQueue<Runnable>();
    private final ThreadPoolExecutor mThreadPoolExecutor = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES,
            KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mDecodeWorkQueue);

    private static class Holder {

        static TaskExecutor instance = new TaskExecutor();
    }

    public static TaskExecutor getInstance() {
        return Holder.instance;
    }

    public void execute(final Task task) {
        MainExecutor.execute(new Runnable() {
            @Override
            public void run() {
                task.onPreExecuteMain();
                mThreadPoolExecutor.execute(task);
            }
        });
    }
}
