package org.cytoscape.hypermodules.internal.task;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class CloseTaskFactory implements TaskFactory {

	private CytoscapeUtils utils;
	
	public CloseTaskFactory(CytoscapeUtils utils){
		this.utils = utils;
	}
	
	
	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new CloseTask(utils));
	}

	@Override
	public boolean isReady() {
		return utils.isMainOpened();
	}

}
