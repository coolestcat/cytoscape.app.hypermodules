package org.cytoscape.hypermodules.internal.task;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.hypermodules.internal.gui.AboutDialog;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

/**
 * 
 * Opens the about dialog
 * @author alvinleung
 *
 */
public class OpenAboutTask implements Task {

	private CytoscapeUtils utils;
	private AboutDialog aboutDialog;
	
	public OpenAboutTask(CytoscapeUtils utils){
		this.utils = utils;
	}

	
	@Override
	public void cancel() {
		
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		aboutDialog = new AboutDialog(utils);
		aboutDialog.setLocationRelativeTo(null);
		aboutDialog.setVisible(true);
	}

}
