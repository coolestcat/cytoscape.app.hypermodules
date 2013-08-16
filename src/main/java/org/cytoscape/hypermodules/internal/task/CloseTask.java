package org.cytoscape.hypermodules.internal.task;

import java.util.HashSet;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.hypermodules.internal.gui.MainPanel;
import org.cytoscape.hypermodules.internal.gui.ResultsPanel;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

/**
 * 
 * Closes the main hypermodules along with all the results panels generated from running the algorithm.
 * @author alvinleung
 *
 */
public class CloseTask implements Task {
	private CytoscapeUtils utils;
	
	public CloseTask(CytoscapeUtils utils){
		this.utils = utils;
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run(TaskMonitor monitor) throws Exception {
		MainPanel mp = utils.getMainPanel();
		utils.serviceRegistrar.unregisterService(mp, CytoPanelComponent.class);
		if (utils.isResultOpened()){
			HashSet<ResultsPanel> rph = utils.getAllResultsPanels();
			for (ResultsPanel rp : rph){
				utils.serviceRegistrar.unregisterService(rp, CytoPanelComponent.class);
			}
		}
		CytoPanel eastPanel = utils.getCytoPanelEast();
		if (eastPanel.getCytoPanelComponentCount() == 1){
			eastPanel.setState(CytoPanelState.HIDE);
		}

		}
}
