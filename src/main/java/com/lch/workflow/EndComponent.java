package com.lch.workflow;

import java.util.concurrent.BlockingQueue;

public interface EndComponent<T> extends BranchComponent<T> {

	public BlockingQueue<T> getOutputQueue();
	
}
