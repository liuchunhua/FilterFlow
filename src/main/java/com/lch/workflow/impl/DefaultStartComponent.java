package com.lch.workflow.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lch.workflow.Component;
import com.lch.workflow.Filter;
import com.lch.workflow.FilterComponent;
import com.lch.workflow.StartComponent;
import com.lch.workflow.WorkFlow;

public abstract class DefaultStartComponent<T> implements StartComponent<T> {

	private WorkFlow workflow;
	private String name;
	private Collection<T> unProcessed;
	private Filter<T> filter;
	private Map<String, FilterComponent<T>> nextComponents;
	private volatile boolean isDone = false;
	private QueueCache<T> cache;
	private int processSize = 0;
	private Throwable e = null;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DefaultStartComponent.class);

	public DefaultStartComponent(WorkFlow workflow, String name) {
		this.workflow = workflow;
		this.name = name;
		this.unProcessed = new Vector<T>(256);
		this.nextComponents = new HashMap<String, FilterComponent<T>>();
		this.cache = new QueueCache<T>(nextComponents);
	}

	@Override
	public Collection<T> getUnProcessedCollection() {
		return this.unProcessed;
	}

	@Override
	public BlockingQueue<T> getInputBlockQueue() {
		return null;
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
	public void registe(FilterComponent<T> filterComponent) {
		this.workflow.register(this, filterComponent);
		filterComponent.setPrevComponent(this);
		this.nextComponents.put(filterComponent.getName(), filterComponent);
	}

	@Override
	public void setPrevComponent(Component component) {

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
	public void run() {
		LOGGER.info("StatComponent is starting...");
		Thread.currentThread().setUncaughtExceptionHandler(this);
		
		process();
		try {
			this.cache.clear();
		} catch (InterruptedException e) {
			LOGGER.error("", e);
		}
		this.isDone = true;
		LOGGER.info("StartComponent run over. process " + processSize
				+ " elements");
	}

	public abstract void process();

	@Override
	public void write(T element) {
		processSize++;
		try {
			boolean isProcessed = false;
			for (Map.Entry<String, FilterComponent<T>> entry : this.nextComponents
					.entrySet()) {
				if (entry.getValue().getFilter().filte(element)) {
					this.cache.addNextQueue(entry.getKey(), element);
					isProcessed = true;
					// break;
				}
			}
			if (!isProcessed)
				this.unProcessed.add(element);
		} catch (InterruptedException e) {
			LOGGER.error("write  " + this.getName() + " failed.", e);
		}

	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		LOGGER.error(t.getName() + " stopped, because of uncaught exception", e);
		this.e = e;
		this.isDone = true;
		while (this.getInputBlockQueue().poll() != null) {
		}
	}

	@Override
	public Throwable getStopException() {
		return this.e;
	}
}
