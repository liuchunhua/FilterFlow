package com.lch.workflow;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
/**
 * 
 * @author liuchunhua
 * 
 * 筛选器
 *
 * @param <T>
 */
public interface FilterComponent<T> extends Component{
	
	public Collection<T> getUnProcessedCollection();
	
	public BlockingQueue<T> getInputBlockQueue();
	
	public Filter<T> getFilter();
	
	public void setFilter(Filter<T> filter);
	
	public void setPrevComponent(Component component);
		
}
