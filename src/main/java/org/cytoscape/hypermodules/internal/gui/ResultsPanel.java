package org.cytoscape.hypermodules.internal.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.hypermodules.internal.ChartDisplay;
import org.cytoscape.hypermodules.internal.ChartDisplayFisher;
import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.hypermodules.internal.task.GenerateNetworkTask;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

/**
 * 
 * the JPanel for displaying and exporting HyperModules results
 * @author alvinleung
 *
 */
public class ResultsPanel extends JPanel implements CytoPanelComponent, ActionListener, MouseListener {
	
	/**
	 * all results from AlgorithmTask.
	 * Formatted as follows:
	 * HashMap<seedName, seedData>
	 * seedData is a hashmap of 3 arraylists of HashMap<String, Double> and a multimap
	 * arraylist.get(0) - original test results (module - statistical test pValue)
	 * arraylist.get(1) - FDR permutation p values (module - FDR permutation test pValue)
	 * arraylist.get(2) - classification of high or low (module - 0,1, or 2)
	 * multimap - all the shuffled data, in case user wants to export all the results
	 * 
	 */
	private HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults;
	/**
	 * export all the hypermodules algorithm data 
	 */
	//private JButton export;
	/**
	 * export the most correlated modules according to the algorithm
	 */
	private JButton exportMostCorrelated;
	/**
	 * visualize the most correlated modules nicely in a new Cytoscape network view
	 */
	private JButton generate;
	/**
	 * discard the current results panel along with its data
	 */
	private JButton discard;
	/**
	 * cytoscape utils
	 */
	private CytoscapeUtils utils;
	/**
	 * table to view results
	 */
	private JScrollPane viewer;
		private JTable resultsTable;
	/**
	 * button panel
	 */
	private JPanel buttonPanel;
	//private JPanel buttonPanel2;
	
	private JButton setCutoff;
	private JTextField cutoff;
	private JPanel panel3;
	/**
	 * the network that the algorithm was run on (may not be current selected network)
	 */
	private CyNetwork network;
	/**
	 * test parameters
	 * length, expandOption, stat, nShuffled - obtained from user input in main panel
	 */
	private HashMap<String, String> parameters;
	/**
	 * genes2samples
	 */
	private ArrayList<String[]> sampleValues;
	
	private ArrayList<String[]> clinicalValues;
	
	private ArrayList<String[]> otherValues;
	
	private ArrayList<String[]> addToTable;

	private double pValueCutoff;
	
	private String[] sas;
	
	private JButton chart;
	
	private double selectedP;
	
	private JTableHeader tableHeader;
	/**
	 * constructor
	 * @param parameters
	 * @param utils
	 * @param allResults
	 * @param network
	 */
	public ResultsPanel(HashMap<String, String> parameters, CytoscapeUtils utils, HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults, CyNetwork network, ArrayList<String[]> sampleValues, ArrayList<String[]> clinicalValues, ArrayList<String[]> otherValues){
		this.utils = utils;
		this.allResults = allResults;
		this.network = network;
		this.parameters = parameters;
		this.sampleValues = sampleValues;
		this.clinicalValues = clinicalValues;
		this.otherValues = otherValues;
		makeComponents();
		makeLayout();
	}

