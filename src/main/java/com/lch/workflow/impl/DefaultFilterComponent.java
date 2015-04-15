package com.lch.workflow.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lch.workflow.BranchComponent;
import com.lch.workflow.Component;
import com.lch.workflow.Filter;
import com.lch.workflow.FilterComponent;
import com.lch.workflow.WorkFlow;

public class DefaultFilterComponent<T> implements BranchComponent<T> {

	private WorkFlow workflow;
	private BlockingQueue<T> toProcess;
	private Filter<T> filter;
	private Map<String, FilterComponent<T>> nextComponents;
	private Collection<T> unProcessed;
	private volatile boolean isDone = false;
	private Component previous;
	private String name;
	private QueueCache<T> cache;
	private int processSize = 0;
	private int unprocessSize = 0;
	private Throwable e;

	private static final int CACHESIZE = 256;
	private static final int QUEUESIZE = 256;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DefaultFilterComponent.class);

	public DefaultFilterComponent(WorkFlow workflow, String name) {
		this.workflow = workflow;
		this.name = name;
		this.nextComponents = new HashMap<String, FilterComponent<T>>();
		this.unProcessed = new Vector<T>(256);
		this.cache = new QueueCache<T>(this.nextComponents);
		this.toProcess = new ArrayBlockingQueue<T>(QUEUESIZE);
	}

	@Override
	public Collection<T> getUnProcessedCollection() {
		return this.unProcessed;
	}

	@Override
	public void run() {
		LOGGER.info("Thread " + this.getName() + " start...");
		Thread.currentThread().setUncaughtExceptionHandler(this);
		while (!this.previous.isDone() || !this.toProcess.isEmpty()) {
			try {
				T element = this.toProcess.poll(1, TimeUnit.MILLISECONDS);
				if (element == null)
					continue;
				filterChain(element);
			} catch (InterruptedException e) {
				LOGGER.error("process queue of " + this.getName()
						+ "FilterComponent", e);
			}
		}

		try {
			this.cache.clear();
		} catch (InterruptedException e) {
			LOGGER.error("process queue of " + this.getName()
					+ "FilterComponent", e);
		}

		this.isDone = true;
		LOGGER.info("Thread " + this.getName() + " end, process " + processSize + " elements, not process " + unprocessSize + "elements");
	}

	protected void filterChain(T element) throws InterruptedException{
		boolean isProcessed = false;
		for (Map.Entry<String, FilterComponent<T>> entry : this.nextComponents
				 .entrySet()) {
			if (entry.getValue().getFilter().filte(element)) {
				this.cache.addNextQueue(entry.getKey(), element);
				isProcessed = true;
			}
		}
		if (!isProcessed){
			this.unProcessed.add(element);
			unprocessSize++;
			if(LOGGER.isDebugEnabled() && unprocessSize % CACHESIZE == 0){
				LOGGER.debug("unprocessed elements: " + unprocessSize);
			}
		}else{
			processSize++;
		}
	}

	@Override
	public boolean isDone() {
		return this.isDone;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public BlockingQueue<T> getInputBlockQueue() {
		return this.toProcess;
	}

	@Override
	public void registe(FilterComponent<T> filterComponent) {
		this.workflow.register(this, filterComponent);
		filterComponent.setPrevComponent(this);
		this.nextComponents.put(filterComponent.getName(), filterComponent);
	}

	@Override
	public Filter<T> getFilter() {
		return this.filter;
	}

	@Override
	public void setFilter(Filter<T> filter) {
		this.filter = filter;
	}

	@Override
	public void setPrevComponent(Component component) {
		this.previous = component;
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		LOGGER.error(t.getName() + " stopped, because of uncaught exception", e);
		this.e = e;
		this.isDone = true;
		while(this.getInputBlockQueue().poll() != null){}		
	}

	@Override
	public Throwable getStopException() {
		return e;
	}

}
