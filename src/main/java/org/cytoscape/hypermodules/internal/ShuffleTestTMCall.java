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

	private String lengthOption;
	private String expandOption;
	private String statTest;
	private ArrayList<String[]> sampleValues;
	private ArrayList<String[]> clinicalValues;
	private ArrayList<String[]> otherValues;
	private CyNetwork network;
	private TaskMonitor tm;
	private int nShuffled;
	private int nCores;
	private HashMap<String, Multimap<String, Double>> rt;
	
	public ShuffleTestTMCall(String lengthOption, int nCores, int nShuffled, String expandOption, String statTest, ArrayList<String[]> sampleValues, ArrayList<String[]> clinicalValues, ArrayList<String[]> otherValues, TaskMonitor tm, CyNetwork network){
		this.lengthOption = lengthOption;
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
		rt = new HashMap<String, Multimap<String, Double>>();
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
			
			for (int i=0; i<nShuffled; i++){
	        	ha.shuffleLabels();
				tm.setTitle("Iteration: " + i+1);
				for (int k=0; k<seedNames.size(); k++){
					tm.setStatusMessage("Running Algorithm on Seed: " + seedNames.get(k) + " ( " + k + " of " + seedNames.size() + " )");
					HashMap<String, Double> oneResult = testSeed(ha, seedNames.get(k), expands.get(k));
					addResult(seedNames.get(k), oneResult);
				}
				tm.setProgress((i+1)/(double) nShuffled);
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
			for (int i=0; i<nShuffled; i++){
				ha.shuffleLabels();
				tm.setTitle("Iteration: " + (i+1));
				for (String runSeed : nameAndNode.keySet()){
					tm.setStatusMessage("Running Algorithm on Seed: " + runSeed + " ( " + k + " of " + nameAndNode.size() + " )");
					HashMap<String, Double> oneResult = testSeed(ha, runSeed, nameAndNode.get(runSeed));
					addResult(runSeed, oneResult);
					k++;
				}
				k = 1;
				tm.setProgress((i+1)/(double) nShuffled);
			}

			
			System.out.println("numberTests/nCores: " + ha.getNumberTests());
		}
		
		
		return rt;
	}
	
	public void addResult(String seed, HashMap<String, Double> result){
		if (rt.get(seed)==null){
			Multimap<String, Double> m = ArrayListMultimap.create();
			for (String s : result.keySet()){
				m.put(s, result.get(s));
			}
			rt.put(seed, m);
		}
		else{
			for (String s : result.keySet()){
				rt.get(seed).put(s,  result.get(s));
			}
		}
	}
	
	public HashMap<String, Double> testSeed (HypermodulesHeuristicAlgorithm ha, String seedName, CyNode seedExpand){

		FindPaths pathfinder = new FindPaths(this.network, 2);
		HashSet<String> allPaths = new HashSet<String>();
		if (this.lengthOption.equals("1")){
			allPaths = pathfinder.getAllPaths1(seedExpand);
		}
		else{
			allPaths = pathfinder.getAllPaths2(seedExpand);
		}
		
		
        ArrayList<String> compress = ha.compressTokens(allPaths, seedName);
        HashMap<String, Double> shuffledAnswer = ha.mineHublets(compress);


		return shuffledAnswer;
	}

}
