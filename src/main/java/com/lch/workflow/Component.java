package com.lch.workflow;

public interface Component extends Runnable, java.lang.Thread.UncaughtExceptionHandler{

	public boolean isDone();
	
	public String getName();
	
	public Throwable getStopException();
}
