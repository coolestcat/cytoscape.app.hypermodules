package org.cytoscape.hypermodules.internal.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.hypermodules.internal.task.OpenResultsTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;

import com.google.common.collect.Multimap;

public class LoadResultsPanel extends JDialog implements ActionListener{
	
	private ArrayList<String[]> loadedResults;
	
	private HashMap<String, String> parameters;
	private ArrayList<String[]> clinicalValues;
	private ArrayList<String[]> sampleValues;
	private ArrayList<String[]> otherValues;
	private CyNetwork selectedNetwork;
	
	private CytoscapeUtils utils;
	private NetworkSelectionPanel netSelect;
	private JButton loadResultsButton;
	private JButton loadMutationsButton;
	private JButton loadClinicalsButton;
	private JButton loadButton;

	private static final long serialVersionUID = 1L;
	
	public LoadResultsPanel(CytoscapeUtils utils){
		super(utils.swingApp.getJFrame(), "Load Results", false);
		
		this.utils = utils;
		this.netSelect = new NetworkSelectionPanel(utils);
		getContentPane().add(netSelect, BorderLayout.NORTH);
		getContentPane().add(getButtonPanel(), BorderLayout.CENTER);

		this.netSelect.setBorder(BorderFactory.createTitledBorder("Select Network"));

		setResizable(false);
		pack();
	}
	

	public JPanel getButtonPanel(){
		JPanel buttonPanel = new JPanel();
		this.loadResultsButton = new JButton("Load Hypermodules Results File");
		this.loadMutationsButton = new JButton("Load Mutations File");
		this.loadClinicalsButton = new JButton("Load Clinical Data File");
		this.loadButton = new JButton("Make Panel");
		
		loadResultsButton.addActionListener(this);
		loadMutationsButton.addActionListener(this);
		loadClinicalsButton.addActionListener(this);
		loadButton.addActionListener(this);
		buttonPanel.setLayout(new GridLayout(4,1));
		buttonPanel.add(loadResultsButton);
		buttonPanel.add(loadMutationsButton);
		buttonPanel.add(loadClinicalsButton);
		buttonPanel.add(loadButton);
		
		loadResultsButton.setAlignmentX(CENTER_ALIGNMENT);
		loadMutationsButton.setAlignmentX(CENTER_ALIGNMENT);
		loadClinicalsButton.setAlignmentX(CENTER_ALIGNMENT);
		loadButton.setAlignmentX(CENTER_ALIGNMENT);


		return buttonPanel;
	}
	
	private List<FileChooserFilter> getFilters()
	{
		List<FileChooserFilter> filters = new ArrayList<FileChooserFilter>();
    	filters.add(new FileChooserFilter("CSV", "csv"));
    	filters.add(new FileChooserFilter("MAF", "maf"));
    	filters.add(new FileChooserFilter("MAF.TXT", "maf.txt"));
    	filters.add(new FileChooserFilter("TXT", "txt"));
    	filters.add(new FileChooserFilter("TSV", "tsv"));
    	return filters;
	}
	
