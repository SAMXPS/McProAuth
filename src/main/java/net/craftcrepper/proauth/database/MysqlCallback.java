package net.craftcrepper.proauth.database;

public abstract interface MysqlCallback<T> {
	
    public abstract void onResult(T data, Throwable error);
    
    public default void onResult(T result) {
       onResult(result, null);
    }
}