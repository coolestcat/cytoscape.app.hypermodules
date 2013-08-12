package org.cytoscape.hypermodules.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.work.TaskMonitor;

import com.google.common.collect.Multimap;

public class OriginalTest {
	
	private CytoscapeUtils utils;
	private String lengthOption;
	private String expandOption;
	private String statTest;
	private ArrayList<String[]> sampleValues;
	private ArrayList<String[]> clinicalValues;
	private ArrayList<String[]> otherValues;
	private CyNetwork network;
	private TaskMonitor tm;
	
	public OriginalTest(String lengthOption, String expandOption, String statTest, ArrayList<String[]> sampleValues, ArrayList<String[]> clinicalValues, ArrayList<String[]> otherValues, CytoscapeUtils utils, TaskMonitor tm, CyNetwork network){
		this.utils = utils;
		this.lengthOption = lengthOption;
		this.expandOption = expandOption;
		this.statTest = statTest;
		this.sampleValues = sampleValues;
		this.otherValues = otherValues;
		this.clinicalValues = clinicalValues;
		this.tm = tm;
		this.network = network;
	}
	
	public HashMap<String, HashMap<String, Double>> testHighOrLow(HashMap<String, HashMap<String, Double>> ot){
		HashMap<String, HashMap<String, Double>> rt = new HashMap<String, HashMap<String, Double>>();
		HypermodulesHeuristicAlgorithm ha = new HypermodulesHeuristicAlgorithm(this.statTest, this.sampleValues, this.clinicalValues, this.otherValues, this.network);
		ha.initialize();
		for (String s : ot.keySet()){
			HashMap<String, Double> newMap = new HashMap<String, Double>();
			for (String t : ot.get(s).keySet()){
				//System.out.println(t);
				if (ha.testModuleBoolean(t)==1){
					newMap.put(t, 1.0);
				}
				else if (ha.testModuleBoolean(t)==0){
					newMap.put(t, 0.0);
				}
				else{
					newMap.put(t, 2.0);
				}
			}
			rt.put(s, newMap);
		}
		
		return rt;
	}
	
	
	public HashMap<String, HashMap<String, Double>> callTest(){
		HashMap<String, HashMap<String, Double>> rt = new HashMap<String, HashMap<String, Double>>();
		HypermodulesHeuristicAlgorithm ha = new HypermodulesHeuristicAlgorithm(this.statTest, this.sampleValues, this.clinicalValues, this.otherValues, this.network);
		ha.initialize();
		
		if (this.expandOption.equals("expand")){
			ArrayList<String> seedNames = new ArrayList<String>();
			ArrayList<CyNode> expands = new ArrayList<CyNode>();
			
			
			String seedName = "Default";
			CyNode seedExpand = null;
			
			for (CyNode node : CyTableUtil.getNodesInState(this.network, "selected", true)){
				seedName = this.network.getRow(node).get(CyNetwork.NAME, String.class);
				seedExpand = node;
				seedNames.add(seedName);
				expands.add(seedExpand);
			}
			
			for (int k=0; k<seedNames.size(); k++){
				tm.setTitle("Running Algorithm on Seed: " + seedNames.get(k));
				HashMap<String, Double> oneResult = testSeed(ha, seedNames.get(k), expands.get(k));
				rt.put(seedName, oneResult);
			}
			
		}
		
		else if (this.expandOption.equals("findMost")){
			HashSet<String> allSeeds = new HashSet<String>();
			for (int i=0; i<sampleValues.size(); i++){
				if (!sampleValues.get(i)[1].equals("no_sample") && sampleValues.get(i)[1]!=null){
					allSeeds.add(sampleValues.get(i)[0]);
				}
			}
			
			HashMap<String, CyNode> nameAndNode = new HashMap<String, CyNode>();
			for (CyNode nameNode : this.network.getNodeList()){
				if (allSeeds.contains(this.network.getRow(nameNode).get(CyNetwork.NAME, String.class))){
					nameAndNode.put(this.network.getRow(nameNode).get(CyNetwork.NAME, String.class), nameNode);
				}
			}
			
			System.out.println("allSeeds size: " + nameAndNode.size());
			
			int k=1;
			for (String runSeed : nameAndNode.keySet()){
				tm.setTitle("Running Algorithm on Seed: " + runSeed + " ( " + k + " of " + nameAndNode.size() + " )");
				HashMap<String, Double> oneResult = testSeed(ha, runSeed, nameAndNode.get(runSeed));
				rt.put(runSeed, oneResult);
				k++;
			}
			
			System.out.println("finished running.");
		}
		
		return rt;
	}
	
	public HashMap<String, Double> testSeed (HypermodulesHeuristicAlgorithm ha, String seedName, CyNode seedExpand){
		
		HashMap<String, Double> returnMap = new HashMap<String, Double>();
		
		this.tm.setStatusMessage("finding this_true");
		this.tm.setProgress(0.001);
		FindPaths pathfinder = new FindPaths(this.network, 2);
		
		HashSet<String> allPaths = new HashSet<String>();
		if (this.lengthOption.equals("1")){
			allPaths = pathfinder.getAllPaths1(seedExpand);
		}
		else{
			allPaths = pathfinder.getAllPaths2(seedExpand);
		}
		
		System.out.println("ALL PATHS SIZE: " + allPaths.size());
		
		this.tm.setProgress(0.3);
		
		ArrayList<String> compress = ha.compressTokens(allPaths, seedName);
		
		System.out.println("COMPRESSED SIZE: " + compress.size());
		
		this.tm.setProgress(0.8);
		
		HashMap<String, Double> answer = ha.mineHublets(compress);
		returnMap = answer;
		
		System.out.println("FINAL SIZE: " + returnMap.size());
		this.tm.setProgress(1.0);
		return returnMap;
	}
}
