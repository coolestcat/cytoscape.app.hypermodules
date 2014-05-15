package org.cytoscape.hypermodules.internal;

import java.awt.Color;
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
import com.xeiam.xchart.Series;
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
	double selectedP;
	String foregroundvariable;
	
	public ChartDisplayFisher(String foregroundvariable, Double selectedP, ArrayList<String[]> otherValues, ArrayList<String[]> sampleValues, CyNetwork network){
		this.foregroundvariable = foregroundvariable;
		this.otherValues = otherValues;
		this.sampleValues = sampleValues;
		this.network = network;
		this.selectedP = selectedP;
		
		allGeneSamplesMap = new HashMap<String, String>();
		
		for (int i=0; i<sampleValues.size(); i++){
			if (allGeneSamplesMap.get(sampleValues.get(i)[0])!=null){
				String sti = allGeneSamplesMap.get(sampleValues.get(i)[0]);
				sti = sti + ":" + sampleValues.get(i)[1];
				allGeneSamplesMap.put(sampleValues.get(i)[0], sti);
			}
			else{
			//System.out.println(sampleValues.get(i)[1]);
			allGeneSamplesMap.put(sampleValues.get(i)[0], sampleValues.get(i)[1]);
			}
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
		
		/*
		System.out.println("allVariableNames: ");
		for (int h=0; h<allVariableNames.size(); h++){
			System.out.println(allVariableNames.get(h));
		}
		*/
		
		String[] genes = s.split(":|;");

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
		
		/*
		System.out.println("Before");
		for (String r : matrix.keySet()){
			System.out.println(r + " : " + matrix.get(r));
		}
		*/
		
		double of = 0;
		double nof = 0;
		for (int k=0; k<otherValues.size(); k++){
			if (var2patients[k]==true){
				//for (int i=0; i<allVariableNames.size(); i++){
					if (otherValues.get(k)[1].equals(foregroundvariable)){
						of++;
						//System.out.println(allVariableNames.get(i));
						//int c = matrix.get(allVariableNames.get(i));
						//matrix.put(allVariableNames.get(i), c+1);
					}
					else{
						nof++;
					}
					
				//}
			}
		}
		
		/*
		System.out.println("After");
		for (String r : matrix.keySet()){
			System.out.println(r + " : " + matrix.get(r));
		}
		*/
		
		/*
		ArrayList<Number> observed = new ArrayList<Number>();
		for (int i=0; i<allVariableNames.size(); i++){
			observed.add((double) matrix.get(allVariableNames.get(i)));
		}
		
		ArrayList<Number> expected = new ArrayList<Number>();
		for (int i=0; i<percentages.length; i++){
			expected.add((double) percentages[i] * alpha);
		}
		*/
		
		
		ArrayList<Number> observed = new ArrayList<Number>();
		observed.add(of);
		observed.add(nof);

		
		//double of = (double) matrix.get(foregroundvariable);
		double ef = 100;
		double otheref = 0;
		for (int i=0; i<percentages.length; i++){
			if (allVariableNames.get(i).equals(foregroundvariable)){
				ef = (double) percentages[i]*alpha;
			}
			else{
				otheref += percentages[i];
			}
		}
		
		otheref = otheref*alpha;
		ArrayList<Number> expected = new ArrayList<Number>();
		expected.add(Math.round(ef));
		expected.add(Math.round(otheref));
		
		
		/*
		System.out.println("Observed: " );
		for (int h = 0; h< observed.size(); h++){
			System.out.println(observed.get(h));
		}
		
		System.out.println("Expected: ");
		for (int h = 0; h< observed.size(); h++){
			System.out.println(observed.get(h));
		}
		*/
		
		
		String[] toChart = new String[allVariableNames.size()];
		

		toChart[0] = foregroundvariable;
		toChart[1] = "NOT " + foregroundvariable;
/*
		System.out.println(observed.get(0));
		System.out.println(expected.get(0));
		System.out.println(toChart[0]);
		
		System.out.println(observed.get(1));
		System.out.println(expected.get(1));
		System.out.println(toChart[1]);
*/
		
		
		Chart chart = null;
		
		
		
		chart = new ChartBuilder().chartType(ChartType.Bar).width(800).height(600).title("Number of Patients with status " + foregroundvariable + " in module, p = " + roundToSignificantFigures(selectedP, 6)).xAxisTitle("Module - " + s).yAxisTitle("Number of Patients").theme(ChartTheme.GGPlot2).build();
		Series s1 = chart.addSeries("observed", new ArrayList<String>(Arrays.asList(new String[] {toChart[0], toChart[1]})), new ArrayList<Number>(Arrays.asList(new Number[]{observed.get(0), observed.get(1)})));
		Series s2 = chart.addSeries("expected", new ArrayList<String>(Arrays.asList(new String[] {toChart[0], toChart[1]})), new ArrayList<Number>(Arrays.asList(new Number[]{expected.get(0), expected.get(1)})));
		/*
		if (allVariableNames.size() == 2){
			//System.out.println("2");
			chart = new ChartBuilder().chartType(ChartType.Bar).width(800).height(600).title("Fisher's Exact Test Observed vs. Expected - Genes: " + s + " - PValue: " + roundToSignificantFigures(selectedP, 6)).xAxisTitle("").yAxisTitle("Number of Patients").theme(ChartTheme.GGPlot2).build();
			chart.addCategorySeries("observed", new ArrayList<String>(Arrays.asList(new String[] {toChart[1], toChart[0]})), new ArrayList<Number>(Arrays.asList(new Number[]{observed.get(1), observed.get(0)})));
			chart.addCategorySeries("expected", new ArrayList<String>(Arrays.asList(new String[] {toChart[1], toChart[0]})), new ArrayList<Number>(Arrays.asList(new Number[]{expected.get(1), expected.get(0)})));
		}
		else{
			chart = new ChartBuilder().chartType(ChartType.Bar).width(800).height(600).title("Fisher's Exact Test Observed vs. Expected - \n Genes: " + s + " - \n PValue: " + roundToSignificantFigures(selectedP, 6)).xAxisTitle("").yAxisTitle("Number of Patients").theme(ChartTheme.GGPlot2).build();
			chart.addCategorySeries("observed", new ArrayList<String>(Arrays.asList(toChart)), observed);
			chart.addCategorySeries("expected", new ArrayList<String>(Arrays.asList(toChart)), expected);
		}	
		*/
		//chart = new ChartBuilder().chartType(ChartType.Bar).width(800).height(600).title("Fisher's Exact Test Observed vs. Expected - Genes: " + s + " - PValue: " + roundToSignificantFigures(selectedP, 6)).xAxisTitle("").yAxisTitle("Number of Patients").theme(ChartTheme.GGPlot2).build();
		
		//Series s1 = chart.addCategorySeries("observed", new ArrayList<String>(Arrays.asList(new String[] {foregroundvariable})), new ArrayList<Number>(Arrays.asList(new Number[]{of})));
		//Series s2 = chart.addCategorySeries("expected", new ArrayList<String>(Arrays.asList(new String[] {foregroundvariable})), new ArrayList<Number>(Arrays.asList(new Number[]{ef})));
		s1.setMarkerColor(Color.orange);
		s1.setLineColor(Color.orange);
		s2.setMarkerColor(Color.gray);
		s2.setLineColor(Color.gray);
		
		new SwingWrapper(chart, 0.0).displayFisherChart();
	}
	
	//rounding function
	public static double roundToSignificantFigures(double num, int n) {
	    if(num == 0) {
	        return 0;
	    }

	    final double d = Math.ceil(Math.log10(num < 0 ? -num: num));
	    final int power = n - (int) d;

	    final double magnitude = Math.pow(10, power);
	    final long shifted = Math.round(num*magnitude);
	    return shifted/magnitude;
	}
	
}
