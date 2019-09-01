package net.craftcrepper.proauth.scheduler;

public interface FutureController {

    public abstract void runAsync(FutureRunnable run);

}