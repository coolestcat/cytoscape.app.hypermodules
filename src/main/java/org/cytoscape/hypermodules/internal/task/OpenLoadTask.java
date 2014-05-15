package org.cytoscape.hypermodules.internal.task;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.hypermodules.internal.gui.LoadResultsPanel;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

public class OpenLoadTask implements Task{

	private CytoscapeUtils utils;	
	private LoadResultsPanel lrp;
	
	public OpenLoadTask(CytoscapeUtils utils) {
		this.utils = utils;
	}

	@Override
	public void cancel() {

		
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		lrp = new LoadResultsPanel(utils);
		lrp.setLocationRelativeTo(null);
		lrp.setVisible(true);
	}

}
