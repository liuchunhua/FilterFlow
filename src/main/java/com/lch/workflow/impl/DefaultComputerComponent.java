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
import com.lch.workflow.Computer;
import com.lch.workflow.ComputerComponent;
import com.lch.workflow.Filter;
import com.lch.workflow.FilterComponent;
import com.lch.workflow.WorkFlow;

public class DefaultComputerComponent<T, V> implements ComputerComponent<T, V> {

	private WorkFlow workflow;
	private BlockingQueue<V> toProcess;
	private Computer<T, V> computer;
	private volatile boolean isDone = false;
	private String name;
	private Component previous;
	private QueueCache<T> cache;
	private Filter<V> filter;
	private Map<String, FilterComponent<T>> nextComponents;
	private Throwable e;

	private static final int QUEUESIZE = 256;

	private final static Logger LOGGER = LoggerFactory
			.getLogger(DefaultComputerComponent.class);

	public DefaultComputerComponent(WorkFlow workflow, String name) {
		this.workflow = workflow;
		this.name = name;
		this.toProcess = new ArrayBlockingQueue<V>(QUEUESIZE);
		this.nextComponents = new HashMap<String, FilterComponent<T>>();
		this.cache = new QueueCache<T>(this.nextComponents);
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

		LOGGER.info("Thread " + this.getName() + " start...");
		Thread.currentThread().setUncaughtExceptionHandler(this);

		while (!this.previous.isDone() || !this.toProcess.isEmpty()) {
			try {
				V element = this.toProcess.poll(1, TimeUnit.MILLISECONDS);
				if (element == null) {
					continue;
				}

				T resultObject = this.computer.compute(element);

				if (resultObject != null) {
					for (Map.Entry<String, FilterComponent<T>> entry : this.nextComponents
							.entrySet()) {
						if (entry.getValue().getFilter().filte(resultObject)) {
							this.cache.addNextQueue(entry.getKey(), resultObject);
						}
					}

				} else {
					LOGGER.error(this.getName() + " compute failed:" + element);
				}
			} catch (InterruptedException e) {
				LOGGER.error("process queue of " + this.getName(), e);
			}
		}

		try {
			this.cache.clear();
		} catch (InterruptedException e) {
			LOGGER.error("process queue of " + this.getName(), e);
		}

		this.isDone = true;

		LOGGER.info("Thread " + this.getName() + " end");
	}

	public void SetComputer(Computer<T, V> c) {
		this.computer = c;
	}

	@Override
	public Collection<V> getUnProcessedCollection() {
		return new Vector<V>();
	}

	@Override
	public BlockingQueue<V> getInputBlockQueue() {
		return this.toProcess;
	}

	@Override
	public Filter<V> getFilter() {
		return this.filter;
	}

	@Override
	public void setFilter(Filter<V> filter) {
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
		this.previous = component;
	}

	@Override
	public Computer<T, V> getComputer() {
		return this.computer;
	}

	@Override
	public void setComputer(Computer<T, V> computer) {
		this.computer = computer;
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
		return e;
	}
}
