package com.lch.workflow;


public interface StartComponent<T> extends BranchComponent<T> {
	
	public void write(T element);
	
}
