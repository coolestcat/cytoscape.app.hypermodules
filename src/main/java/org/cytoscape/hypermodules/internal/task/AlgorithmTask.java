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
import org.cytoscape.hypermodules.internal.statistics.MyFET;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

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
	
	private List<Future<HashMap<String, Multimap<String, Double>>>> list;
	
	
	private CytoscapeUtils utils;
	private String expandOption;
	private String statTest;
	private ArrayList<String[]> sampleValues;
	private ArrayList<String[]> filteredSampleValues;
	private ArrayList<String[]> clinicalValues;
	private Multimap<String, String> sampleValueHash;
	private ArrayList<String[]> otherValues;
	private int nShuffled;
	private CyNetwork network;
	private String foregroundvariable;
	
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

	public AlgorithmTask(CyNetwork currNetwork, int nShuffled, String expandOption, String statTest, String foregroundVariable, ArrayList<String[]> sampleValues, ArrayList<String[]> clinicalValues, ArrayList<String[]> otherValues, CytoscapeUtils utils){
		this.utils = utils;
		this.expandOption = expandOption;
		this.statTest = statTest;
		this.foregroundvariable = foregroundVariable;
		this.sampleValues = sampleValues;
		this.otherValues = otherValues;
		this.nShuffled = nShuffled;
		this.network = currNetwork;
		this.sampleValueHash = ArrayListMultimap.create();
		for (int i=0; i<sampleValues.size(); i++){
			sampleValueHash.put(sampleValues.get(i)[0], sampleValues.get(i)[1]);
		}
		
		if (statTest.equals("logRank")){
		
		
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

			String[] thisString = new String[3];
			thisString[0]=clinicalValues.get(sortedPositions.get(k))[0];
			thisString[1]=clinicalValues.get(sortedPositions.get(k))[1];
			thisString[2]=clinicalValues.get(sortedPositions.get(k))[2];
			

			//thisString[3]=clinicalValues.get(sortedPositions.get(k))[3];
			sortedClinicals.add(thisString);
			
		}
		this.clinicalValues = sortedClinicals;
		}
		
		else{
			this.clinicalValues = clinicalValues;
		}
		
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
		OriginalTest ot = new OriginalTest(this.expandOption, this.statTest, this.foregroundvariable, this.sampleValues, this.clinicalValues, this.otherValues, this.utils, taskMonitor, this.network);
		int nCores = Runtime.getRuntime().availableProcessors();
		this.originalResults = ot.callTest();
		
		fixOriginalResults();
		
		if (statTest.equals("logRank")){
			this.classification = ot.testHighOrLow(this.originalResults);
		}
		
		combinedShuffling = new ArrayList<HashMap<String, Multimap<String, Double>>>();
		//create/initialize algorithm here, pass in to each callable
		int shuffleCount = 0;
		
		if(interrupted){
			System.out.println("Task was cancelled.");
			return;
		}
		
		ExecutorService executor = Executors.newFixedThreadPool(nCores);
		list = new ArrayList<Future<HashMap<String, Multimap<String, Double>>>>();
		
		
		ShuffleTestTMCall sttm = new ShuffleTestTMCall(nCores,(int) nShuffled/nCores , this.expandOption, this.statTest, this.foregroundvariable, this.sampleValues, this.clinicalValues, this.otherValues, taskMonitor, this.network);
		Future<HashMap<String, Multimap<String, Double>>> submit = executor.submit(sttm);
		list.add(submit);
		shuffleCount += (int) nShuffled/nCores;
		
		for (int i=1; i<nCores-1; i++){
			//reinitializeVariables();
			ShuffleTestCall st = new ShuffleTestCall((int) nShuffled/nCores, this.expandOption, this.statTest, this.foregroundvariable, this.sampleValues, this.clinicalValues, this.otherValues, this.network);
			Future<HashMap<String, Multimap<String, Double>>> submitPool = executor.submit(st);
			list.add(submitPool);
			shuffleCount += (int) nShuffled/nCores;
		}

		//reinitializeVariables();
		ShuffleTestCall st = new ShuffleTestCall(nShuffled-shuffleCount, this.expandOption, this.statTest, this.foregroundvariable, this.sampleValues, this.clinicalValues, this.otherValues, this.network);
		Future<HashMap<String, Multimap<String, Double>>> submitPool = executor.submit(st);
		list.add(submitPool);
		
		/*
		if (interrupted){
			for (Future<HashMap<String, Multimap<String, Double>>> ft : list){
				ft.cancel(true);
			}
			System.out.println("Task was cancelled.");
			return;
		}
		*/
		
		for (Future<HashMap<String, Multimap<String, Double>>> future : list){
			try{
				combinedShuffling.add(future.get());
			}
			catch (RuntimeException e){
				//e.printStackTrace();
				System.out.println("task was cancelled.");
				return;
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
		System.out.println("Packaging Data");
		
		HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults = resultsFormat();
		
		HashMap<String, String> parameters = new HashMap<String, String>();
		parameters.put("length", "2");
		parameters.put("expand", this.expandOption);
		parameters.put("nShuffled", String.valueOf(this.nShuffled));
		parameters.put("stat", this.statTest);
		parameters.put("foregroundvariable", this.foregroundvariable);
		OpenResultsTaskFactory resultsTaskFac = new OpenResultsTaskFactory(parameters, utils, allResults, this.network, this.sampleValues, this.clinicalValues, this.otherValues);
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
			//System.out.println(adjusted.size());
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
	
//	private void adjustWithR() throws REngineException, REXPMismatchException{
//		adjustedWithR = new HashMap<String, HashMap<String, Double>>();
//		for (String s : originalResults.keySet()){
//			ConnectR cr = new ConnectR(originalResults.get(s), shuffling.get(s));
//			HashMap<String, Double> adjusted = cr.fdrAdjust();
//			adjustedWithR.put(s, adjusted);
//		}
//	}
	
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
			if (this.statTest.equals("logRank")){
				ah.add(classification.get(s));
			}
			//ah.add(adjustedWithR.get(s));
			
			HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>> hah = new HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>();
			hah.put(ah,  null);// we don't actually need the shuffling multimap - null instead of shuffling.get(s)
			allResults.put(s, hah); 
		}
		
		//filter redundant values in clinicalValues and otherValues?
		
		//filter redundant values in sampleValues
		HashSet<String> sam = new HashSet<String>();
		for (int i=0; i<sampleValues.size(); i++){
			String add = sampleValues.get(i)[0] + ";" + sampleValues.get(i)[1];
			//System.out.println(add);
			sam.add(add);
		}
		
		this.filteredSampleValues = new ArrayList<String[]>();
		for (String s : sam){
			String[] t = s.split(";");
			//System.out.println(t[0] + " : " + t[1]);
			filteredSampleValues.add(t);
		}
		
		for (int i=0; i<filteredSampleValues.size(); i++){
			//System.out.println(filteredSampleValues.get(i)[0] + " : " + filteredSampleValues.get(i)[1]);
		}
		//System.out.println("finished filtering sampleValues");
		allResults = filterRedundantResults(allResults);
		return allResults;
	}
	
	/**
	 * Filters the "redundant" results (same p-value, same patients, or same genes + more genes)
	 * Adds a column for number of patients in the module
	 * Adds a column for the odds ratio
	 * Makes sure the genes string has the seed at the head (seed:b:c:...)
	 * 
	 * ArrayList<HashMap<String, Double>>: 
	 * .get(0) - original
	 * .get(1) - adjusted
	 * .get(2) - classification
	 * .get(3) (or 2) - numberPatients
	 * .get(4) (or 3) - odds ratio
	 * @param input
	 * @return
	 */
	public HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> filterRedundantResults(HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> input){
		HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> output = new HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>>();
		
		HashMap<String, Double> masterList = new HashMap<String, Double>();
		ArrayList<String> everyString = new ArrayList<String>();
		
		ArrayList<String> rejectedList = new ArrayList<String>();
		
		for (String s : input.keySet()){
			for (ArrayList<HashMap<String, Double>> ahsd : input.get(s).keySet()){
				HashMap<String, Double> original = ahsd.get(0);
				for (String i : original.keySet()){
					masterList.put(i, original.get(i));
					everyString.add(i);
					
					//TODO: Added
					
					if (checkNoMutations(s,i)){
						rejectedList.add(i);
					}
					
					//System.out.println(i + " : " + original.get(i));
				}
			}
		}
		
		//System.out.println("everyString size: " + everyString.size());
		//System.out.println("masterList size: " + masterList.size());
		
		for (int i=0; i<everyString.size(); i++){
			double d = masterList.get(everyString.get(i));
			for (int j=i; j<everyString.size(); j++){
				double e = masterList.get(everyString.get(j));
				if ((d == e) && !(i==j)){
					if (checkConditions(everyString.get(i), everyString.get(j))){
						rejectedList.add(everyString.get(j));
					}
				}			
			}
		}
		
	
		System.out.println("Redundant Modules Filtered: "  + rejectedList.size());

		for (String s : input.keySet()){
			//output.put(s, input.get(s));
			
			HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>> hah = new HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>();
			
			HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>> inputhah = input.get(s);
			
			for (ArrayList<HashMap<String, Double>> ahsd : inputhah.keySet()){
				ArrayList<HashMap<String, Double>> newahsd = new ArrayList<HashMap<String, Double>>();
				
				HashMap<String, Double> orig = ahsd.get(0);
				HashMap<String, Double> neworig = new HashMap<String, Double>();
				for (String o : orig.keySet()){
					boolean rejected = false;
					for (int i=0; i<rejectedList.size(); i++){
						if (rejectedList.get(i).equals(o)){
							rejectedList.remove(i);
							rejected = true;
							break;
						}
					}
					if (rejected == false){
						neworig.put(seedAtBeginning(s, o), roundToSignificantFigures(orig.get(o),5));
					}
				}
				
				HashMap<String, Double> adj = ahsd.get(1);
				HashMap<String, Double> newadj = new HashMap<String, Double>();
				for (String o : orig.keySet()){
					boolean rejected = false;
					for (int i=0; i<rejectedList.size(); i++){
						if (rejectedList.get(i).equals(o)){
							rejectedList.remove(i);
							rejected = true;
							break;
						}
					}
					if (rejected == false){
						newadj.put(seedAtBeginning(s, o), roundToSignificantFigures(adj.get(o),5));
					}
				}
				
				HashMap<String, Double> patn = new HashMap<String, Double>();
				for (String x : neworig.keySet()){
					patn.put(x, (double) getNumPatients(x));
				}
				
				HashMap<String, Double> oddsratio = new HashMap<String, Double>();
				if (this.statTest.equals("logRank")){
					for (String x : neworig.keySet()){
						oddsratio.put(x, roundToSignificantFigures(getRatioLogRank(x), 5));
					}
				}
				else{
					for (String x : neworig.keySet()){
						oddsratio.put(x, roundToSignificantFigures(getRatioFisher(x), 5));
					}
				}

					
				newahsd.add(neworig);
				newahsd.add(newadj);
				newahsd.add(patn);
				newahsd.add(oddsratio);
				
				hah.put(newahsd, inputhah.get(ahsd));
			}
			output.put(s, hah);
			
		}
		
		return output;
	}
	
	
	public static double roundToSignificantFigures(double num, int n) {
		if (Double.isNaN(num)){
			return Double.NaN;
		}
		if (Double.isInfinite(num)){
			return num;
		}
		
	    if(num == 0) {
	        return 0;
	    }

	    final double d = Math.ceil(Math.log10(num < 0 ? -num: num));
	    final int power = n - (int) d;

	    final double magnitude = Math.pow(10, power);
	    final long shifted = Math.round(num*magnitude);
	    return shifted/magnitude;
	}
	
	
	public String seedAtBeginning(String s, String t){
		if (t.equals("none")){
			return "none";
		}
		String[] splitted = t.split(":");
		HashSet<String> hs = new HashSet<String>();
		for (int i=0; i<splitted.length; i++){
			hs.add(splitted[i]);
		}
		
		hs.remove(s);
		String returnVal = s;
		
		for (String x : hs){
			returnVal = returnVal + ":" + x;
		}
		
		return returnVal;
	}
	
	
	public boolean checkNoMutations(String seed, String s){
		String[] genes = s.split(":");
		
		//TODO: Added
		boolean ret = true;
		String t = "default";
		for (int i=0; i<genes.length; i++){
			if (!genes[i].equals(seed)){
				if (!(sampleValueHash.get(genes[i])==null)){
					if (!(sampleValueHash.get(genes[i]).size() == 0)){
						ret = false;
						t = genes[i];
					}
				}
			}
		}

		return ret;
		
		
	}
	
	public boolean checkConditions(String s, String t){
		String[] genes1 = s.split(":");
		String[] genes2 = t.split(":");
		
		HashSet<String> g1 = new HashSet<String>();
		HashSet<String> g2 = new HashSet<String>();
		
		//System.out.println("s:" + s);
		//System.out.println("t:" + t);
		
		
		for (int i=0; i<genes1.length; i++){
			g1.add(genes1[i]);
		}
		
		for (int i=0; i<genes2.length; i++){
			g2.add(genes2[i]);
			
		}
		
		boolean sSubsetOfT = true;
		for (String x : g1){
			if (!g2.contains(x)){
				sSubsetOfT = false;
				//System.out.println(x + " is not in g2 but is in g1");
			}
		}
		
		if (sSubsetOfT == true){
			return true;
		}
		
		/*
		HashSet<String> p1 = new HashSet<String>();
		HashSet<String> p2 = new HashSet<String>();
		for (int i=0; i<filteredSampleValues.size(); i++){
			if (g1.contains(filteredSampleValues.get(i)[0])){
				p1.add(filteredSampleValues.get(i)[1]);
			}
			if (g2.contains(filteredSampleValues.get(i)[0])){
				p2.add(filteredSampleValues.get(i)[1]);
			}
		}
		
		if (p1.equals(p2)){
			if (genes2.length >= genes1.length){
				return true;
			}
		}
		*/
		
		return false;
	}
	
	public String getPatientList(String genes){
		String ret = "";
		String[] t = genes.split(":");
		HashSet<String> pats = new HashSet<String>();
		for (int i=0; i<t.length; i++){
			for (String s : sampleValueHash.get(t[i])){
				pats.add(s);
			}	
		}
		
		for (String s : pats){
			ret = ret + s + ",";
		}
		
		if (ret.charAt(ret.length()-1)==','){
			ret = ret.substring(0, ret.length()-1);
		}
			return ret;
	}
	
	public int getNumPatients(String genes){
		int result = 0;
		String[] splitted = genes.split(":");
		HashSet<String> checker = new HashSet<String>();
		for (int i=0; i<splitted.length; i++){
			checker.add(splitted[i]);
		}
		
		HashSet<String> patients = new HashSet<String>();
		for (int i=0; i<filteredSampleValues.size(); i++){
			if (checker.contains(filteredSampleValues.get(i)[0])){
				if (!filteredSampleValues.get(i)[1].equals("no_sample")){
					patients.add(filteredSampleValues.get(i)[1]);
				}

			}
		}
		
		result = patients.size();
		return result;
	}
	
	//TODO:
	public double getRatioLogRank(String genes){
		String[] g = genes.split(":");

		
		HashSet<String> gs = new HashSet<String>();
		for (int i=0; i<g.length; i++){
			gs.add(g[i]);
		}
		
		
		HashSet<String> inModulePatients = new HashSet<String>();
		
		for (int i=0; i<filteredSampleValues.size(); i++){
			if (gs.contains(filteredSampleValues.get(i)[0])){
				if (!filteredSampleValues.get(i)[1].equals("no_sample")){
					inModulePatients.add(filteredSampleValues.get(i)[1]);
				}
			}
		}
		
		
		ArrayList<Double> inModuleFollowup = new ArrayList<Double>();
		ArrayList<Double> outOfModuleFollowup = new ArrayList<Double>();
		
		for (int i=0; i<clinicalValues.size(); i++){
			if (inModulePatients.contains(clinicalValues.get(i)[0])){
				inModuleFollowup.add(Double.valueOf(clinicalValues.get(i)[2]));
			}
			else{
				outOfModuleFollowup.add(Double.valueOf(clinicalValues.get(i)[2]));
			}
		}
		
		
		if (inModuleFollowup.size()>0){
			
			double p1 = 0;
			if (inModuleFollowup.size() %2 == 0){
				p1 = ((double)inModuleFollowup.get(inModuleFollowup.size()/2) + (double) inModuleFollowup.get((inModuleFollowup.size()/2)-1))/2;
			}
			else{
				p1 = inModuleFollowup.get(inModuleFollowup.size()/2);
			}

			double p2 = 0;
			if (outOfModuleFollowup.size() %2 ==0){
				p2 = ((double)outOfModuleFollowup.get(outOfModuleFollowup.size()/2) + (double) outOfModuleFollowup.get((outOfModuleFollowup.size()/2)-1))/2;
			}
			else{
				p2 = outOfModuleFollowup.get(outOfModuleFollowup.size()/2);
			}
			
			return Math.log(p1/(double)p2);
		}
		else{
			return Double.NaN;
		}
		
		
	}
	
	//TODO:
	
	//variable 1 is the first one to appear in otherValues
	public double getRatioFisher(String thisNetwork){
		String[] genes = thisNetwork.split(":");
		
		String v1 = foregroundvariable;
		
		HashSet<String> gs = new HashSet<String>();
		for (int i=0; i<genes.length; i++){
			gs.add(genes[i]);
		}
		
		
		
		ArrayList<String> patients = new ArrayList<String>();
		
		for (int i=0; i<filteredSampleValues.size(); i++){
			if (gs.contains(filteredSampleValues.get(i)[0])){
				if (!filteredSampleValues.get(i)[1].equals("no_sample")){
					patients.add(filteredSampleValues.get(i)[1]);
				}
			}
		}
		
		boolean[] var2patients = new boolean[this.otherValues.size()];
		for (int k=0; k<this.otherValues.size(); k++){
			var2patients[k]=false;
			
			for (int l=0; l<patients.size(); l++){		
				if(patients.get(l).equals(otherValues.get(k)[0])){
					var2patients[k]=true;
				}
			}
		}
		
		int alpha=0;
		for (int k=0; k<var2patients.length; k++){
			if (var2patients[k]==true){
				alpha++;
			}
		}
		
		
		int c = 0;
		int matrix00 = 0;
		for (int k=0; k<this.otherValues.size(); k++){
			if (var2patients[k] == true){
				if (this.otherValues.get(k)[1].equals(this.foregroundvariable)){
					matrix00++;
				}
			}
			if (this.otherValues.get(k)[1].equals(this.foregroundvariable)){
				c++;
			}
		}
		
		
		MyFET fet = new MyFET(otherValues.size(), c, alpha, matrix00); 
		Double rvalue =  fet.getLogOdds();
		
		/*
		if (gs.contains("XAB2")){
			System.out.println(thisNetwork);
			System.out.println("populationSize: " + otherValues.size());
			System.out.println("totalSuccesses: " + c);
			System.out.println("sampleSize: " + alpha);
			System.out.println("sampleSucceses: " + matrix00);
			System.out.println("p-value: " + fet.getResult());
			System.out.println("lodds: " + rvalue);
			
            int[][] data = new int[2][2];
            data[0][0] = matrix00;
            data[0][1] = alpha - matrix00;
            data[1][0] = c-matrix00;
            data[1][1] = otherValues.size() - data[0][0] - data[0][1] - data[1][0];
            
            System.out.println("Contingency table: " + data[0][0] + " - " + data[0][1] + " - " + data[1][0] + " - " + data[1][1]);   
            System.out.println();
            System.out.println();
		}
		*/

	
		/*
		String[] g = genes.split(":");
		
		String v1 = foregroundvariable;
		
		HashSet<String> gs = new HashSet<String>();
		for (int i=0; i<g.length; i++){
			gs.add(g[i]);
		}
		
		
		HashSet<String> inModulePatients = new HashSet<String>();
		HashSet<String> outOfModulePatients = new HashSet<String>();
		
		for (int i=0; i<filteredSampleValues.size(); i++){
			if (gs.contains(filteredSampleValues.get(i)[0])){
				if (!filteredSampleValues.get(i)[1].equals("no_sample")){
					inModulePatients.add(filteredSampleValues.get(i)[1]);
				}
			}
			else{
				if (!filteredSampleValues.get(i)[1].equals("no_sample")){
					outOfModulePatients.add(filteredSampleValues.get(i)[1]);
				}
			}
		}
		

		int inModuleVar1 = 0;
		int outOfModuleVar1 = 0;
		
		for (int i=0; i<otherValues.size(); i++){
			if (inModulePatients.contains(otherValues.get(i)[0])){
				if (otherValues.get(i)[1].equals(v1)){
					inModuleVar1++;
				}
			}
			else{
				if (otherValues.get(i)[1].equals(v1)){
					outOfModuleVar1++;
				}
			}
		}
		
		
		double p1 = inModuleVar1/ (double) inModulePatients.size();
		double p2 = outOfModuleVar1/ (double) outOfModulePatients.size();
		double rvalue = p1*(1-p2)/(double) (p2*(1-p1));
		*/
		
		return rvalue;
	}
	
	
	/**
	 * cancels the task (eg if it is taking too long)
	 */
	@Override
	public void cancel() {
		this.interrupted = true;
		for (int i=0; i<list.size(); i++){
			list.get(i).cancel(true);
		}
		Thread.currentThread().interrupt();
		return;
	}

}
