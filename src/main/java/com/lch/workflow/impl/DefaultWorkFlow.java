package com.lch.workflow.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lch.workflow.Component;
import com.lch.workflow.EndComponent;
import com.lch.workflow.FilterComponent;
import com.lch.workflow.StartComponent;
import com.lch.workflow.WorkFlow;

public class DefaultWorkFlow implements WorkFlow {

	private Node start;
	private String name;
	private volatile boolean isDone = false;
	private Set<Component> components;
	private Throwable e;
	
	private final ExecutorService pool;

	private final static Logger LOGGER = LoggerFactory
			.getLogger(DefaultComputerComponent.class);

	public DefaultWorkFlow(String name) {
		this.start = new Node();
		this.start.setChildren(new ArrayList<Node>());
		this.components = new HashSet<Component>();
		pool = Executors.newCachedThreadPool();
		this.name = name;
	}

	@Override
	public void run() {
		LOGGER.info("Thread " + this.name + " start...");
		Thread.currentThread().setUncaughtExceptionHandler(this);
		long time = System.currentTimeMillis();

		for (Component component : this.components) {
			LOGGER.debug("start component thread:" + component.getName());
			this.pool.execute(component);
		}

		this.pool.shutdown();

		while (!this.pool.isTerminated()) {
			try {
				TimeUnit.SECONDS.sleep(5);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
			LOGGER.info(this.name + " worflow is running...");
		}

		this.isDone = true;

		LOGGER.info(this.name + " workflow run over! Time:"
				+ (System.currentTimeMillis() - time) + "Millis");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Collection<T> getNotProcessed() {
		Collection<T> vector = new Vector<T>(256);
		Iterator<Component> iterator = this.components.iterator();
		while (iterator.hasNext()) {
			Component component = iterator.next();
			if (component instanceof FilterComponent) {
				FilterComponent<T> filter = (FilterComponent<T>) component;
				synchronized (filter.getUnProcessedCollection()) {
					vector.addAll(filter.getUnProcessedCollection());
					filter.getUnProcessedCollection().clear();
				}
				if (filter.isDone()) {
					iterator.remove();
				}
			}

		}
		return vector;
	}

	@Override
	public void register(Component previouse, Component c) {
		Node node = Node.getNode(this.start, previouse.getName());
		if (node != null) {
			if(this.components.contains(c)){
				return;
			}
			Node newnode = new Node();
			newnode.setNode(c);
			newnode.setChildren(new ArrayList<Node>());
			node.getChildren().add(newnode);
			addComponent(c);
		} else {
			LOGGER.error("not found previouse node!");
			throw new NoSuchElementException(previouse.getName());
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
	public <T> void registerStart(StartComponent<T> component) {
		this.start.setNode(component);
		addComponent(component);
	}

	@Override
	public <T> BlockingQueue<T> registerEnd(EndComponent<T> component) {
		this.components.add(component);
		return component.getOutputQueue();
	}

	private void addComponent(Component c) {
		this.components.add(c);
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		LOGGER.error(t.getName() + " stopped, because of uncaught exception", e);
		this.e = e;
		this.isDone = true;
		if((this.pool.shutdownNow()).size() != 0){
			try {
				this.pool.awaitTermination(1, TimeUnit.MINUTES);
			} catch (InterruptedException e1) {
				LOGGER.error("", e);
			}
			if((this.pool.shutdownNow()).size() != 0){
				LOGGER.error("workflow stopped but has some threads that is running.");
			}
		}
	}

	@Override
	public Throwable getStopException() {
		return this.e;
	}

}

class Node {
	private Component node;
	private List<Node> children;

	public Component getNode() {
		return node;
	}

	public void setNode(Component node) {
		this.node = node;
	}

	public List<Node> getChildren() {
		return children;
	}

	public void setChildren(List<Node> children) {
		this.children = children;
	}

	public static Node getNode(Node node, String name) {
		if (node.getNode().getName().equals(name))
			return node;

		for (Node child : node.getChildren()) {
			if (child.getNode().getName().equals(name)) {
				return child;
			}
		}

		for (Node child : node.getChildren()) {
			Node result = getNode(child, name);
			if (result != null) {
				return result;
			}
		}

		return null;
	}
}
