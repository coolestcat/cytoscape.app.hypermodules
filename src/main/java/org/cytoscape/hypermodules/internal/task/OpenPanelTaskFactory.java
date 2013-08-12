package org.cytoscape.hypermodules.internal.task;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class OpenPanelTaskFactory implements TaskFactory {

	private CytoscapeUtils utils;
	private CySwingApplication swingApp;
	
	public OpenPanelTaskFactory(CySwingApplication swingApp, CytoscapeUtils utils){
		this.utils = utils;
		this.swingApp = swingApp;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator( new OpenPanelTask(swingApp, utils));
	}

	@Override
	public boolean isReady() {
		// TODO return true might mess up multithreading??
		return !utils.isMainOpened();
	}

}
