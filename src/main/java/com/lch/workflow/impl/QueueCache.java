package com.lch.workflow.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lch.workflow.FilterComponent;

public class QueueCache<T> {
	
	private Map<String, List<T>> cache;
	
	private Map<String, FilterComponent<T>> nextComponents;
	
	private int cacheSize = 256;
	
	public QueueCache(Map<String, FilterComponent<T>> nextComponent){
		this.cache = new HashMap<String, List<T>>();
		this.nextComponents = nextComponent;
	}

	
	public int getCacheSize() {
		return cacheSize;
	}


	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}
	
	protected void addNextQueue(String name, T element)
			throws InterruptedException {
		if (this.cache.get(name) == null) {
			this.cache.put(name, new ArrayList<T>(this.getCacheSize()));
		}
		if (this.cache.get(name).size() >= this.getCacheSize()) {
			for (T t : this.cache.get(name)) {
				this.nextComponents.get(name).getInputBlockQueue().put(t);
			}
			this.cache.get(name).clear();
			this.cache.get(name).add(element);
		} else {
			this.cache.get(name).add(element);
		}
	}

	protected void clear() throws InterruptedException {
		for (Map.Entry<String, List<T>> entry : this.cache.entrySet()) {
			for (T t : entry.getValue()) {
				if (!this.nextComponents.get(entry.getKey()).isDone()) {
					this.nextComponents.get(entry.getKey())
							.getInputBlockQueue().put(t);
				}
			}
		}
		for (Map.Entry<String, List<T>> entry : this.cache.entrySet()) {
			entry.getValue().clear();
		}
	}
}
