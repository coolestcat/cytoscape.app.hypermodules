package org.cytoscape.hypermodules.internal.task;

import java.util.ArrayList;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class AlgorithmTaskFactory implements TaskFactory{

	private CytoscapeUtils utils;
	private String lengthOption;
	private String expandOption;
	private String statTest;
	private ArrayList<String[]> sampleValues;
	private ArrayList<String[]> clinicalValues;
	private ArrayList<String[]> otherValues;
	private int nShuffled;
	private CyNetwork network;
	
	public AlgorithmTaskFactory(int nShuffled, String lengthOption, String expandOption, String statTest, ArrayList<String[]> sampleValues, ArrayList<String[]> clinicalValues, ArrayList<String[]> otherValues, CytoscapeUtils utils){
		this.utils = utils;
		this.lengthOption = lengthOption;
		this.expandOption = expandOption;
		this.statTest = statTest;
		this.sampleValues = sampleValues;
		this.otherValues = otherValues;
		this.nShuffled = nShuffled;
	}
	
	
	@Override
	public TaskIterator createTaskIterator() {
		return null;
		//return (new TaskIterator(new AlgorithmTask(nShuffled, lengthOption, expandOption, statTest, sampleValues, clinicalValues, otherValues, utils)));
	}

	@Override
	public boolean isReady() {		
		return true;
	}

}
