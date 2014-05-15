package org.cytoscape.hypermodules.internal.task;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class OpenLoadTaskFactory implements TaskFactory{

	private CytoscapeUtils utils;
	
	public OpenLoadTaskFactory (CytoscapeUtils utils){
		this.utils = utils;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new OpenLoadTask(utils));
	}

	@Override
	public boolean isReady() {
		return true;
	}

}
