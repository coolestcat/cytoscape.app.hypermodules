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
import org.cytoscape.hypermodules.internal.statistics.ConnectR;
import org.cytoscape.hypermodules.internal.statistics.FDRAdjust;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * The main task for running the algorithm. Manages all the threads and compiles the final test results set
 * to send to resultsPanel for display and export.
 * @author alvinleung
 *
 */
public class AlgorithmTask implements Task {

	/**
	 * original pValues from OriginalTest
	 */
	private HashMap<String, HashMap<String, Double>> originalResults;
	/**
	 * classification of "HIGH" or "LOW" survival for all the modules in originalResults
	 */
	private HashMap<String, HashMap<String, Double>> classification;
	/**
	 * all the shuffled data to perform FDR adjustment
	 */
	private HashMap<String, Multimap<String, Double>> shuffling;
	/**
	 * an arraylist to add to in order to manage having multiple threads - to avoid synchronization
	 * issues related to adding to a hashmap simultaneously from different threads
	 */
	private ArrayList<HashMap<String, Multimap<String, Double>>> combinedShuffling;
	/**
	 * FDR p-values
	 */
	private HashMap<String, HashMap<String, Double>> adjustedResults;
	
	private HashMap<String, HashMap<String, Double>> adjustedWithR;
	
	
	private CytoscapeUtils utils;
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
	//test comment!
	//upload to github
	//include parameters in export
	//fix generate networks (HashSet<String> to String)
	//copy hotnet cytoscape plugin visual styles for generate networks
	//in Results JTable, only show most correlated if find most (both <0.05) (maybe?)

	/**
	 * constructor
	 * @param currNetwork
	 * @param nShuffled
	 * @param lengthOption
	 * @param expandOption
	 * @param statTest
	 * @param sampleValues
	 * @param clinicalValues
	 * @param otherValues
	 * @param utils
	 */

