package org.cytoscape.hypermodules.internal;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

import com.xeiam.xchart.Chart;
import com.xeiam.xchart.ChartBuilder;
import com.xeiam.xchart.Series;
import com.xeiam.xchart.SeriesMarker;
import com.xeiam.xchart.StyleManager.ChartTheme;

import org.cytoscape.hypermodules.internal.statistics.LogRankTest;

public class ChartDisplay {
	
	private ArrayList<String[]> clinicalValues;
	private ArrayList<String[]> sampleValues;
	public CyNetwork network;
	private HashMap<String, String> allGeneSamplesMap;
	
	private String[] allPatients;
	private boolean[] status;
	private double[] daysFromBirth;
	private double[] followupDays;
	private double[] age;
	private double[] censor;
	
	private LogRankTest lrt;
	
	public ChartDisplay(ArrayList<String[]> clinicalValues, ArrayList<String[]> sampleValues, CyNetwork network){
		this.clinicalValues = clinicalValues;
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
		initClinicals();
		lrt = new LogRankTest(this.followupDays);
		
		
	}	
	
	public void initClinicals(){
		//have a "load (String/Double) Column" method?
		allPatients = new String[5];
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
		}
		

		censor = new double[this.clinicalValues.size()];
		for (int k=0; k<this.clinicalValues.size(); k++){
			if (status[k]==true){
				censor[k]=1;
			}
			else{
				censor[k]=0;
			}
			
		}
		/*
		System.out.println("censor: ");
		for (int i=0; i<censor.length; i++){
			System.out.println(censor[i]);
		}
		*/
	}
	
	public void display(String s){
		String[] genes = s.split(":");
		
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
		
		double[] time1 = new double[alpha];
		double[] time2 = new double[allPatients.length-alpha];
		double[] censor1 = new double[alpha];
		double[] censor2 = new double[allPatients.length-alpha];
		
		int x = 0;
		int y = 0;
	
		for (int i=0; i<allPatients.length; i++){
			if (truePatients.contains(allPatients[i])){
				time1[x] = followupDays[i];
				censor1[x] = censor[i];
				x++;
			}
			else{
				time2[y] = followupDays[i];
				censor2[y] = censor[i];
				y++;
			}
		}
		
		ArrayDeque<Double> sortedTime1 = new ArrayDeque<Double>();
		ArrayDeque<Double> sortedTime2 = new ArrayDeque<Double>();
		ArrayDeque<Double> sortedCensor1 = new ArrayDeque<Double>();
		ArrayDeque<Double> sortedCensor2 = new ArrayDeque<Double>();
		
		for (int i=0; i<time1.length; i++){
			//allTimeHash.add(time1[i]);
			sortedTime1.add(time1[i]);
			sortedCensor1.add(censor1[i]);
		}
		
		for (int i=0; i<time2.length; i++){
			//allTimeHash.add(time2[i]);
			sortedTime2.add(time2[i]);
			sortedCensor2.add(censor2[i]);
		}
		
		Double[] lrvalue = lrt.logRank(sortedTime1, sortedTime2, sortedCensor1, sortedCensor2);
		
		/*
		System.out.println("time1: ");
		for (int i=0; i<time1.length; i++){
			System.out.println(time1[i]);
		}
		System.out.println("time2: ");
		for (int i=0; i<time2.length; i++){
			System.out.println(time2[i]);
		}
		System.out.println("censor1: ");
		for (int i=0; i<censor1.length; i++){
			System.out.println(censor1[i]);
		}
		System.out.println("censor2: " );
		for (int i=0; i<censor2.length; i++){
			System.out.println(censor2[i]);
		}
		*/
		
		
		ArrayList<Double> xData1 = new ArrayList<Double>();
		ArrayList<Double> xData2 = new ArrayList<Double>();
		
		ArrayList<Double> yData1 = new ArrayList<Double>();
		ArrayList<Double> yData2 = new ArrayList<Double>();
		
		double survival = 1;
		double numPeopleLeft = alpha;
		
		xData1.add(0.0);
		yData1.add(1.0);
		
		xData2.add(0.0);
		yData2.add(1.0);
		
		
		for (int i=0; i<censor1.length; i++){
			if (censor1[i]==1){
				xData1.add(time1[i]);
				xData1.add(time1[i]);
				yData1.add(survival);
				numPeopleLeft--;
				survival = numPeopleLeft/(double) alpha;
				yData1.add(survival);
			}
			else{
				xData1.add(time1[i]);
				yData1.add(survival);
			}
		}
		
		survival = 1;
		numPeopleLeft = allPatients.length-alpha;
		System.out.println("numPeopleLeft: " + numPeopleLeft);
		
		for(int i=0; i<censor2.length; i++){
			if (censor2[i]==1){
				xData2.add(time2[i]);
				xData2.add(time2[i]);
				yData2.add(survival);
				numPeopleLeft--;
				survival = numPeopleLeft/ (double) (allPatients.length-alpha);
				System.out.println("new survival: " + survival);
				yData2.add(survival);	
			}
			else{
				xData2.add(time2[i]);
				yData2.add(survival);
			}
		}
		
		
		double[] xd1 = new double[xData1.size()];
		double[] xd2 = new double[xData2.size()];
		
		double[] yd1 = new double[yData1.size()];
		double[] yd2 = new double[yData2.size()];
		
		for (int i=0; i<xData1.size(); i++){
			xd1[i] = xData1.get(i);
			yd1[i] = yData1.get(i);
		}
		
		for (int i=0; i<xData2.size(); i++){
			xd2[i] = xData2.get(i);
			yd2[i] = yData2.get(i);
		}
		

	    Chart chart = new ChartBuilder().width(800).height(600).theme(ChartTheme.GGPlot2).build();
	    chart.setChartTitle("Kaplan-Meier Survival Analysis");
	    chart.setXAxisTitle("Time");
	    chart.setYAxisTitle("Survival Probability");
	    chart.getStyleManager().setYAxisMin(0.0);
	    chart.getStyleManager().setYAxisMax(1);
	    Series series = chart.addSeries("Patients with mutation in module", xd1, yd1);
	    Series series2 = chart.addSeries("Other patients", xd2, yd2);
	    series.setLineColor(Color.RED);
	    series.setMarker(SeriesMarker.CIRCLE);
	    series.setMarkerColor(Color.RED);
	    series2.setLineColor(Color.BLACK);
	    series2.setMarker(SeriesMarker.TRIANGLE_UP);
	    series2.setMarkerColor(Color.BLACK);
	    new SwingWrapper(chart, lrvalue[2]).displayChart();
	}
	
	
}
