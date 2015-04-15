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

import com.lch.workflow.Component;
import com.lch.workflow.EndComponent;
import com.lch.workflow.Filter;
import com.lch.workflow.FilterComponent;
import com.lch.workflow.WorkFlow;

public class DefaultEndComponent<T> implements EndComponent<T> {

	private WorkFlow workflow;
	private BlockingQueue<T> toProcess;
	private Collection<T> unProcessed;
	private volatile boolean isDone = false;
	private String name;
	private Map<String, Component> previousMap;
	private Throwable e;

	private static final int QUEUESIZE = 512;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DefaultEndComponent.class);

	public DefaultEndComponent(WorkFlow workflow, String name) {
		this.workflow = workflow;
		this.name = name;
		this.unProcessed = new Vector<T>(256);
		this.toProcess = new ArrayBlockingQueue<T>(QUEUESIZE);
		this.previousMap = new HashMap<String, Component>();
	}

	@Override
	public Collection<T> getUnProcessedCollection() {
		return this.unProcessed;
	}

	@Override
	public void run() {
		LOGGER.info("Thread " + this.getName() + " start...");
		Thread.currentThread().setUncaughtExceptionHandler(this);
		while (!isPreviousComponentsDone()) {
			try {
				TimeUnit.MILLISECONDS.sleep(5000);
				LOGGER.debug("OutputQueue has "
						+ (QUEUESIZE - this.toProcess.remainingCapacity())
						+ " elements");
				LOGGER.debug("Workflow has " + workflow.getNotProcessed()
						+ " elements not processed");
				if (workflow.getNotProcessed().size() > 2048) {
					LOGGER.debug("workflow has too much elements not processed and clear");
					workflow.getNotProcessed().clear();
				}
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		}
		this.isDone = true;
		LOGGER.info("Thread " + this.getName() + " end");
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
		throw new UnsupportedOperationException();
	}

	@Override
	public Filter<T> getFilter() {
		return new Filter<T>() {

			@Override
			public boolean filte(T t) {
				return true;
			}
		};
	}

	@Override
	public void setFilter(Filter<T> filter) {

	}

	@Override
	public void setPrevComponent(Component component) {
		this.previousMap.put(component.getName(), component);
	}

	@Override
	public BlockingQueue<T> getOutputQueue() {
		return this.getInputBlockQueue();
	}

	private boolean isPreviousComponentsDone() {
		boolean result = true;
		for (Component c : this.previousMap.values()) {
			result = (result && c.isDone());
		}
		return result;
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
		return this.e;
	}
}