	public AlgorithmTask(CyNetwork currNetwork, int nShuffled, String expandOption, String statTest, ArrayList<String[]> sampleValues, ArrayList<String[]> clinicalValues, ArrayList<String[]> otherValues, CytoscapeUtils utils){
		this.utils = utils;
		this.expandOption = expandOption;
		this.statTest = statTest;
		this.sampleValues = sampleValues;
		this.otherValues = otherValues;
		this.nShuffled = nShuffled;
		this.network = currNetwork;
		
		ArrayList<String[]> sortedClinicals = new ArrayList<String[]>();
		Multimap<Double, Integer> followupDaysPosition = ArrayListMultimap.create();
		
		for (int k=0; k<clinicalValues.size(); k++){
			boolean b = true;
			try{
				Double d = Double.valueOf(clinicalValues.get(k)[2]);
			}
			catch (NumberFormatException e){
				b = false;
			}
			finally{
				if (b){
					followupDaysPosition.put(Double.valueOf(clinicalValues.get(k)[2]), Integer.valueOf(k));
				}
			}
		}
		
		ArrayList<Double> sortedTime = new ArrayList<Double>();
		ArrayList<Integer> sortedPositions = new ArrayList<Integer>();
		
		for (Double d : followupDaysPosition.keySet()){
			sortedTime.add(d);
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
	
	/**
	 * First, we run the algorithm to get the original test results. Then, we split up the 
	 * shuffling into all possible cores, and run the algorithm that many times after shuffling the gene-sample
	 * associations each time. Then, we calculate the FDR p-values based on the randomized results, and
	 * pass all the information into the results panel.
	 */
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		this.interrupted = false;
		long before = System.nanoTime();
		OriginalTest ot = new OriginalTest(this.expandOption, this.statTest, this.sampleValues, this.clinicalValues, this.otherValues, this.utils, taskMonitor, this.network);
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
		
		
		ShuffleTestTMCall sttm = new ShuffleTestTMCall(nCores,(int) nShuffled/nCores , this.expandOption, this.statTest, this.sampleValues, this.clinicalValues, this.otherValues, taskMonitor, this.network);
		Future<HashMap<String, Multimap<String, Double>>> submit = executor.submit(sttm);
		list.add(submit);
		shuffleCount += (int) nShuffled/nCores;
		
		for (int i=1; i<nCores-1; i++){
			//reinitializeVariables();
			ShuffleTestCall st = new ShuffleTestCall((int) nShuffled/nCores, this.expandOption, this.statTest, this.sampleValues, this.clinicalValues, this.otherValues, this.network);
			Future<HashMap<String, Multimap<String, Double>>> submitPool = executor.submit(st);
			list.add(submitPool);
			shuffleCount += (int) nShuffled/nCores;
		}

		//reinitializeVariables();
		ShuffleTestCall st = new ShuffleTestCall(nShuffled-shuffleCount, this.expandOption, this.statTest, this.sampleValues, this.clinicalValues, this.otherValues, this.network);
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
		//adjustWithR();
		System.out.println("Finished Adjusting");
		
		HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults = resultsFormat();
		HashMap<String, String> parameters = new HashMap<String, String>();
		parameters.put("length", "2");
		parameters.put("expand", this.expandOption);
		parameters.put("nShuffled", String.valueOf(this.nShuffled));
		parameters.put("stat", this.statTest);
		OpenResultsTaskFactory resultsTaskFac = new OpenResultsTaskFactory(parameters, utils, allResults, this.network, this.sampleValues, this.clinicalValues);
		utils.taskMgr.execute(resultsTaskFac.createTaskIterator());
		
		
		long after = System.nanoTime();
		double timeToRun = (double) (after-before)/1000000000;
		System.out.println("Time to run: " + timeToRun + " seconds");
	}
	
	/**
	 * removes duplicates of gene names in each module (this wasn't done earlier in order to improve running time)
	 */
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
				Double d = this.originalResults.get(s).get(t);
				//d = (double)Math.round(d * 10000) / 10000;
				newHash.put(newString, d);
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
	
	/**
	 * formats the shuffle data
	 */
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
	
	/**
	 * performs the FDR adjustment
	 */
	private void adjustResults(){
		adjustedResults = new HashMap<String, HashMap<String, Double>>();
		for (String s : originalResults.keySet()){
			FDRAdjust fdr = new FDRAdjust(originalResults.get(s), shuffling.get(s));
			HashMap<String, Double> adjusted = fdr.fdrAdjust();
			System.out.println(adjusted.size());
			adjustedResults.put(s, adjusted);
			
		}
		/*
		for (String s : adjustedResults.keySet()){
			System.out.println(s);
			for (String d : adjustedResults.get(s).keySet()){
				System.out.println(d + " : " + adjustedResults.get(s).get(d));
			}
		}
		*/
	}
	
	private void adjustWithR() throws REngineException, REXPMismatchException{
		adjustedWithR = new HashMap<String, HashMap<String, Double>>();
		for (String s : originalResults.keySet()){
			ConnectR cr = new ConnectR(originalResults.get(s), shuffling.get(s));
			HashMap<String, Double> adjusted = cr.fdrAdjust();
			adjustedWithR.put(s, adjusted);
		}
	}
	
	/**
	 * formats/concatenates the results into one object to pass to results panel
	 * @return
	 */
	private HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> resultsFormat(){
		HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults = new HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>>();
		for (String s : adjustedResults.keySet()){
			ArrayList<HashMap<String, Double>> ah = new ArrayList<HashMap<String, Double>> ();
			ah.add(originalResults.get(s));
			ah.add(adjustedResults.get(s));
			ah.add(classification.get(s));
			//ah.add(adjustedWithR.get(s));
			HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>> hah = new HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>();
			hah.put(ah,  shuffling.get(s));
			allResults.put(s, hah);
		}

		return allResults;
	}
	
	/**
	 * cancels the task (eg if it is taking too long)
	 */
	@Override
	public void cancel() {
		this.interrupted = true;
	}

}
