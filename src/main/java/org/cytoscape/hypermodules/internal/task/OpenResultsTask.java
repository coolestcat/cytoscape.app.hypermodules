package org.cytoscape.hypermodules.internal.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.hypermodules.internal.gui.ResultsPanel;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

import com.google.common.collect.Multimap;

public class OpenResultsTask implements Task {
	
	private HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults;
	private CytoscapeUtils utils;
	private CyNetwork network;
	private HashMap<String, String> parameters;
	
	public OpenResultsTask(HashMap<String, String> parameters, CytoscapeUtils utils, HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults, CyNetwork network){
		this.utils = utils;
		this.allResults = allResults;
		this.network = network;
		this.parameters = parameters;
	}
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		ResultsPanel resultsPanel = new ResultsPanel(parameters, utils, allResults, network);
		utils.serviceRegistrar.registerService(resultsPanel, CytoPanelComponent.class, new Properties());
		CytoPanel eastPanel = utils.getCytoPanelEast();
		eastPanel.setState(CytoPanelState.DOCK);
	}

	@Override
	public void cancel() {
		
	}

}
