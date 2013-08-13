package org.cytoscape.hypermodules.internal.task;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class OpenVisualizeTaskFactory implements TaskFactory {

	private CytoscapeUtils utils;
	
	public OpenVisualizeTaskFactory(CytoscapeUtils utils){
		this.utils = utils;
	}
	
	
	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new OpenVisualizeTask(utils));
	}

	@Override
	public boolean isReady() {
		return true;
	}

}