	public HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> makeAllResults(){
		HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>> ar = new HashMap<String, HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>>>();
		
		int start = 0;
		if (parameters.get("stat").equals("logRank")){
			start = 9;
		}
		else{
			start = 11;
		}
		
		HashSet<String> allSeeds = new HashSet<String>();
		for (int i=start; i<loadedResults.size(); i++){
			allSeeds.add(loadedResults.get(i)[0]);
		}
		
		for (String s : allSeeds){
			HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>> toPut = new HashMap<ArrayList<HashMap<String, Double>>, Multimap<String, Double>> ();
			ArrayList<HashMap<String, Double>> ahsd = new ArrayList<HashMap<String, Double>>();
			HashMap<String, Double> orig = new HashMap<String, Double>();
			HashMap<String, Double> adj = new HashMap<String, Double>();
			HashMap<String, Double> patn = new HashMap<String, Double>();
			HashMap<String, Double> oddsratio = new HashMap<String, Double>();
			for (int i=start; i<loadedResults.size(); i++){
				if (loadedResults.get(i)[0].equals(s)){
					String t = loadedResults.get(i)[1];
					orig.put(t, Double.valueOf(loadedResults.get(i)[2]));
					adj.put(t, Double.valueOf(loadedResults.get(i)[3]));
					patn.put(t, Double.valueOf(loadedResults.get(i)[4]));
					oddsratio.put(t, Double.valueOf(loadedResults.get(i)[5]));
				}
			}
			ahsd.add(orig);
			ahsd.add(adj);
			ahsd.add(patn);
			ahsd.add(oddsratio);
			toPut.put(ahsd, null);
			ar.put(s, toPut);
		}
		
		return ar;
	}
	
	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() == loadResultsButton){
			File DataFile = utils.fileUtil.getFile(utils.swingApp.getJFrame(), "Load HyperModules Result Data", FileUtil.LOAD, getFilters());
			if (DataFile!=null){
				CSVFile c = new CSVFile();
				loadedResults = c.ReadCSVfile(DataFile);
			}
		}
		if (ae.getSource() == loadMutationsButton){
			File DataFile = utils.fileUtil.getFile(utils.swingApp.getJFrame(), "Load Mutation Data", FileUtil.LOAD, getFilters());
			if (DataFile!=null){
				CSVFile c = new CSVFile();
				sampleValues = c.ReadCSVfile(DataFile);
			}
		}
		if (ae.getSource() == loadClinicalsButton){
			File DataFile = utils.fileUtil.getFile(utils.swingApp.getJFrame(), "Load Clinical Data", FileUtil.LOAD, getFilters());
			if (DataFile!=null){
				CSVFile c = new CSVFile();
				clinicalValues = c.ReadCSVfile(DataFile);
				c = new CSVFile();
				otherValues = c.ReadCSVfile(DataFile);
			}
		}
		if (ae.getSource() == loadButton){
			//System.out.println("Making Panel!");
			selectedNetwork = netSelect.getSelectedNetwork();
			this.parameters = new HashMap<String, String>();
			parameters.put("length", loadedResults.get(3)[1]);
			parameters.put("expand", loadedResults.get(4)[1]);
			parameters.put("nShuffled", loadedResults.get(5)[1]);
			parameters.put("stat", loadedResults.get(6)[1]);
			if (loadedResults.get(6)[1].equals("logRank")){
				parameters.put("foregroundvariable", null);
			}
			else{
				parameters.put("foregroundvariable", loadedResults.get(7)[1]);
			}
			OpenResultsTaskFactory resultsTaskFac = new OpenResultsTaskFactory(parameters, utils, makeAllResults(), selectedNetwork, this.sampleValues, this.clinicalValues, this.otherValues);
			utils.taskMgr.execute(resultsTaskFac.createTaskIterator());
		}
	}
	

	public class CSVFile {
	     private ArrayList<String[]> Rs = new ArrayList<String[]>();
	     private String[] OneRow;

	        public ArrayList<String[]> ReadCSVfile (File DataFile) {
	            try {
	            BufferedReader brd = new BufferedReader (new FileReader(DataFile));

	            String st = brd.readLine();
	            
	            	while (st!=null) {
	            			OneRow = st.split(",|\t");
	            			if (OneRow.length<2){
	            				String[] newRow = new String[2];
	            				for (int y = 0; y<OneRow.length; y++){
	            					newRow[y] = OneRow[y];
	            				}
	            				for (int y =0; y<newRow.length; y++){
	            					if (newRow[y]==null){
	            						newRow[y] = " ";
	            					}
	            				}
	            				OneRow = newRow;
	            			}
	            			Rs.add(OneRow);
	            			st = brd.readLine();
	                } 
	            } 
	            catch (Exception e) {
	                String errmsg = e.getMessage();
	                System.out.println ("File not found:" +errmsg);
	            }     
	           //System.out.println("RSsize: " + Rs.size());
	        return Rs;
	        }
	 }
}
