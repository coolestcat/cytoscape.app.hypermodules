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
	private HashMap<String, String> parameters;
	private ArrayList<String[]> sampleValues;
	private ArrayList<String[]> clinicalValues;
	
	public OpenResultsTaskFactory(HashMap<String, String> parameters, CytoscapeUtils utils, HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults, CyNetwork network, ArrayList<String[]> sampleValues, ArrayList<String[]> clinicalValues){
		this.utils = utils;
		this.allResults = allResults;
		this.network = network;
		this.parameters = parameters;
		this.sampleValues = sampleValues;
		this.clinicalValues = clinicalValues;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		return (new TaskIterator(new OpenResultsTask(parameters, utils, allResults, network, sampleValues, clinicalValues)));
	}

	@Override
	public boolean isReady() {
		return !utils.isResultOpened();
	}

}
