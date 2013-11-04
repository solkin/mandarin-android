package com.tomclaw.mandarin.core;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 31.10.13
 * Time: 11:08
 */
public abstract class Task implements Runnable {

    @Override
    public void run() {
        try {
            execute();
            onSuccess();
        } catch(Throwable ex) {
            onFail();
        }
    }

    public abstract void execute() throws Throwable;
    public void onSuccess() {}
    public void onFail() {}
}
