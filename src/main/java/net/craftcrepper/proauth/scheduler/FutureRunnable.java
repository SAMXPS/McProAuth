package net.craftcrepper.proauth.scheduler;

public abstract class FutureRunnable implements Runnable {

    public abstract void run();

    public void runAsync(FutureController cont) {
        cont.runAsync(this);
    }
 
}