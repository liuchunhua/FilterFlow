package com.lch.workflow;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;

public interface WorkFlow extends Runnable, Component{
	
	public <T> Collection<T> getNotProcessed();
	
	public <T> void registerStart(StartComponent<T> component);
	
	public void register(Component previouse, Component c);
	
	public <T> BlockingQueue<T> registerEnd(EndComponent<T> component);
	
}
