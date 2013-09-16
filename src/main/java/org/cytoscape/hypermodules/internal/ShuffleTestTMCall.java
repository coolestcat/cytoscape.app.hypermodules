package org.cytoscape.hypermodules.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.work.TaskMonitor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
/**
 * 
 * The first instance (thread) of the randomization call to run the algorithm - 
 * a task monitor is passed in so that we can set the progress bar in the Cytoscape UI
 * @author alvinleung
 *
 */
public class ShuffleTestTMCall implements Callable<HashMap<String, Multimap<String, Double>>> {

	private String expandOption;
	private String statTest;
	private ArrayList<String[]> sampleValues;
	private ArrayList<String[]> clinicalValues;
	private ArrayList<String[]> otherValues;
	private CyNetwork network;
	private TaskMonitor tm;
	private int nShuffled;
	private int nCores;
	
	public ShuffleTestTMCall(int nCores, int nShuffled, String expandOption, String statTest, ArrayList<String[]> sampleValues, ArrayList<String[]> clinicalValues, ArrayList<String[]> otherValues, TaskMonitor tm, CyNetwork network){
		this.expandOption = expandOption;
		this.statTest = statTest;
		this.sampleValues = sampleValues;
		this.otherValues = otherValues;
		this.clinicalValues = clinicalValues;
		this.tm = tm;
		this.nShuffled = nShuffled;
		this.network = network;
		this.nCores = nCores;
	}
	
	@Override
	public HashMap<String, Multimap<String, Double>> call() throws Exception {
		HashMap<String, Multimap<String, Double>> rt = new HashMap<String, Multimap<String, Double>>();
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
				Multimap<String, Double> oneResult = testSeed(ha, seedNames.get(k), expands.get(k));
				rt.put(seedName, oneResult);
			}
			
			System.out.println("numberTests/4: " + ha.getNumberTests());
			
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
			
			tm.setTitle("Testing on Random Permutations");
			
			int k=1;
			for (String runSeed : nameAndNode.keySet()){
				tm.setTitle("Running Algorithm on Seed: " + runSeed + " ( " + k + " of " + nameAndNode.size() + " )");
				Multimap<String, Double> oneResult = testSeed(ha, runSeed, nameAndNode.get(runSeed));
				rt.put(runSeed, oneResult);
				k++;
			}
			
			System.out.println("numberTests/4: " + ha.getNumberTests());
		}
		
		
		return rt;
	}
	
	public Multimap<String, Double> testSeed (HypermodulesHeuristicAlgorithm ha, String seedName, CyNode seedExpand){

		FindPaths pathfinder = new FindPaths(this.network, 2);
		HashSet<String> allPaths = new HashSet<String>();
		allPaths = pathfinder.getAllPaths2(seedExpand);

		
		
		Multimap<String, Double> returnMap = ArrayListMultimap.create();
		
    	for (int x = 0; x < this.nShuffled; x++){
        	tm.setStatusMessage("Iteration " + (x+1));
        	ha.shuffleLabels();
        	ArrayList<String> compress = ha.compressTokens(allPaths, seedName);
        	HashMap<String, Double> shuffledAnswer = ha.mineHublets(compress);
        	for (String s : shuffledAnswer.keySet()){
        		returnMap.put(s, shuffledAnswer.get(s));
        	}
        	tm.setProgress((x+1)*nCores/ (double) nShuffled*nCores);
    	}

		return returnMap;
	}

}
