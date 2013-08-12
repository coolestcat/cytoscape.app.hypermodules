package org.cytoscape.hypermodules.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import javastat.survival.regression.CoxRegression;

import org.cytoscape.hypermodules.internal.statistics.CoxPh;
import org.cytoscape.hypermodules.internal.statistics.FishersExact;
import org.cytoscape.hypermodules.internal.statistics.LogRankTest;
import org.cytoscape.hypermodules.internal.task.AlgorithmTask;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.TaskMonitor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class HypermodulesHeuristicAlgorithm {
	
	private String statTest;
	private ArrayList<String[]> sampleValues;
	private ArrayList<String[]> clinicalValues;
	private ArrayList<String[]> otherValues;
	private HashMap<String, String> allGeneSamplesMap;
	private CyNetwork network;
	private String[] allPatients;
	private boolean[] status;
	private double[] followupDays;
	private double[] daysFromBirth;
	private double[] age;
	private double[] censor;
	private String[] otherPatients;
	private String[] clinicalVariable;
	private HashSet<String> clinicalVariableHash;
	private HashMap<String, String> clinicalVariableMap;
	private ArrayList<String> hashArray;
	private LogRankTest logRankObject;
	private CoxPh coxModel;
	
	private ArrayList<String> allGenes;
	private ArrayList<String> allSamples;
	
	//<gene, level of survival (long, medium, short)>
	private HashMap<String, String> survivalClassification;
	private HashMap<String, Integer> patientDaysFollowup;
	private HashMap<String, Double> geneAverageSurvival;
	
	private double highCutoff;
	private double lowCutoff;
	
	private int numberTests;
	
	public HypermodulesHeuristicAlgorithm(String statTest, ArrayList<String[]> sampleValues, ArrayList<String[]> clinicalValues, ArrayList<String[]> otherValues, CyNetwork network){
		this.statTest = statTest;
		this.sampleValues = sampleValues;
		this.otherValues = otherValues;
		this.clinicalValues = clinicalValues;
		this.network = network;
	}

	public void initialize(){
		this.numberTests = 0;
		if (!otherValues.isEmpty()){
			initOther();
		}
		initClinicals();
		logRankObject = new LogRankTest(this.followupDays);
		coxModel = new CoxPh(this.followupDays.length, this.followupDays, this.censor, this.age);
		coxModel.coxInit();

		allGeneSamplesMap = new HashMap<String, String>();
		
		for (int i=0; i<sampleValues.size(); i++){
			allGeneSamplesMap.put(sampleValues.get(i)[0], sampleValues.get(i)[1]);
		}
		
		for (CyNode cynode : this.network.getNodeList()){
			if (allGeneSamplesMap.get(this.network.getRow(cynode).get(CyNetwork.NAME, String.class))==null){
				String[] inconsistency = new String[2];
				inconsistency[0] = this.network.getRow(cynode).get(CyNetwork.NAME, String.class);
				inconsistency[1] = "no_sample";
				sampleValues.add(inconsistency);
				allGeneSamplesMap.put(sampleValues.get(sampleValues.size()-1)[0], sampleValues.get(sampleValues.size()-1)[1]);
			}
		}
		
		Multimap<String, String> memoryone = ArrayListMultimap.create();
		HashSet<String> g2sSamples = new HashSet<String>();
		for (String[] s : sampleValues){
			String[] sampleSplit = s[1].split(":");
			for (String t : sampleSplit){
				if (!t.equals("no_sample")){
					g2sSamples.add(t);
					memoryone.put(t, s[0]);
				}
			}
		}
		
		HashSet<String> clinicalSamples = new HashSet<String>();
		for (String[] s : clinicalValues){
			clinicalSamples.add(s[0]);
		}
		
		for (String s : g2sSamples){
			if (!clinicalSamples.contains(s)){
				for (String z : memoryone.get(s)){
					allGeneSamplesMap.put(z, "no_sample");
					System.out.println("The sample " + s + " was not found in your clinical table. All genes corresponding to " + s + " are now assumed to have no sample.");
				}
			}
		}
		
		
		//do it again in case sample values (those in the cynetwork) doesnt match the input list of genes
		/*
		for (int i=0; i<sampleValues.size(); i++){
			allGeneSamplesMap.put(sampleValues.get(i)[0], sampleValues.get(i)[1]);
		}
		*/
		

		allGenes = new ArrayList<String>();
		allSamples = new ArrayList<String>();
		
		for (String key : allGeneSamplesMap.keySet()){
			allGenes.add(key);
			allSamples.add(allGeneSamplesMap.get(key));
		}
		
		initializeSurvivalClassification();
	}
	
	public void initializeSurvivalClassification(){
		this.patientDaysFollowup = new HashMap<String, Integer>();
		for (int i=0; i<this.clinicalValues.size(); i++){
			patientDaysFollowup.put(clinicalValues.get(i)[0],  Integer.valueOf(clinicalValues.get(i)[2]));
		}
		
		int survivalSum = 0;
		for (int i=0; i<this.clinicalValues.size(); i++){
			survivalSum += Integer.valueOf(clinicalValues.get(i)[2]);
		}
		
		double mean = survivalSum /(double) clinicalValues.size();
		
		double standardDeviation = 0;
		int[] distanceFromMean = new int[clinicalValues.size()];
		for (int i=0; i<clinicalValues.size(); i++){
			standardDeviation += Math.pow(Integer.valueOf(clinicalValues.get(i)[2]) - mean, 2);
		}
		
		standardDeviation = Math.pow(standardDeviation/(double) clinicalValues.size(), 0.5);
		
		//standardDeviation = standardDeviation/(double) 2;
		
		highCutoff = mean + standardDeviation;
		lowCutoff = mean - standardDeviation;
		
		System.out.println("mean: " + mean);
		System.out.println("standard deviation: " + standardDeviation);
		

		
		ArrayList<String> allSeeds = new ArrayList<String>();
		for (int i=0; i<allGenes.size(); i++){
			if (!allGeneSamplesMap.get(allGenes.get(i)).equals("no_sample")){
				allSeeds.add(allGenes.get(i));
			}
		}
		
		this.survivalClassification = new HashMap<String, String>();
		this.geneAverageSurvival = new HashMap<String, Double>();
		for (int i=0; i<allSeeds.size(); i++){
			String[] p = allGeneSamplesMap.get(allSeeds.get(i)).split(":");
			double survivalAverage = 0;
			for (int k=0; k<p.length; k++){
				survivalAverage += patientDaysFollowup.get(p[k]);
			}
			survivalAverage = survivalAverage/(double) p.length;
			geneAverageSurvival.put(allSeeds.get(i), survivalAverage);
			if (survivalAverage > highCutoff){
				survivalClassification.put(allSeeds.get(i), "LONG");
			}
			else if (survivalAverage > lowCutoff){
				survivalClassification.put(allSeeds.get(i), "MEDIUM");
			}
			else{
				survivalClassification.put(allSeeds.get(i), "SHORT");
			}
		}
		
		
	}
	
	public boolean testClassification(String one, String two){
		String[] a = one.split(":");
		String[] b = two.split(":");
		
		double oneAverage = 0;
		int k = 0;
		for (int i=0; i<a.length; i++){
			if (geneAverageSurvival.get(a[i])!=null){
				oneAverage += geneAverageSurvival.get(a[i]);
				k++;
			}
		}
		oneAverage = oneAverage/(double) k;
		
		double twoAverage = 0;
		k = 0;
		for (int i=0; i<b.length; i++){
			if (geneAverageSurvival.get(b[i])!=null){
				twoAverage += geneAverageSurvival.get(b[i]);
				k++;
			}
		}
		
		twoAverage = twoAverage/(double) k;
		
		if (oneAverage > highCutoff && twoAverage < lowCutoff || oneAverage < lowCutoff && twoAverage > highCutoff){
			return false;
		}
		
		else{
			return true;
		}
	}
	
	public boolean testClassification2(String one, String two){
		String[] a = one.split(":");
		String[] b = two.split(":");
		
		double oneAverage = 0;
		int k = 0;
		for (int i=0; i<a.length; i++){
			if (survivalClassification.get(a[i])!=null){
				k++;
				if (survivalClassification.get(a[i]).equals("LONG")){
					oneAverage += 2;
				}
				else if (survivalClassification.get(a[i]).equals("MEDIUM")){
					oneAverage += 1;
				}
				else{
					oneAverage += 0;
				}
			}
		}
		
		oneAverage = oneAverage/(double) k;
		
		double twoAverage = 0;
		int j = 0;
		for (int i=0; i<b.length; i++){
			if (survivalClassification.get(b[i])!=null){
				if (survivalClassification.get(b[i])!=null){
					j++;
					if (survivalClassification.get(b[i]).equals("LONG")){
						twoAverage += 2;
					}
					else if (survivalClassification.get(b[i]).equals("MEDIUM")){
						twoAverage += 1;
					}
					else{
						twoAverage += 0;
					}
				}
			
			}
		}
		
		twoAverage = twoAverage/(double) j;
		int max = 2*(j+k);
		double first = max/(double) 3;
		double second = first * 2;
		
		if (oneAverage < first && twoAverage > second || oneAverage > second && twoAverage < first){
			return false;
		}

		return true;
	}
	
	public HashMap<String, Double> mineHublets(ArrayList<String> compressedList){
		
		HashMap<String, Double> hubletsTested = new HashMap<String, Double>();
		HashMap<String, Double> repository = new HashMap<String, Double>();
		
		String key5;
		Double value5;
    	for(int i=0; i<compressedList.size(); i++){
    		key5 = compressedList.get(i);
    		value5 = testModuleClinical(compressedList.get(i), 1, false);
    		if (value5!=null){
        		hubletsTested.put(key5, value5);
    		}
    	}

    	if (hubletsTested.isEmpty()){
    		return hubletsTested;
    	}
    	
    	HashMap<String[], Double[]> pairwise = new HashMap<String[], Double[]>();
    	HashMap<String, Double> pairwiseConcat = new HashMap<String, Double>();
		HashMap<String, String[]> pairwiseConcatMemory = new HashMap<String, String[]>();
    	
    	while(true){
        	

			String[] pairwiseKey = new String[2];
    		Double[] pairwiseValue = new Double[2];
    		
    		
    		ArrayList<String> list = new ArrayList<String>();
 
    		for (String cy1 : hubletsTested.keySet()){
    			list.add(cy1);
    		}
    		
    		
    		for (int k=0; k<list.size(); k++){
    			for (int j = k+1; j<list.size(); j++){
    				pairwiseKey = new String[2];
    				pairwiseKey[0]=list.get(k);
    				pairwiseKey[1]=list.get(j);

    				pairwiseValue = new Double[2];
    				pairwiseValue[0]=hubletsTested.get(list.get(k));
    				pairwiseValue[1]=hubletsTested.get(list.get(j));
    				
    				pairwise.put(pairwiseKey, pairwiseValue);
    			}
    		}

    		String key7;
    		Double value7;
    		//System.out.println("concatenatedNetwork :");
    		
    		String minKey = null;
    		Double minVal=Double.valueOf(2);
    		
    		for (String[] key6 : pairwise.keySet()){
    			//TODO: got rid of concatenate network... why does it still work?
    			key7 = key6[0] + ":" + key6[1];
    			//key7 = concatenateNetwork(key6[0], key6[1]);
    			if (repository.get(key7)!=null){
    				value7 = repository.get(key7);
    			}
    			else{
    				//if (testClassification(key6[0], key6[1])){
            			value7 = testModuleClinical(key7, 1, false);
            			repository.put(key7, value7);
    				//}
    				//else{
    				//	value7 = 2.0;
    				//}
    			}

    			if (value7 < pairwise.get(key6)[0] && value7 < pairwise.get(key6)[1]){
        		pairwiseConcat.put(key7, value7);
        		pairwiseConcatMemory.put(key7, key6);
        			if (value7<minVal){
        				minVal = value7;
        				minKey = key7;
        			}
    			}
    		}

    		if (pairwiseConcat.isEmpty()){
    			break;
    		}
    		
    		//here, if there are two keys having the same min value, it may be different from R (R picks arbitrarily anyways)
    		
    		hubletsTested.remove(pairwiseConcatMemory.get(minKey)[0]);
    		hubletsTested.remove(pairwiseConcatMemory.get(minKey)[1]);
    		hubletsTested.put(minKey, pairwiseConcat.get(minKey));
    		
    		if(hubletsTested.size()<2){
    			break;
    		}
    		
    		pairwise.clear();
    		pairwiseConcat.clear();
    		pairwiseConcatMemory.clear();
    		
    	}
    	
    	Double finalTestValue;
    	ArrayList<String> toBeRemoved = new ArrayList<String>();
    	
    	for (String finalTest : hubletsTested.keySet()){
    		//this can be skipped? (flag?)
    		finalTestValue = testModuleClinical(finalTest, 2, true);
    		if (finalTestValue==null){
    			toBeRemoved.add(finalTest);
    		}
    	}
    	
    	for (String remove : toBeRemoved){
    		hubletsTested.remove(remove);
    	}
    	
    	if (hubletsTested.isEmpty()){
    		String emptySet = new String();
    		emptySet = "none";
    		hubletsTested.put(emptySet, Double.valueOf(1));
    		return hubletsTested;
    	}
    	
    	
    	//filter out unique sample hublets (whichUnique)
    	
    	HashMap<String, String> hubletPatientSamples = new HashMap<String, String>();
    	
    	for(String finalFinalTest : hubletsTested.keySet()){
    		String[] nodes = finalFinalTest.split(":");
    		String allSampleString = "";
    		for (int j=0; j<nodes.length; j++){
    			String thesePatientSamples = allGeneSamplesMap.get(nodes[j]);
    			if (!thesePatientSamples.equals("no_sample")){
    				allSampleString = allSampleString + thesePatientSamples + ":";
    			}
    		}
    		
    		//TODO: could cause array out of bounds error:
    		if (allSampleString.length()>=2){
    			allSampleString = allSampleString.substring(0, allSampleString.length()-1);
    		}
    			
    		hubletPatientSamples.put(finalFinalTest, allSampleString);
    	}
    	
    	//this next part is O(n^2) :S
    	
    	
    	HashMap<String, Boolean> shouldKeep = new HashMap<String, Boolean>();
    	for (String hubs : hubletPatientSamples.keySet()){
    		shouldKeep.put(hubs, true);
    	}
    	
    	
    	for (String firstSet : hubletPatientSamples.keySet()){
    		for (String secondSet : hubletPatientSamples.keySet()){
    			if (!firstSet.equals(secondSet)){
    				HashSet<String> compareSet = new HashSet<String>();
    				String[] firstCompare =  hubletPatientSamples.get(firstSet).split(":");
    				for (int i=0; i<firstCompare.length; i++){
    					compareSet.add(firstCompare[i]);
    				}
        			int beforeSize = compareSet.size();
        			
        			String[] secondCompare = hubletPatientSamples.get(secondSet).split(":");
        			for (int i=0; i<secondCompare.length; i++){
        				compareSet.add(secondCompare[i]);
        			}
        			int afterSize = compareSet.size();
        			
        			if (beforeSize==afterSize && hubletsTested.get(secondSet)>hubletsTested.get(firstSet)){
        				shouldKeep.put(secondSet, false);
        			}
    			}
    			
    		}
    	}
    	
    	ArrayList<String> toBeRemoved2 = new ArrayList<String>();
    	
    	for (String ahh : hubletPatientSamples.keySet()){
    		if (shouldKeep.get(ahh)==false){
    			toBeRemoved2.add(ahh);
    		}
    	}
    	
    	
    	for (String omg : toBeRemoved2){
    		hubletsTested.remove(omg);
    	}

    	repository.clear();
    	
    	return hubletsTested;
	}
	
	
	public int getNumberTests(){
		return this.numberTests;
	}
	
	
	
	public String concatenateNetwork(String one, String two){
		String newNetwork = new String();
		newNetwork = "";
		HashSet<String> newHash = new HashSet<String>();
		
		String[] oneSplit = one.split(":");
		String[] twoSplit = two.split(":");
		
		for (int i=0; i<oneSplit.length; i++){
			newHash.add(oneSplit[i]);
		}
		for (int i=0; i<twoSplit.length; i++){
			newHash.add(twoSplit[i]);
		}
		int k=0;
		for (String addHash : newHash){
			if (k==newHash.size()-1){
				newNetwork = newNetwork + addHash;
			}
			else{
				newNetwork = newNetwork + addHash + ":";
			}
			k++;
		}
		
		return newNetwork;
		
	}
	
	public Double testModuleFisher(String thisNetwork, int limit){

		
		String[] genes = thisNetwork.split(":");
		
		ArrayList<String> patients = new ArrayList<String>();
		String[] thesePatients;
		
		for (int i=0; i<genes.length; i++){
			thesePatients = allGeneSamplesMap.get(genes[i]).split(":");
			for (int t=0; t<thesePatients.length; t++){
				patients.add(thesePatients[t]);
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
		
		if (alpha<limit){
			return null;
		}
		
		
		int[][] matrix = new int[clinicalVariableHash.size()][2];
		
		for (int i=0; i<matrix.length; i++){
			for (int j=0; j<matrix[i].length; j++){
				matrix[i][j]=0;
			}
		}
		
		for (int k=0; k<otherValues.size(); k++){
			if (var2patients[k]==true){
				for (int i=0; i<hashArray.size(); i++){
					if (clinicalVariableMap.get(otherPatients[k]).equals(hashArray.get(i))){
						matrix[i][0]++;
					}
				}
			}
			else{
				for (int i=0; i<hashArray.size(); i++){
					if (clinicalVariableMap.get(otherPatients[k]).equals(hashArray.get(i))){
						matrix[i][1]++;
					}
				}
			}
		}
		
		FishersExact fe = new FishersExact(matrix);
		return fe.fisher2c();
	}
	
	
	
	public int testModuleBoolean(String thisNetwork){
		String[] genes = thisNetwork.split(":");
		HashSet<String> truePatients = new HashSet<String>();
		String[] thesePatients;
		
		if (genes[0].equals("none")){
				return 2;
		}
		
		for (int i=0; i<genes.length; i++){
			thesePatients = allGeneSamplesMap.get(genes[i]).split(":");
			for (int t=0; t<thesePatients.length; t++){
				if (!thesePatients[t].equals("no_sample"))
				truePatients.add(thesePatients[t]);
			}
		}

		int alpha=truePatients.size();
		
		ArrayDeque<Double> time1 = new ArrayDeque<Double>();
		ArrayDeque<Double> time2 = new ArrayDeque<Double>();
		ArrayDeque<Double> censor1 = new ArrayDeque<Double>();
		ArrayDeque<Double> censor2 = new ArrayDeque<Double>();
		
		for (int i=0; i<allPatients.length; i++){
			if (truePatients.contains(allPatients[i])){
				time1.add(followupDays[i]);
				censor1.add(censor[i]);
			}
			else{
				time2.add(followupDays[i]);
				censor2.add(censor[i]);
			}
		}
		
		if (logRankObject.logRankSurvivalTest(time1, time2, censor1, censor2)){
			return 1;
		}else{
			return 0;
		}
	}
	
	public Double testModuleClinical(String thisNetwork, int limit, boolean flag){
		if (this.statTest.equals("fisher")){
			return testModuleFisher(thisNetwork, limit);
		}

		String[] genes = thisNetwork.split(":");
		HashSet<String> truePatients = new HashSet<String>();
		String[] thesePatients;
		
		for (int i=0; i<genes.length; i++){
			thesePatients = allGeneSamplesMap.get(genes[i]).split(":");
			for (int t=0; t<thesePatients.length; t++){
				if (!thesePatients[t].equals("no_sample"))
				truePatients.add(thesePatients[t]);
			}
		}

		
		int alpha=truePatients.size();
		
		/*
		System.out.println("Alpha size: " + alpha);
		System.out.println("truePatients size: " + truePatients.size());
		System.out.println("|");
		*/
		
		if (alpha<limit){
			return null;
		}
		
		if (flag){
			return Double.valueOf(1);
		}
		
		ArrayDeque<Double> time1 = new ArrayDeque<Double>();
		ArrayDeque<Double> time2 = new ArrayDeque<Double>();
		ArrayDeque<Double> censor1 = new ArrayDeque<Double>();
		ArrayDeque<Double> censor2 = new ArrayDeque<Double>();
		
		for (int i=0; i<allPatients.length; i++){
			if (truePatients.contains(allPatients[i])){
				time1.add(followupDays[i]);
				censor1.add(censor[i]);
			}
			else{
				time2.add(followupDays[i]);
				censor2.add(censor[i]);
			}
		}

		Double pValue = Double.valueOf(0);
		
		if (this.statTest.equals("logRank")){
			Double[] result = logRankObject.logRank(time1, time2, censor1, censor2);
			pValue = result[2];
			this.numberTests++;
		}
		
		else if (this.statTest.equals("CoxPh")){

			double[] group = new double[allPatients.length];
			for (int k=0; k<allPatients.length; k++){
				if (truePatients.contains(allPatients[k])){
					group[k]=1.0;
				}
				else{
					group[k]=2.0;
				}
			}

			pValue = coxModel.cox(group);
			
			/*
			CoxRegression testclass1 = new CoxRegression(0.05, this.followupDays, this.censor, group, this.age); 
			double [] pValueArray = testclass1.pValue; 
			if (Double.isNaN(pValueArray[1])){
				pValue = Double.valueOf(0);
			}
			else{
				pValue = pValueArray[1];
			}
			*/
			/*
			ConnectR rconnection = new ConnectR(this.followupDays, this.censor, group, this.age);
			try {
				pValue = Double.valueOf(rconnection.coxph());
			} catch (REngineException e) {
				e.printStackTrace();
			}
			*/
		}

		return pValue;
	}
	
	public void initOther(){
		otherPatients = new String[this.otherValues.size()];
		for (int k=0; k<this.otherValues.size(); k++){
			otherPatients[k] = this.otherValues.get(k)[0];
		}
		
		clinicalVariableMap = new HashMap<String, String>();
		clinicalVariableHash = new HashSet<String>();
		for (int k=0; k<this.otherValues.size(); k++){
			clinicalVariableMap.put(this.otherValues.get(k)[0], this.otherValues.get(k)[1]);
			clinicalVariableHash.add(this.otherValues.get(k)[1]);
		}
		hashArray = new ArrayList<String>();
		for (String hashElement : clinicalVariableHash){
			hashArray.add(hashElement);
		}
		
		clinicalVariable = new String[this.otherValues.size()];
		for (int k=0; k<this.otherValues.size(); k++){
			clinicalVariable[k] = this.otherValues.get(k)[1];
		}
	}
	
	
	public void initClinicals(){
		//have a "load (String/Double) Column" method?

		allPatients = new String[this.clinicalValues.size()];
		for (int k=0; k<this.clinicalValues.size(); k++){
			allPatients[k]=this.clinicalValues.get(k)[0];
		}
		
		status = new boolean[this.clinicalValues.size()];
		for (int k=0; k<this.clinicalValues.size(); k++){
			if (clinicalValues.get(k)[1].equals("DECEASED")){
				status[k]=true;
			}
			else{
				status[k]=false;
			}
		}
		
		followupDays = new double[this.clinicalValues.size()];
		for (int k=0; k<this.clinicalValues.size(); k++){
			followupDays[k] = Double.valueOf(clinicalValues.get(k)[2]);
		}
		
		daysFromBirth = new double[this.clinicalValues.size()];
		for (int k=0; k<this.clinicalValues.size(); k++){
			daysFromBirth[k] = Double.valueOf(clinicalValues.get(k)[3]);
		}
		
		age = new double[this.clinicalValues.size()];
		for (int k=0; k<this.clinicalValues.size(); k++){
			age[k]=(-1*daysFromBirth[k]+followupDays[k]);
			//System.out.println(age[k]);
		}
		

		censor = new double[this.clinicalValues.size()];
		for (int k=0; k<this.clinicalValues.size(); k++){
			if (status[k]==true){
				censor[k]=1;
				//System.out.println(censor[k]);
			}
			else{
				censor[k]=0;
				//System.out.println(censor[k]);
			}
			
		}
		
	}
	

	public ArrayList<String> compressTokens(HashSet<String> allPaths, String seedName){
		
		ArrayList<String> compress = new ArrayList<String>();

		//3. make a list of patient samples concatenated (each item of list one CyNetwork samples); make sure if nodes in the CyNetwork have same sample, 
		//only add one copy of sample
		
		HashMap<String, String> allPatients = new HashMap<String, String>();
		
		
		for (String network : allPaths){
			String patients = allGeneSamplesMap.get(seedName);
			String[] genes = network.split(":");
			for (int i=0; i<genes.length; i++){
				if (!genes[i].equals(seedName) && !allGeneSamplesMap.get(genes[i]).equals("no_sample")){
					patients = patients + ":" + allGeneSamplesMap.get(genes[i]);
				}
			}

			if (allPatients.get(patients)==null){
				allPatients.put(patients, network);
			}
			else{
				String[] genes2 = allPatients.get(patients).split(":");
				if(genes.length<genes2.length){
					allPatients.put(patients, network);
				}
			}
		}
		
		
		String seedClassification = survivalClassification.get(seedName);
		//System.out.println(seedClassification);

		if (seedClassification!=null){
			if (seedClassification.equals("LONG") || seedClassification.equals("MEDIUM")){
				for (String list : allPatients.keySet()){
					String[] genes = allPatients.get(list).split(":");
					boolean toAdd = true;
					for (int i=0; i<genes.length; i++){
						if (survivalClassification.get(genes[i])!=null){
							if (survivalClassification.get(genes[i]).equals("SHORT")){
								toAdd = false;
							}
						}
					}
					if (toAdd){
						compress.add(allPatients.get(list));
					}
				}
			}
			else if (seedClassification .equals("SHORT")){
				for (String list : allPatients.keySet()){
					String[] genes = allPatients.get(list).split(":");
					boolean toAdd = true;
					for (int i=0; i<genes.length; i++){
						if (survivalClassification.get(genes[i])!=null){
							if (survivalClassification.get(genes[i]).equals("LONG")){
								toAdd = false;
							}
						}
					}
					if (toAdd){
						compress.add(allPatients.get(list));
					}
				}
			}
		}
		
		else{
			for (String list : allPatients.keySet()){
				compress.add(allPatients.get(list));
			}
		}

		return compress;
	}
	
	
	public HashMap<HashSet<String>, Double> expandToHashSet(HashMap<String, Double> original){
		HashMap<HashSet<String>, Double> returnValue = new HashMap<HashSet<String>, Double>();
		for (String genez : original.keySet()){
			String[] expanded = genez.split(":");
			HashSet<String> newHashSet = new HashSet<String>();
			for (int i=0; i<expanded.length; i++){
				newHashSet.add(expanded[i]);
			}
			returnValue.put(newHashSet, original.get(genez));
		}
		
		return returnValue;
	}

	public void shuffleLabels(){
		Collections.shuffle(this.allSamples);
		for (int i=0; i<allGenes.size(); i++){
			allGeneSamplesMap.put(allGenes.get(i),  allSamples.get(i));
		}
	}
	
}
