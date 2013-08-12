package org.cytoscape.hypermodules.internal.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.hypermodules.internal.OriginalTest;
import org.cytoscape.hypermodules.internal.ShuffleTestCall;
import org.cytoscape.hypermodules.internal.ShuffleTestTMCall;
import org.cytoscape.hypermodules.internal.statistics.FDRAdjust;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class AlgorithmTask implements Task {

	private HashMap<String, HashMap<String, Double>> originalResults;
	private HashMap<String, HashMap<String, Double>> classification;
	private HashMap<String, Multimap<String, Double>> shuffling;
	private ArrayList<HashMap<String, Multimap<String, Double>>> combinedShuffling;
	private HashMap<String, HashMap<String, Double>> adjustedResults;
	
	private CytoscapeUtils utils;
	private String lengthOption;
	private String expandOption;
	private String statTest;
	private ArrayList<String[]> sampleValues;
	private ArrayList<String[]> clinicalValues;
	private ArrayList<String[]> otherValues;
	private int nShuffled;
	private CyNetwork network;
	
	private boolean interrupted;
	
	
	//Done:
	//only sort once, not at every Log Rank Test, see if it makes it faster
	//1. fix mine hublets (figure out last step)
	//add rserve, try testing with coxph
	//get rserve to work
	//expand from selected SEEDS
	//fdr with r
	//3. try run 10000, see if it matches, if not fix
	//2. padjust (figure out fdr)
	//fix NaN with no shuffles
	//get export button to work
	//fix results panel constructors
	//weekend: run all seeds
	//repositories
	//fix BFS (only one method)
	//am I doing the shuffling right?
	//run code profiling
	//fisher test
	//input options: enter any number of tables with clinical variables (tumor_stage, primary_outcome, etc), 
	//find fisher test differences
	//coxph
	//make results panel/display
	//not store seed?
	//allow for searching only paths of length 1
	//filter for most correlated of all
	//parse/run new dataset
	//make sure that DFR contains all the patients referenced in G2S (add check at beginning)
	//test sorting (sorting should work now)
	//CUT DOWN NUMBER OF TESTS USING HEURISTIC
	//fix results panel layout
	//hookup networkSelectionPanel logic
	//FIX MAIN PANEL LAYOUT/collapsible/on load, change label background (green?)
	//change log rank test to output corresponding high survival or low survival
	


	public AlgorithmTask(CyNetwork currNetwork, int nShuffled, String lengthOption, String expandOption, String statTest, ArrayList<String[]> sampleValues, ArrayList<String[]> clinicalValues, ArrayList<String[]> otherValues, CytoscapeUtils utils){
		this.utils = utils;
		this.lengthOption = lengthOption;
		this.expandOption = expandOption;
		this.statTest = statTest;
		this.sampleValues = sampleValues;
		this.otherValues = otherValues;
		this.nShuffled = nShuffled;
		this.network = currNetwork;
		
		ArrayList<String[]> sortedClinicals = new ArrayList<String[]>();
		Multimap<Double, Integer> followupDaysPosition = ArrayListMultimap.create();
		
		for (int k=0; k<clinicalValues.size(); k++){
			followupDaysPosition.put(Double.valueOf(clinicalValues.get(k)[2]), Integer.valueOf(k));
		}
		
		ArrayList<Double> sortedTime = new ArrayList<Double>();
		ArrayList<Integer> sortedPositions = new ArrayList<Integer>();
		
		for (int k=0; k<clinicalValues.size(); k++){
			sortedTime.add(Double.valueOf(clinicalValues.get(k)[2]));
		}
		
		Collections.sort(sortedTime);
		HashMap<Double, Boolean> alreadyAdded = new HashMap<Double, Boolean>();
		
		for (Double key : followupDaysPosition.keySet()){
			alreadyAdded.put(key,  false);
		}

		for (int k=0; k<sortedTime.size(); k++){
			if (alreadyAdded.get(sortedTime.get(k))==false){
				Collection<Integer> coll = followupDaysPosition.get(sortedTime.get(k));
				for (Integer value : coll){
					sortedPositions.add(value);
				}
			}
			alreadyAdded.put(sortedTime.get(k), true);
		}
		
		for (int k=0; k<sortedPositions.size(); k++){
			String[] thisString = new String[4];
			thisString[0]=clinicalValues.get(sortedPositions.get(k))[0];
			thisString[1]=clinicalValues.get(sortedPositions.get(k))[1];
			thisString[2]=clinicalValues.get(sortedPositions.get(k))[2];
			thisString[3]=clinicalValues.get(sortedPositions.get(k))[3];
			sortedClinicals.add(thisString);
			
		}
		this.clinicalValues = sortedClinicals;
	}
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		this.interrupted = false;
		long before = System.nanoTime();
		OriginalTest ot = new OriginalTest(this.lengthOption, this.expandOption, this.statTest, this.sampleValues, this.clinicalValues, this.otherValues, this.utils, taskMonitor, this.network);
		int nCores = Runtime.getRuntime().availableProcessors();
		this.originalResults = ot.callTest();
		
		fixOriginalResults();
		
		this.classification = ot.testHighOrLow(this.originalResults);
		
		combinedShuffling = new ArrayList<HashMap<String, Multimap<String, Double>>>();
		//create/initialize algorithm here, pass in to each callable
		int shuffleCount = 0;
		
		if(interrupted){
			System.out.println("Task was cancelled.");
			return;
		}
		
		ExecutorService executor = Executors.newFixedThreadPool(nCores);
		List<Future<HashMap<String, Multimap<String, Double>>>> list = new ArrayList<Future<HashMap<String, Multimap<String, Double>>>>();
		
		
		ShuffleTestTMCall sttm = new ShuffleTestTMCall(this.lengthOption, nCores,(int) nShuffled/nCores , this.expandOption, this.statTest, this.sampleValues, this.clinicalValues, this.otherValues, taskMonitor, this.network);
		Future<HashMap<String, Multimap<String, Double>>> submit = executor.submit(sttm);
		list.add(submit);
		shuffleCount += (int) nShuffled/nCores;
		
		for (int i=1; i<nCores-1; i++){
			//reinitializeVariables();
			ShuffleTestCall st = new ShuffleTestCall(this.lengthOption, (int) nShuffled/nCores, this.expandOption, this.statTest, this.sampleValues, this.clinicalValues, this.otherValues, this.network);
			Future<HashMap<String, Multimap<String, Double>>> submitPool = executor.submit(st);
			list.add(submitPool);
			shuffleCount += (int) nShuffled/nCores;
		}

		//reinitializeVariables();
		ShuffleTestCall st = new ShuffleTestCall(this.lengthOption, nShuffled-shuffleCount, this.expandOption, this.statTest, this.sampleValues, this.clinicalValues, this.otherValues, this.network);
		Future<HashMap<String, Multimap<String, Double>>> submitPool = executor.submit(st);
		list.add(submitPool);

		if (interrupted){
			for (Future<HashMap<String, Multimap<String, Double>>> ft : list){
				ft.cancel(true);
			}
			System.out.println("Task was cancelled.");
			return;
		}
		
		for (Future<HashMap<String, Multimap<String, Double>>> future : list){
			try{
				if (interrupted){
					for (Future<HashMap<String, Multimap<String, Double>>> ft : list){
						ft.cancel(true);
					}
					System.out.println("Task was cancelled.");
					return;
				}
				combinedShuffling.add(future.get());
			}
			catch (Exception e){
				e.printStackTrace();
			}
		}
		
		executor.shutdown();
		
		taskMonitor.setTitle("Adjusting Results");
		moveShuffled();
		System.out.println("Finished Moving");

		//printShuffling();
		System.out.println("Shuffled size: " + getShuffleSize());
		adjustResults();
		System.out.println("Finished Adjusting");
		
		HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults = resultsFormat();
		OpenResultsTaskFactory resultsTaskFac = new OpenResultsTaskFactory(utils, allResults, this.network);
		utils.taskMgr.execute(resultsTaskFac.createTaskIterator());
		
		
		long after = System.nanoTime();
		double timeToRun = (double) (after-before)/1000000000;
		System.out.println("Time to run: " + timeToRun + " seconds");
	}
	
	private void fixOriginalResults(){
		HashMap<String, HashMap<String, Double>> newOriginal = new HashMap<String, HashMap<String, Double>>();
		for (String s : this.originalResults.keySet()){
			HashMap<String, Double> newHash = new HashMap<String, Double>();
			for (String t : this.originalResults.get(s).keySet()){
				
				String[] st = t.split(":");
				HashSet<String> hs = new HashSet<String>();
				for (int i=0; i<st.length; i++){
					hs.add(st[i]);
				}
				String newString = "";
				for (String u : hs){
					newString = newString + u + ":";
				}
				newString = newString.substring(0, newString.length()-1);
				newHash.put(newString, this.originalResults.get(s).get(t));
			}
			newOriginal.put(s,  newHash);
		}
		
		this.originalResults = newOriginal;
		
	}
	
	private int getShuffleSize(){
		int c = 0;
		for (String s : shuffling.keySet()){
			c += shuffling.get(s).size();
		}
		
		return c;
		
	}
	
	private void moveShuffled(){
		shuffling = new HashMap<String, Multimap<String, Double>>();
		HashMap<String, Multimap<String, Double>> lastCore = combinedShuffling.get(combinedShuffling.size()-1);
		for (String str : lastCore.keySet()){
			Multimap<String, Double> mhsd = ArrayListMultimap.create();
			for (String hs : lastCore.get(str).keySet()){
				for (Double d : lastCore.get(str).get(hs)){
					mhsd.put(hs, d);
				}
			}
			shuffling.put(str, mhsd);
		}
		
		for (int i=1; i<combinedShuffling.size(); i++){
			HashMap<String, Multimap<String, Double>> thisCore = combinedShuffling.get(i);
			for (String s : thisCore.keySet()){
				for (String hs : thisCore.get(s).keySet()){
					for (Double d : thisCore.get(s).get(hs)){
						shuffling.get(s).put(hs, d);
					}
				}
			}
		}
	}
	
	private void adjustResults(){
		adjustedResults = new HashMap<String, HashMap<String, Double>>();
		for (String s : originalResults.keySet()){
			FDRAdjust fdr = new FDRAdjust(originalResults.get(s), shuffling.get(s));
			HashMap<String, Double> adjusted = fdr.fdrAdjust();
			adjustedResults.put(s, adjusted);
		}
	}
	
	private HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> resultsFormat(){
		HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults = new HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>>();
		for (String s : adjustedResults.keySet()){
			ArrayList<HashMap<String, Double>> ah = new ArrayList<HashMap<String, Double>> ();
			ah.add(originalResults.get(s));
			ah.add(adjustedResults.get(s));
			ah.add(classification.get(s));
			HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>> hah = new HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>();
			hah.put(ah,  shuffling.get(s));
			allResults.put(s, hah);
		}

		return allResults;
	}
	

	@Override
	public void cancel() {
		this.interrupted = true;
	}

}
