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

/**
 * 
 * the JPanel for displaying and exporting HyperModules results
 * @author alvinleung
 *
 */
public class ResultsPanel extends JPanel implements CytoPanelComponent, ActionListener {
	
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
	
	private ArrayList<String[]> addToTable;
	
	private ArrayList<String[]> newTableAdd;

	private double pValueCutoff;
	
	private String[] sas;
	/**
	 * constructor
	 * @param parameters
	 * @param utils
	 * @param allResults
	 * @param network
	 */
	public ResultsPanel(HashMap<String, String> parameters, CytoscapeUtils utils, HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> allResults, CyNetwork network, ArrayList<String[]> sampleValues, ArrayList<String[]> clinicalValues){
		this.utils = utils;
		this.allResults = allResults;
		this.network = network;
		this.parameters = parameters;
		this.sampleValues = sampleValues;
		this.clinicalValues = clinicalValues;
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
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridBagLayout());
		//buttonPanel.add(export);
		buttonPanel.add(exportMostCorrelated);
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
		setUpTable();

	}
	private void redoTable(){
		Model tab = new Model();
		addToTable = new ArrayList<String[]>();
		
		for (String key : allResults.keySet()){
			for (ArrayList<HashMap<String,Double>> set : allResults.get(key).keySet()){
				for (String genes : set.get(0).keySet()){
					String[] newEntry = new String[5];
					newEntry[0]=key;
					newEntry[1] = genes;
					newEntry[2]=String.valueOf((double)Math.round(set.get(0).get(genes)* 100000) / 100000);
					Double b = set.get(1).get(genes);
					if (b!=null){
						b = (double)Math.round(b * 100000) / 100000;
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
		final ChartDisplay cd = new ChartDisplay(this.clinicalValues, this.sampleValues, this.network);

		resultsTable.addMouseListener(new MouseAdapter() {
			  public void mouseClicked(MouseEvent e) {
				JTable target = (JTable)e.getSource();
				int row = target.getSelectedRow();
			    sas = new String[2];
			    sas[0] = addToTable.get(row)[0];
			    sas[1] = addToTable.get(row)[1];
			    if (e.getClickCount() == 2) {
			      //JTable target = (JTable)e.getSource();
			      //int row = target.getSelectedRow();
			      if (!addToTable.get(row)[1].equals("none")){
			    	  cd.display(addToTable.get(row)[1]);
			      }
			    }
			  }
			});
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
	
	
	private void setUpTable(){
		Model tab = new Model();
		this.sas = new String[2];
		sas[0] = "none";
		addToTable = new ArrayList<String[]>();
		
		for (String key : allResults.keySet()){
			for (ArrayList<HashMap<String,Double>> set : allResults.get(key).keySet()){
				for (String genes : set.get(0).keySet()){
					String[] newEntry = new String[5];
					newEntry[0]=key;
					newEntry[1] = genes;
					newEntry[2]=String.valueOf((double)Math.round(set.get(0).get(genes)* 100000) / 100000);
					Double b = set.get(1).get(genes);
					if (b!=null){
						b = (double)Math.round(b * 100000) / 100000;
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
		final ChartDisplay cd = new ChartDisplay(this.clinicalValues, this.sampleValues, this.network);
		

		resultsTable.addMouseListener(new MouseAdapter() {
			  public void mouseClicked(MouseEvent e) {
			      JTable target = (JTable)e.getSource();
			      int row = target.getSelectedRow();
			      sas = new String[2];
			      sas[0] = addToTable.get(row)[0];
			      sas[1] = addToTable.get(row)[1];
			      if (e.getClickCount() == 2) {
			    	  if (!addToTable.get(row)[1].equals("none")){
			    	  cd.display(addToTable.get(row)[1]);
			      }
			    }
			  }
			});
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

	public void sortTable(int col, ArrayList<String[]> addToTable){
		Model tab = new Model();
		newTableAdd = new ArrayList<String[]>();
		ArrayList<Double> toSort = new ArrayList<Double>();
		if (col==2){
			Multimap<Double, String[]> mds = ArrayListMultimap.create();
			for (int i=0; i<addToTable.size(); i++){
				String[] otherThings = new String[4];
				otherThings[0] = addToTable.get(i)[0];
				otherThings[1] = addToTable.get(i)[1];
				otherThings[2] = addToTable.get(i)[3];
				otherThings[3] = addToTable.get(i)[4];
				if (addToTable.get(i)[2]!=null){
					mds.put(Double.valueOf(addToTable.get(i)[2]), otherThings);
					toSort.add(Double.valueOf(addToTable.get(i)[2]));
				}
			}
			
			Collections.sort(toSort);
			
			for (int i=0; i<toSort.size(); i++){
				for (String[] s : mds.get(toSort.get(i))){
					String[] toAdd = new String[5];
					toAdd[0] = s[0];
					toAdd[1] = s[1];
					toAdd[2] = String.valueOf(toSort.get(i));
					toAdd[3] = s[2];
					toAdd[4] = s[3];
					newTableAdd.add(toAdd);
				}
			}
			tab.AddCSVData(newTableAdd);
			resultsTable = new JTable();
			resultsTable.setModel(tab);
			final ChartDisplay cd = new ChartDisplay(this.clinicalValues, this.sampleValues, this.network);
			
			resultsTable.addMouseListener(new MouseAdapter() {
				  public void mouseClicked(MouseEvent e) {
				      JTable target = (JTable)e.getSource();
				      int row = target.getSelectedRow();
				      sas = new String[2];
				      sas[0] = newTableAdd.get(row)[0];
				      sas[1] = newTableAdd.get(row)[1];
				    if (e.getClickCount() == 2) {
				      if (!newTableAdd.get(row)[1].equals("none")){
				    	  cd.display(newTableAdd.get(row)[1]);
				      }
				    }
				  }
				});
			viewer.setViewportView(resultsTable);

		}
		else if (col==3){
			System.out.println("3");
		}
		else if (col==4){
			System.out.println("4");
		}
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
		
		if (ae.getSource()==generate){
			if (!sas[0].equals("none")){
				this.utils.taskMgr.execute(new TaskIterator(new GenerateNetworkTask(sas, this.network, utils, sampleValues)));
			}
			else{
				System.out.println("Please select a valid network to visualize.");
			}
			
		}
		
		if (ae.getSource()==discard){
			utils.discardResults(this);
		}
		
		if (ae.getSource()==setCutoff){
			if (Double.valueOf(cutoff.getText())>1 || Double.valueOf(cutoff.getText())<0){
				System.out.println("Please enter a pValue between 0 and 1.");
			}
			else{
				this.pValueCutoff = Double.valueOf(cutoff.getText());
				System.out.println(this.pValueCutoff);
				redoTable();
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
			private String[] columnNames = {"Seed", "Genes", "Log-Rank P-Value", "Empirical FDR P-value", "Classification"};
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
