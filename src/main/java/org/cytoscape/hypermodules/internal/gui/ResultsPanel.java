package org.cytoscape.hypermodules.internal.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

public class ResultsPanel extends JPanel implements CytoPanelComponent, ActionListener {
	
	private HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults;
	private JButton export;
	private JButton exportMostCorrelated;
	private JButton generate;
	private JButton discard;
	private CytoscapeUtils utils;
	private JScrollPane viewer;
	private JTable resultsTable;
	private JPanel buttonPanel;
	private CyNetwork network;
	private HashMap<String, String> parameters;
	
	public ResultsPanel(HashMap<String, String> parameters, CytoscapeUtils utils, HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults, CyNetwork network){
		this.utils = utils;
		this.allResults = allResults;
		this.network = network;
		this.parameters = parameters;
		makeComponents();
		makeLayout();

	}

	public void makeComponents(){
		export  = new JButton("export");
		export.addActionListener(this);
		exportMostCorrelated = new JButton("export most correlated");
		exportMostCorrelated.addActionListener(this);
		generate = new JButton("visualize networks");
		generate.addActionListener(this);
		discard = new JButton("discard results");
		discard.addActionListener(this);
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridBagLayout());
		buttonPanel.add(export);
		buttonPanel.add(exportMostCorrelated);
		buttonPanel.add(generate);
		buttonPanel.add(discard);
		Model tab = new Model();
		ArrayList<String[]> addToTable = new ArrayList<String[]>();
		
		for (String key : allResults.keySet()){
			for (ArrayList<HashMap<String,Double>> set : allResults.get(key).keySet()){
				for (String genes : set.get(0).keySet()){
					String[] newEntry = new String[5];
					newEntry[0]=key;
					newEntry[1] = genes;
					newEntry[2]=String.valueOf(set.get(0).get(genes));
					newEntry[3]=String.valueOf(set.get(1).get(genes));
					if (set.get(2).get(genes)==1){
						newEntry[4] = "HIGH";
					}
					else if (set.get(2).get(genes)==0){
						newEntry[4] = "LOW";
					}
					else{
						newEntry[4] = "NA";
					}
					addToTable.add(newEntry);
				}
			}
		}
		tab.AddCSVData(addToTable);
		resultsTable = new JTable(tab);
		viewer = new JScrollPane(resultsTable);
		
	}

	public void makeLayout(){
		this.setPreferredSize(new Dimension(500, 350));
		add(viewer);
		viewer.setPreferredSize(new Dimension(450, 300));
		add(buttonPanel);
	}
	
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
						if (original.get(set)<0.05 && adjusted.get(set)<0.05){
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
	
	public HashMap<String, String> seedAndString(){
		HashMap<String, String> hss = new HashMap<String, String>();
		for (String s : allResults.keySet()){
			for (ArrayList<HashMap<String, Double>> ahhs : allResults.get(s).keySet()){
				HashMap<String, Double> original = ahhs.get(0);
				HashMap<String, Double> adjusted = ahhs.get(1);
				for (String set : original.keySet()){
					if (adjusted.containsKey(set)){
						if (original.get(set)<0.05 && adjusted.get(set)<0.05){
							hss.put(s, set);
						}
					}
				}
			}
		}
		
		return hss;
		
	}
	
	
	
	public void exportMostCorrelated(){
		
		ArrayList<HashMap<String, Double>> a = extractMostCorrelated();
		
		HashMap<String, Double> mostCorrelated = a.get(0);
		HashMap<String, Double> mostCorrelatedFDR = a.get(1);
		
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
				
				fout.write("Length Option: " + parameters.get("length") + lineSep);
				fout.write("Expand Option: " + parameters.get("expand") + lineSep);
				fout.write("Shuffle Number: " + parameters.get("nShuffled") + lineSep);
				fout.write("Statistical Test: " + parameters.get("stat") + lineSep + lineSep);
				
				fout.write("Most Correlated Results:" + lineSep + lineSep);
				
				for (String s : mostCorrelated.keySet()){
					fout.write(s + "-" + mostCorrelated.get(s) + "-" + mostCorrelatedFDR.get(s) + lineSep);
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
		System.out.println("hello");

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

	public void exportResults(){
		System.out.println("hello");

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
							fout.write(nodes + " - " + conv.get(0).get(nodes) + lineSep);
						}
					}

					fout.write(lineSep);
					fout.write("AdjustedResults: " + lineSep);
					for (ArrayList<HashMap<String, Double>> conv : allResults.get(key).keySet()){
						for (String nodes : conv.get(1).keySet()){
							fout.write(nodes + " - " + conv.get(1).get(nodes) + lineSep);
						}
					}
					fout.write(lineSep);
					fout.write("Random Results: " + lineSep);
					for (ArrayList<HashMap<String, Double>> conv : allResults.get(key).keySet()){
						for (String nodes : allResults.get(key).get(conv).keySet())
							fout.write(nodes + " - " + allResults.get(key).get(conv).get(nodes)  + lineSep);
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

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource()==export){
			exportResults();
		}
		
		if (ae.getSource()==exportMostCorrelated){
			//exportCorrelatedNetworks();
			exportMostCorrelated();
		}
		
		if (ae.getSource()==generate){
			HashMap<String, String> sas = seedAndString();
			this.utils.taskMgr.execute(new TaskIterator(new GenerateNetworkTask(sas, this.network, utils)));
			
		}
		
		if (ae.getSource()==discard){
			utils.discardResults(this);
		}
	}
	
	  private class Model extends AbstractTableModel {
		 	 
			private static final long serialVersionUID = 1L;
			private String[] columnNames = {"Seed", "Genes", "Real P-Values", "Adjusted P-values", "Classification"};
			private ArrayList<String[]> data =  new ArrayList<String[]>();
	    // private Class[] columnTypes = {String.class, String.class, Integer.class, String.class};
			
	     
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

}
