package org.cytoscape.hypermodules.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.xeiam.xchart.Chart;
import com.xeiam.xchart.ChartBuilder;
import com.xeiam.xchart.StyleManager.ChartTheme;
import com.xeiam.xchart.StyleManager.ChartType;

public class ChartDisplayFisher {


	private ArrayList<String[]> otherValues;
	private ArrayList<String[]> sampleValues;
	public CyNetwork network;
	private HashMap<String, String> allGeneSamplesMap;
	
	private String[] allPatients;
	private String[] variable;
	private HashSet<String> allVariables;
	private ArrayList<String> allVariableNames;
	private Multimap<String, String> variable2patients;
	double[] percentages;
	
	public ChartDisplayFisher(ArrayList<String[]> otherValues, ArrayList<String[]> sampleValues, CyNetwork network){
		this.otherValues = otherValues;
		this.sampleValues = sampleValues;
		this.network = network;
		
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
		initOther();
	}
	
	public void initOther(){
		allPatients = new String[this.otherValues.size()];
		for (int i=0; i<otherValues.size(); i++){
			allPatients[i] = otherValues.get(i)[0];
		}
		
		allVariables = new HashSet<String>();
		
		variable = new String[this.otherValues.size()];
		for (int i=0; i<otherValues.size(); i++){
			variable[i] = otherValues.get(i)[1];
			allVariables.add(otherValues.get(i)[1]);
		}
		
		variable2patients = ArrayListMultimap.create();
		for (String s : allVariables){
			for (int i=0; i<variable.length; i++){
				if (variable[i].equals(s)){
					variable2patients.put(s, variable[i]);
				}
			}
		}	
		allVariableNames = new ArrayList<String>();
		for (String s : allVariables){
			allVariableNames.add(s);
		}
		
		percentages = new double[allVariableNames.size()];
		int i=0;
		for (String s : allVariableNames){
			percentages[i] = variable2patients.get(s).size() / (double) allPatients.length;
			i++;
		}
	}
	
	public void display(String s){
		String[] genes = s.split(":");

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
		
		HashMap<String, Integer> matrix = new HashMap<String, Integer>();
		for (String x : allVariableNames){
			matrix.put(x, 0);
		}
		
		for (int k=0; k<otherValues.size(); k++){
			if (var2patients[k]==true){
				for (int i=0; i<allVariableNames.size(); i++){
					if (otherValues.get(i)[1].equals(allVariableNames.get(i))){
						//System.out.println(allVariableNames.get(i));
						int c = matrix.get(allVariableNames.get(i));
						matrix.put(allVariableNames.get(i), c+1);
					}
				}
			}
		}
		
		for (String r : matrix.keySet()){
			System.out.println(r + " : " + matrix.get(r));
		}
		
		ArrayList<Number> observed = new ArrayList<Number>();
		for (int i=0; i<allVariableNames.size(); i++){
			observed.add((double) matrix.get(allVariableNames.get(i)));
		}
		
		ArrayList<Number> expected = new ArrayList<Number>();
		for (int i=0; i<percentages.length; i++){
			expected.add((double) percentages[i] * alpha);
		}
		
		String[] toChart = new String[allVariableNames.size()];
		for (int y =0; y<allVariableNames.size(); y++){
			toChart[y] = allVariableNames.get(y);
		}
	
		Chart chart = new ChartBuilder().chartType(ChartType.Bar).width(800).height(600).title("Fisher's Exact Test Observed vs. Expected").xAxisTitle("").yAxisTitle("Number of Patients").theme(ChartTheme.GGPlot2).build();
		chart.addCategorySeries("observed", new ArrayList<String>(Arrays.asList(toChart)), observed);
		chart.addCategorySeries("expected", new ArrayList<String>(Arrays.asList(toChart)), expected);

		new SwingWrapper(chart, 0.0).displayFisherChart();
	}
	
}
