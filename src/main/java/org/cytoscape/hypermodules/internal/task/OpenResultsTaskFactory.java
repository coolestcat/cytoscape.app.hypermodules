package org.cytoscape.hypermodules.internal.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import com.google.common.collect.Multimap;

public class OpenResultsTaskFactory implements TaskFactory {

	private HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults;
	private CytoscapeUtils utils;
	private CyNetwork network;
	
	public OpenResultsTaskFactory(CytoscapeUtils utils, HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults, CyNetwork network){
		this.utils = utils;
		this.allResults = allResults;
		this.network = network;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		return (new TaskIterator(new OpenResultsTask(utils, allResults, network)));
	}

	@Override
	public boolean isReady() {
		return !utils.isResultOpened();
	}

}
