package com.lch.workflow;


public interface ComputerComponent<T, V> extends FilterComponent<V>, Register<T>{

	public Computer<T, V> getComputer();
	
	public void setComputer(Computer<T, V> computer);
	
}