	/**
	 * make components
	 */
	public void makeComponents(){
		//export  = new JButton("export");
		//export.addActionListener(this);
		exportMostCorrelated = new JButton("export results");
		exportMostCorrelated.addActionListener(this);
		generate = new JButton("visualize");
		generate.addActionListener(this);
		discard = new JButton("discard results");
		discard.addActionListener(this);
		chart = new JButton("display chart");
		chart.addActionListener(this);
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridBagLayout());
		//buttonPanel.add(export);
		buttonPanel.add(exportMostCorrelated);
		buttonPanel.add(chart);
		buttonPanel.add(generate);
		buttonPanel.add(discard);
		setCutoff = new JButton("Set P-Value Cutoff");
		setCutoff.addActionListener(this);
		cutoff = new JTextField("0.05");
		panel3 = new JPanel();
		panel3.setLayout(new GridBagLayout());
		panel3.add(setCutoff);
		panel3.add(cutoff);
		this.pValueCutoff = 0.05;
		if (parameters.get("stat").equals("logRank")){
			setUpTableLogRank();
		}
		else if (parameters.get("stat").equals("fisher")){
			setUpTableFisher();
		}

	}
	
	public void sortTable(int colNumber){
		System.out.println("col: " + colNumber);
		ArrayList<String[]> newTable = new ArrayList<String[]>();
		
		Model tab = null;
		if (parameters.get("stat").equals("logRank")){
			String[] c = {"Seed", "Genes", "Log-Rank P-Value", "Empirical FDR P-value", "Classification"};
			tab = new Model(c);
		}
		else{
			String[] c = {"Seed", "Genes", "Fisher P-Value", "Empirical FDR P-value"};
			tab = new Model(c);
		}
		
		if (colNumber == 2 || colNumber == 3){
			Multimap<Double, String[]> mds = ArrayListMultimap.create();
			ArrayList<Double> toSort = new ArrayList<Double>();
			HashSet<Double> gg = new HashSet<Double>();
			for (int i=0; i<addToTable.size(); i++){
				gg.add(Double.valueOf(addToTable.get(i)[colNumber]));
				String[] mdsentry = new String[4];
				if (parameters.get("stat").equals("logRank")){
					mdsentry[0] = addToTable.get(i)[0];
					mdsentry[1] = addToTable.get(i)[1];
					if (colNumber == 3){
						mdsentry[2] = addToTable.get(i)[2];
					}
					else{
						mdsentry[2] = addToTable.get(i)[3];
					}
					mdsentry[3] = addToTable.get(i)[4];
				}
				else{//fisher
					mdsentry[0] = addToTable.get(i)[0];
					mdsentry[1] = addToTable.get(i)[1];
					if (colNumber == 3){
						mdsentry[2] = addToTable.get(i)[2];
					}
					else{
						mdsentry[2] = addToTable.get(i)[3];
					}
				}
				mds.put(Double.valueOf(addToTable.get(i)[colNumber]), mdsentry);
			}
			
			for (Double t : gg){
				toSort.add(t);
			}
			//System.out.println("toSortsize: " + toSort.size());
			Collections.sort(toSort);
			
			if (parameters.get("stat").equals("logRank")){
				for (int i=0; i<toSort.size(); i++){
					for (String[] d : mds.get(toSort.get(i))){
						//System.out.println(toSort.get(i) + ":" + d);
						String[] newentry = new String[5];
						newentry[0] = d[0];
						newentry[1] = d[1];
						if (colNumber == 2){
							newentry[2] = String.valueOf(toSort.get(i));
							newentry[3] = d[2];
						}
						else{
							newentry[2] = d[2];
							newentry[3] = String.valueOf(toSort.get(i));
						}
						newentry[4] = d[3];
						newTable.add(newentry);
					}
				}
			}
			
			else{
				for (int i=0; i<toSort.size(); i++){
					for (String[] d : mds.get(toSort.get(i))){
						String[] newentry = new String[4];
						newentry[0] = d[0];
						newentry[1] = d[1];
						if (colNumber == 2){
							newentry[2] = String.valueOf(toSort.get(i));
							newentry[3] = d[2];
						}
						else{
							newentry[2] = d[2];
							newentry[3] = String.valueOf(toSort.get(i));
						}
						newTable.add(newentry);
					}
				}
			}
			
			//System.out.println("newTablesize: " + newTable.size());
			
		}
		else{
			ArrayList<String> toSort = new ArrayList<String>();
			Multimap<String, String[]> mds = ArrayListMultimap.create();
			HashSet<String> gg = new HashSet<String>();
			
			for (int i=0; i<addToTable.size(); i++){
				gg.add((addToTable.get(i)[colNumber]));
				String[] mdsentry = new String[4];
				if (parameters.get("stat").equals("logRank")){
					if (colNumber == 0){
						mdsentry[0] = addToTable.get(i)[1];
						mdsentry[1] = addToTable.get(i)[2];
						mdsentry[2] = addToTable.get(i)[3];
						mdsentry[3] = addToTable.get(i)[4];
					}
					else if (colNumber ==1){
						mdsentry[0] = addToTable.get(i)[0];
						mdsentry[1] = addToTable.get(i)[2];
						mdsentry[2] = addToTable.get(i)[3];
						mdsentry[3] = addToTable.get(i)[4];
						
					}
					else if (colNumber == 4){
						mdsentry[0] = addToTable.get(i)[0];
						mdsentry[1] = addToTable.get(i)[1];
						mdsentry[2] = addToTable.get(i)[2];
						mdsentry[3] = addToTable.get(i)[3];
						
					}
				}
				else{//fisher
					if (colNumber == 0){
						mdsentry[0] = addToTable.get(i)[1];
						mdsentry[1] = addToTable.get(i)[2];
						mdsentry[2] = addToTable.get(i)[3];
					}
					else if (colNumber ==1 ){
						mdsentry[0] = addToTable.get(i)[0];
						mdsentry[1] = addToTable.get(i)[2];
						mdsentry[2] = addToTable.get(i)[3];
					}
				}
				mds.put(addToTable.get(i)[colNumber], mdsentry);
			}
			
			for (String s : gg){
				toSort.add(s);
			}
			Collections.sort(toSort);
			
			
			if (parameters.get("stat").equals("logRank")){
				for (int i=0; i<toSort.size(); i++){
					for (String[] d : mds.get(toSort.get(i))){
						String[] newentry = new String[5];
						if (colNumber == 0){
							newentry[0] = toSort.get(i);
							newentry[1] = d[0];
							newentry[2] = d[1];
							newentry[3] = d[2];
							newentry[4] = d[3];
							
						}
						else if (colNumber == 1){
							newentry[0] = d[0];
							newentry[1] = toSort.get(i);
							newentry[2] = d[1];
							newentry[3] = d[2];
							newentry[4] = d[3];
						}
						else if (colNumber == 4){
							newentry[0] = d[0];
							newentry[1] = d[1];
							newentry[2] = d[2];
							newentry[3] = d[3];
							newentry[4] = toSort.get(i);
						}
						newTable.add(newentry);
					}
				}
			}
			
			else{
				for (int i=0; i<toSort.size(); i++){
					for (String[] d : mds.get(toSort.get(i))){
						String[] newentry = new String[4];
						if (colNumber == 0){
							newentry[0] = toSort.get(i);
							newentry[1] = d[0];
							newentry[2] = d[1];
							newentry[3] = d[2];
							
						}
						else if (colNumber == 1){
							newentry[0] = d[0];
							newentry[1] = toSort.get(i);
							newentry[2] = d[1];
							newentry[3] = d[2];
						}
						newTable.add(newentry);
					}
				}
			}
			
			
			
		}
		//System.out.println("reachedddd");
		addToTable = new ArrayList<String[]>();
		addToTable = newTable;
		tab.AddCSVData(addToTable);
		resultsTable = new JTable();
		resultsTable.setModel(tab);
		tableHeader = resultsTable.getTableHeader();
		tableHeader.addMouseListener(this);
		viewer.setViewportView(resultsTable);
	}
	
	
	public void setUpTableFisher(){
		String[] c = {"Seed", "Genes", "Fisher P-Value", "Empirical FDR P-value"};
		Model tab = new Model(c);
		this.sas = new String[2];
		sas[0] = "none";
		addToTable = new ArrayList<String[]>();
		
		for (String key : allResults.keySet()){
			for (ArrayList<HashMap<String,Double>> set : allResults.get(key).keySet()){
				for (String genes : set.get(0).keySet()){
					String[] newEntry = new String[4];
					newEntry[0]=key;
					newEntry[1] = genes;
					newEntry[2] = String.valueOf(set.get(0).get(genes));
					//newEntry[2]=String.valueOf((double)Math.round(set.get(0).get(genes)* 100000) / 100000);
					Double b = set.get(1).get(genes);
					if (b!=null){
						//b = (double)Math.round(b * 100000) / 100000;
					}
					newEntry[3]=String.valueOf(b);

					if (b!=null){
						if (Double.valueOf(newEntry[2])<=this.pValueCutoff && Double.valueOf(newEntry[3])<=this.pValueCutoff){
							addToTable.add(newEntry);
						}
					}
					else{
						if (Double.valueOf(newEntry[2])<=this.pValueCutoff){
							addToTable.add(newEntry);
						}
					}
				}
			}
		}
		
		tab.AddCSVData(addToTable);
		resultsTable = new JTable();
		resultsTable.setModel(tab);
		//final ChartDisplayFisher cd = new ChartDisplayFisher(selectedP, this.otherValues, this.sampleValues, this.network);
		tableHeader = resultsTable.getTableHeader();
		tableHeader.addMouseListener(this);
		viewer = new JScrollPane(resultsTable);
	}
	
	public void redoLrTable(){
		String[] c = {"Seed", "Genes", "Log-Rank P-Value", "Empirical FDR P-value", "Classification"};
		Model tab = new Model(c);
		addToTable = new ArrayList<String[]>();
		
		for (String key : allResults.keySet()){
			for (ArrayList<HashMap<String,Double>> set : allResults.get(key).keySet()){
				for (String genes : set.get(0).keySet()){
					String[] newEntry = new String[5];
					newEntry[0]=key;
					newEntry[1] = genes;
					newEntry[2] = String.valueOf(set.get(0).get(genes));
					//newEntry[2]=String.valueOf((double)Math.round(set.get(0).get(genes)* 100000) / 100000);
					Double b = set.get(1).get(genes);
					if (b!=null){
						//b = (double)Math.round(b * 100000) / 100000;
					}
					newEntry[3]=String.valueOf(b);
					
					if (set.get(2).get(genes)==1){
						newEntry[4] = "HIGH";
					}
					else if (set.get(2).get(genes)==0){
						newEntry[4] = "LOW";
					}
					else{
						newEntry[4] = "NA";
					}
					
					if (b!=null){
						if (Double.valueOf(newEntry[2])<=this.pValueCutoff && Double.valueOf(newEntry[3])<=this.pValueCutoff){
							addToTable.add(newEntry);
						}
					}
					else{
						if (Double.valueOf(newEntry[2])<=this.pValueCutoff){
							addToTable.add(newEntry);
						}
					}
				}
			}
		}
		tab.AddCSVData(addToTable);
		resultsTable = new JTable();
		resultsTable.setModel(tab);
		//final ChartDisplay cd = new ChartDisplay(this.clinicalValues, this.sampleValues, this.network);
		tableHeader = resultsTable.getTableHeader();
		tableHeader.addMouseListener(this);
		viewer.setViewportView(resultsTable);
		/*
		resultsTable.getTableHeader().addMouseListener(new MouseAdapter() {
		    @Override
		    public void mouseClicked(MouseEvent e) {
		        int col = resultsTable.columnAtPoint(e.getPoint());
		        String name = resultsTable.getColumnName(col);
		        System.out.println(col);
		        sortTable(col, addToTable);
		    }
		});
		*/
	}
	
	
	public void setUpTableLogRank(){
		String[] c = {"Seed", "Genes", "Log-Rank P-Value", "Empirical FDR P-value", "Classification"};
		Model tab = new Model(c);
		this.sas = new String[2];
		sas[0] = "none";
		addToTable = new ArrayList<String[]>();
		
		for (String key : allResults.keySet()){
			for (ArrayList<HashMap<String,Double>> set : allResults.get(key).keySet()){
				for (String genes : set.get(0).keySet()){
					String[] newEntry = new String[5];
					newEntry[0]=key;
					newEntry[1] = genes;
					newEntry[2] = String.valueOf(set.get(0).get(genes));
					//newEntry[2]=String.valueOf((double)Math.round(set.get(0).get(genes)* 100000) / 100000);
					Double b = set.get(1).get(genes);
					if (b!=null){
						//b = (double)Math.round(b * 100000) / 100000;
					}
					newEntry[3]=String.valueOf(b);
					
						if (set.get(2).get(genes)==1){
							newEntry[4] = "HIGH";
						}
						else if (set.get(2).get(genes)==0){
							newEntry[4] = "LOW";
						}
						else{
							newEntry[4] = "NA";
						}

					if (b!=null){
						if (Double.valueOf(newEntry[2])<=this.pValueCutoff && Double.valueOf(newEntry[3])<=this.pValueCutoff){
							addToTable.add(newEntry);
						}
					}
					else{
						if (Double.valueOf(newEntry[2])<=this.pValueCutoff){
							addToTable.add(newEntry);
						}
					}
				}
			}
		}
		tab.AddCSVData(addToTable);
		resultsTable = new JTable();
		resultsTable.setModel(tab);
		//final ChartDisplay cd = new ChartDisplay(this.clinicalValues, this.sampleValues, this.network);
		tableHeader = resultsTable.getTableHeader();
		tableHeader.addMouseListener(this);
		/*
		resultsTable.addMouseListener(new MouseAdapter() {
			  public void mouseClicked(MouseEvent e) {
			      JTable target = (JTable)e.getSource();
			      int row = target.getSelectedRow();
			      sas = new String[2];
			      sas[0] = addToTable.get(row)[0];
			      sas[1] = addToTable.get(row)[1];
			      selectedP = Double.valueOf(addToTable.get(row)[2]);
			      if (e.getClickCount() == 2) {
			    	  if (!addToTable.get(row)[1].equals("none")){
			    	  cd.display(addToTable.get(row)[1]);
			      }
			    }
			  }
			});
		*/
		viewer = new JScrollPane(resultsTable);
		//viewer.setViewportView(resultsTable);
		
		/*
		resultsTable.getTableHeader().addMouseListener(new MouseAdapter() {
		    @Override
		    public void mouseClicked(MouseEvent e) {
		        int col = resultsTable.columnAtPoint(e.getPoint());
		        String name = resultsTable.getColumnName(col);
		        System.out.println(col);
		        sortTable(col, addToTable);
		       
		    }
		});
		*/

	}
	
	
	/**
	 * set layout
	 */
	public void makeLayout(){
		this.setPreferredSize(new Dimension(500, 450));
		add(viewer);
		viewer.setPreferredSize(new Dimension(400, 225));
		add(buttonPanel);
		add(panel3);
	}
	
	/**
	 * We look at all the data and take all modules with statistical test pValue less than 0.05 
	 * AND also FDR permutation pValue of less than 0.05 - these are the most correlated modules that
	 * are also validated
	 * @return an arraylist of map of most correlated module in string form to the pValue of that module
	 */
	public ArrayList<HashMap<String, Double>> extractMostCorrelated(){
		ArrayList<HashMap<String, Double>> rt = new ArrayList<HashMap<String, Double>>();
		HashMap<String, Double> mostCorrelated = new HashMap<String, Double>();
		HashMap<String, Double> mostCorrelatedFDR = new HashMap<String, Double>();
		
		for (String s : allResults.keySet()){
			for (ArrayList<HashMap<String, Double>> ahhs : allResults.get(s).keySet()){
				HashMap<String, Double> original = ahhs.get(0);
				HashMap<String, Double> adjusted = ahhs.get(1);
				for (String set : original.keySet()){
					if (adjusted.containsKey(set)){
						if (original.get(set)<this.pValueCutoff && adjusted.get(set)<this.pValueCutoff){
							mostCorrelated.put(set, original.get(set));
							mostCorrelatedFDR.put(set, adjusted.get(set));
						}
					}
				}
			}
		}
		
		rt.add(mostCorrelated);
		rt.add(mostCorrelatedFDR);
		
		return rt;
		
	}
	
	/**
	 * We find the most correlated modules, and find which seed that module was expanded from 
	 * (to visualize the network)
	 * @return HashMap<seedName, moduleString>
	 */
	public HashMap<String, String> seedAndString(){
		HashMap<String, String> hss = new HashMap<String, String>();
		for (String s : allResults.keySet()){
			for (ArrayList<HashMap<String, Double>> ahhs : allResults.get(s).keySet()){
				HashMap<String, Double> original = ahhs.get(0);
				HashMap<String, Double> adjusted = ahhs.get(1);
				for (String set : original.keySet()){
					if (adjusted.containsKey(set)){
						if (original.get(set)<this.pValueCutoff && adjusted.get(set)<this.pValueCutoff){
							hss.put(s, set);
						}
					}
				}
			}
		}
		
		return hss;
		
	}
	
	
	/**
	 * export the most correlated data into a text file
	 */
	public void exportMostCorrelated(){
		
		ArrayList<HashMap<String, Double>> a = extractMostCorrelated();
		
		HashMap<String, Double> mostCorrelated = a.get(0);
		HashMap<String, Double> mostCorrelatedFDR = a.get(1);
		
		final String lineSep = System.getProperty("line.separator");
		String fileName = null;
		FileWriter fout = null;

		try {
			File file = utils.fileUtil.getFile(utils.swingApp.getJFrame(), "Export Most Correlated Results in CSV File", FileUtil.SAVE, getFilters());
			
			
			if (file!=null){
				fileName = file.getAbsolutePath();
				if (!fileName.substring(fileName.length()-4,fileName.length()).equals(".csv")){
					fileName = fileName + ".csv";
				}
				fout = new FileWriter(fileName);
				
				
				fout.write("Module" + ',' + "Statistical Test P-Value" + ',' + "FDR P-Value" + lineSep);
				for (String s : mostCorrelated.keySet()){
					fout.write(s + ',' + mostCorrelated.get(s) + ',' + mostCorrelatedFDR.get(s) + lineSep);
				}
				fout.write(lineSep);
			
				fout.write("HyperModules Results" + lineSep);
				fout.write("Date: " + ',' + DateFormat.getDateTimeInstance().format(new Date()) + lineSep + lineSep);
				
				fout.write("Length Option: " + ','+ parameters.get("length") + lineSep);
				fout.write("Expand Option: " + ',' + parameters.get("expand") + lineSep);
				fout.write("Shuffle Number: "+ ',' + parameters.get("nShuffled") + lineSep);
				fout.write("Statistical Test: " + ','+ parameters.get("stat") + lineSep + lineSep);

			}
		} 
		catch (IOException e) {
			JOptionPane.showMessageDialog(null,
										  e.toString(),
										  "Error Writing to \"" + fileName + "\"",
										  JOptionPane.ERROR_MESSAGE);
		} 
		finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	/**
	 * old export correlated method definition - this exports all results sorted based on the pValue 
	 * (lowest pValue first)
	 */
	public void exportCorrelatedNetworks(){
		
		Multimap<Double, String> newMultimap = ArrayListMultimap.create();
		ArrayList<Double> toSort = new ArrayList<Double>();
		
		for (String s : allResults.keySet()){
			for (ArrayList<HashMap<String, Double>> ahhs : allResults.get(s).keySet()) {
				HashMap<String, Double> original = ahhs.get(0);
				for (String set : original.keySet()){
					newMultimap.put(original.get(set), set);
					toSort.add(original.get(set));
				}
			}
		}
		
		Collections.sort(toSort);
		
		Multimap<Double, String> newMultimap2 = ArrayListMultimap.create();
		ArrayList<Double> toSort2 = new ArrayList<Double>();
		
		for (String s : allResults.keySet()){
			for (ArrayList<HashMap<String, Double>> ahhs : allResults.get(s).keySet()) {
				HashMap<String, Double> original = ahhs.get(1);
				for (String set : original.keySet()){
					newMultimap2.put(original.get(set), set);
					toSort2.add(original.get(set));
				}
			}
		}
		
		Collections.sort(toSort2);
		//System.out.println("hello");

		final String lineSep = System.getProperty("line.separator");
		String fileName = null;
		FileWriter fout = null;

		try {
			File file = utils.fileUtil.getFile(utils.swingApp.getJFrame(), "Export Most Correlated Results (Sorted) in Text File", FileUtil.SAVE, getFilters());

			if (file!=null){
				fileName = file.getAbsolutePath();
				fout = new FileWriter(file);
				fout.write("HyperModules Results" + lineSep);
				fout.write("Date: " + DateFormat.getDateTimeInstance().format(new Date()) + lineSep + lineSep);
				
				fout.write("Sorted Adjusted Results:" + lineSep);
				for (Double d : toSort2){
					for (String hs : newMultimap2.get(d)){
						if (d!=1.0){
							fout.write(hs + " - " + d + lineSep);
						}
					}
				}
				
				
				fout.write("Sorted Original Results:" + lineSep);
				for (Double d : toSort){
					for (String hs : newMultimap.get(d)){
						if (d!=1.0){
							fout.write(hs + " - " + d + lineSep);
						}
					}
				}
			}
		} 
		catch (IOException e) {
			JOptionPane.showMessageDialog(null,
										  e.toString(),
										  "Error Writing to \"" + fileName + "\"",
										  JOptionPane.ERROR_MESSAGE);
		} 
		finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * exports all hypermodules algorithm results, including the random permutation data, the original
	 * test results, and the FDR test results.
	 */
	public void exportResults(){
		//System.out.println("hello");

		final String lineSep = System.getProperty("line.separator");
		String fileName = null;
		FileWriter fout = null;

		try {
			File file = utils.fileUtil.getFile(utils.swingApp.getJFrame(), "Export Results in Text File", FileUtil.SAVE, getFilters());

			if (file!=null){
				fileName = file.getAbsolutePath();
				fout = new FileWriter(file);
				fout.write("HyperModules Results" + lineSep);
				fout.write("Date: " + DateFormat.getDateTimeInstance().format(new Date()) + lineSep + lineSep);
				for (String key : allResults.keySet()){
					fout.write("seed: " + key + lineSep + lineSep);
					fout.write("True Results: " + lineSep);
					for (ArrayList<HashMap<String, Double>> conv : allResults.get(key).keySet()){
						for (String nodes : conv.get(0).keySet()){
							fout.write(nodes + "\t" + conv.get(0).get(nodes) + lineSep);
						}
					}

					fout.write(lineSep);
					fout.write("AdjustedResults: " + lineSep);
					for (ArrayList<HashMap<String, Double>> conv : allResults.get(key).keySet()){
						for (String nodes : conv.get(1).keySet()){
							fout.write(nodes + "\t" + conv.get(1).get(nodes) + lineSep);
						}
					}
					fout.write(lineSep);
					fout.write("Random Results: " + lineSep);
					for (ArrayList<HashMap<String, Double>> conv : allResults.get(key).keySet()){
						for (String nodes : allResults.get(key).get(conv).keySet())
							fout.write(nodes + "\t" + allResults.get(key).get(conv).get(nodes)  + lineSep);
					}
					fout.write(lineSep);
				}
			}
		} 
		catch (IOException e) {
			JOptionPane.showMessageDialog(null,
										  e.toString(),
										  "Error Writing to \"" + fileName + "\"",
										  JOptionPane.ERROR_MESSAGE);
		} 
		finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private List<FileChooserFilter> getFilters()
	{
		List<FileChooserFilter> filters = new ArrayList<FileChooserFilter>();
    	filters.add(new FileChooserFilter("Text format", "TXT"));
    	return filters;
	}

	
	
	private static final long serialVersionUID = 1L;

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.EAST;
	}

	@Override
	public Icon getIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTitle() {
		return "HyperModules Results";
	}

	public void redoFisherTable(){
		
		String[] c = {"Seed", "Genes", "Fisher P-Value", "Empirical FDR P-value"};
		Model tab = new Model(c);
		addToTable = new ArrayList<String[]>();
		
		for (String key : allResults.keySet()){
			for (ArrayList<HashMap<String,Double>> set : allResults.get(key).keySet()){
				for (String genes : set.get(0).keySet()){
					String[] newEntry = new String[4];
					newEntry[0]=key;
					newEntry[1] = genes;
					newEntry[2] = String.valueOf(set.get(0).get(genes));
					//newEntry[2]=String.valueOf((double)Math.round(set.get(0).get(genes)* 100000) / 100000);
					Double b = set.get(1).get(genes);
					if (b!=null){
						//b = (double)Math.round(b * 100000) / 100000;
					}
					newEntry[3]=String.valueOf(b);
					
					if (b!=null){
						if (Double.valueOf(newEntry[2])<=this.pValueCutoff && Double.valueOf(newEntry[3])<=this.pValueCutoff){
							addToTable.add(newEntry);
						}
					}
					else{
						if (Double.valueOf(newEntry[2])<=this.pValueCutoff){
							addToTable.add(newEntry);
						}
					}
				}
			}
		}
		tab.AddCSVData(addToTable);
		resultsTable = new JTable();
		resultsTable.setModel(tab);
		//final ChartDisplayFisher cd = new ChartDisplayFisher(selectedP, this.otherValues, this.sampleValues, this.network);

		tableHeader = resultsTable.getTableHeader();
		tableHeader.addMouseListener(this);
		viewer.setViewportView(resultsTable);
	}
	
	@Override
	public void actionPerformed(ActionEvent ae) {
		/*
		if (ae.getSource()==export){
			exportResults();
		}
		*/
		
		if (ae.getSource()==exportMostCorrelated){
			//exportCorrelatedNetworks();
			exportMostCorrelated();
		}
		/*
		resultsTable.addMouseListener(new MouseAdapter() {
			  public void mouseClicked(MouseEvent e) {
			      JTable target = (JTable)e.getSource();
			      int row = target.getSelectedRow();
			      sas = new String[2];
			      sas[0] = addToTable.get(row)[0];
			      sas[1] = addToTable.get(row)[1];
			      selectedP = Double.valueOf(addToTable.get(row)[2]);
			      if (e.getClickCount() == 2) {
			    	  if (!addToTable.get(row)[1].equals("none")){
			    	  cd.display(addToTable.get(row)[1]);
			      }
			    }
			  }
			});
		*/
		if (ae.getSource()==generate){
			int row = resultsTable.getSelectedRow();
			if (row >= 0){
			sas = new String[2];
			sas[0] = addToTable.get(row)[0];
			sas[1] = addToTable.get(row)[1];
			selectedP = Double.valueOf(addToTable.get(row)[2]);
			if (sas[1]!="none"){
				this.utils.taskMgr.execute(new TaskIterator(new GenerateNetworkTask(sas, this.network, utils, sampleValues)));
			}
			}
			else{
				ErrorDialog ed = new ErrorDialog(utils, "Please select one valid network to visualize.");
				ed.setLocationRelativeTo(null);
				ed.setVisible(true);
				System.out.println("Please select a valid network to visualize.");
			}
			
		}
		
		if (ae.getSource()==discard){
			utils.discardResults(this);
		}
		
		if (ae.getSource()==chart){
			int row = resultsTable.getSelectedRow();
			if (row >= 0 ){
				sas = new String[2];
				sas[0] = addToTable.get(row)[0];
				sas[1] = addToTable.get(row)[1];
				selectedP = Double.valueOf(addToTable.get(row)[2]);
				if (sas[1]!="none"){
				if (parameters.get("stat").equals("logRank")){
					ChartDisplay cd = new ChartDisplay(this.clinicalValues, this.sampleValues, this.network);
					cd.display(sas[1]);
				}
				else{ //fisher
					ChartDisplayFisher cdf = new ChartDisplayFisher(selectedP, this.otherValues, this.sampleValues, this.network);
					cdf.display(sas[1]);
				}
				}
			}
			else{
				ErrorDialog ed = new ErrorDialog(utils, "Please select one valid entry to display the chart for.");
				ed.setLocationRelativeTo(null);
				ed.setVisible(true);
				System.out.println("Please select one valid entry to display the chart for.");
				
			}
		}
		
		if (ae.getSource()==setCutoff){
			if (Double.valueOf(cutoff.getText())>1 || Double.valueOf(cutoff.getText())<0){
				System.out.println("Please enter a pValue between 0 and 1.");
			}
			else{
				this.pValueCutoff = Double.valueOf(cutoff.getText());
				System.out.println(this.pValueCutoff);
				if (parameters.get("stat").equals("logRank")){
					redoLrTable();
				}
				else{
					redoFisherTable();
				}
			}
		}
	}
	
	/**
	 * 
	 * Table model for results table
	 * @author alvinleung
	 *
	 */
	  private class Model extends AbstractTableModel {
		 	 
			private static final long serialVersionUID = 1L;
			private String[] columnNames; 
			private ArrayList<String[]> data =  new ArrayList<String[]>();
	    // private Class[] columnTypes = {String.class, String.class, Integer.class, String.class};
			
	     public Model(String[] columns){
	    	 this.columnNames = columns;
	     }
			
	     public void AddCSVData(ArrayList<String[]> DataIn) {
	         this.data = DataIn;
	         this.fireTableDataChanged();
	     }
			
			@Override
			public int getColumnCount() {
				return columnNames.length;
			}

			@Override
			public int getRowCount() {
				return data.size();
			}
			
			@Override
	     public String getColumnName(int col) {
	         return columnNames[col];
	     }

			@Override
			public Object getValueAt(int row, int col) {
				return data.get(row)[col];
			}
	  }

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource()==tableHeader){
			int colNumber = resultsTable.columnAtPoint(e.getPoint());
			sortTable(colNumber);
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

}
