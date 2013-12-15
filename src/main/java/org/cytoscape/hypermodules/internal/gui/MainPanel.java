package org.cytoscape.hypermodules.internal.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.hypermodules.internal.gui.NewMainPanel.CSVFile;
import org.cytoscape.hypermodules.internal.task.AlgorithmTask;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.NetworkDestroyedListener;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;

public class MainPanel extends JPanel implements CytoPanelComponent, ActionListener, MouseListener{
	/**
	 * cytoscape utilities
	 */
	private CytoscapeUtils utils;
	/**
	 * swingApp
	 */
	private CySwingApplication swingApp;
	/**
	 * serialization ID
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * network selection panel
	 */
	private NetworkSelectionPanel netSelect;
	/**
	 * main panel - these panel names are pretty self explanatory
	 */
	private JPanel mainPanel;
	
				private JPanel expandOptionPanel;
				private JPanel testPanel;
				
		private JPanel shufflePanel;
		
		private JPanel samplePanel;
		private JScrollPane samplePanelScrollPane;
			private CollapsiblePanel loadSamplePanel;
			
		private JPanel clinicalPanel;
		private JScrollPane clinicalPanelScrollPane;
			private CollapsiblePanel loadClinicalPanel;
	/**
	 * run panel
	 */
	private JPanel runPanel;
	/**
	 * expand from selected seeds
	 */
	private JComboBox expandComboBox;
	
	private JRadioButton expand;
	/**
	 * find most correlated modules among all seeds
	 */
	private JRadioButton findMost;
	/**
	 * expand options group
	 */
	private ButtonGroup options;
	/**
	 * log rank test for comparing survival curves
	 */
	private JRadioButton logRank;
	/**
	 * fisher's test (Size 2xC) for discrete clinical variable
	 */
	private JRadioButton fisher;
	/**
	 * statistical test option group
	 */
	private ButtonGroup statTest;
	/**
	 * "shuffle"
	 */
	private JLabel shuffle;
	/**
	 * number of times to shuffle for FDR permutation validation
	 */
	private JTextField nShuffled;
	/**
	 * whether or not input file has headers
	 */
	private JCheckBox headers;
	private JCheckBox clinicalheaders;
	/**
	 * load gene-sample association - we have a jscrollpane to view data
	 */
	private JButton loadSamples;
		private JTable allGeneSamples;
		private JScrollPane sampleScrollPane;
	/**
	 * load clinical survival data
	 */
	private JButton loadClinicalData;
		private JTable clinicalTable;
		private JScrollPane clinicalScrollPane;
	/**
	 * run algorithm button
	 */
	private JButton run;
	/**
	 * gene-sample associations
	 */
	private ArrayList<String[]> genes2samplesvalues;
	/**
	 * clinical survival data
	 */
	private ArrayList<String[]> clinicalValues;
	/**
	 * clinical variable data
	 */
	private ArrayList<String[]> otherValues;
	
	private ArrayList<String[]> allClinicalData;
	
	private String[] header;
	private String[] clinicalheader;
	
	private HashMap<String, Integer> columnIndices;
	
	
	private JComboBox lrpatients;
	private JComboBox lrdaysfollowup;
	private JComboBox lrvitalstatus;
	
	private JComboBox fpatients;
	private JComboBox fvariable;
	
	private int state;
	private ArrayList<String[]> newGeneTable;
	private ArrayList<String[]> genes2samplesvaluescopy;
	//private JButton sort;
	
	
	/**
	 * constructor
	 * @param swingApp
	 * @param utils
	 */
	public MainPanel(CySwingApplication swingApp, CytoscapeUtils utils){
		this.utils = utils;
		this.swingApp = swingApp;
		this.genes2samplesvalues = null;
		this.clinicalValues = null;
		makeComponents();		
		makeLayout();
		state = 0;
	}
	

	public void makeLayout(){
		
		setLayout(new BorderLayout());
		add(netSelect, BorderLayout.NORTH);
		this.netSelect.setBorder(BorderFactory.createTitledBorder("Select Network"));
		netSelect.setPreferredSize(new Dimension(350, 70));
		
		add(mainPanel, BorderLayout.CENTER);
		expandOptionPanel.setLayout(new BoxLayout(expandOptionPanel, BoxLayout.Y_AXIS));
		expandOptionPanel.setBorder(BorderFactory.createTitledBorder("Expand Option:"));
		expandOptionPanel.setMinimumSize(new Dimension(350, 75));
		expandOptionPanel.setMaximumSize(new Dimension(350, 75));
		expandOptionPanel.setPreferredSize(new Dimension(350, 75));
		expandOptionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		findMost.setSelected(true);

		testPanel.setLayout(new BoxLayout(testPanel, BoxLayout.Y_AXIS));
		testPanel.setBorder(BorderFactory.createTitledBorder("Analysis Type:"));
		testPanel.setPreferredSize(new Dimension(350, 75));
		testPanel.setMinimumSize(new Dimension(350, 75));
		testPanel.setMaximumSize(new Dimension(350, 75));
		testPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		

		shufflePanel.setLayout(new BoxLayout(shufflePanel, BoxLayout.X_AXIS));
		
		
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		logRank.setSelected(true);
		
		add(runPanel, BorderLayout.SOUTH);
		runPanel.setLayout(new GridBagLayout());
	} 
	
	public void makeComponents(){	
		
		netSelect = new NetworkSelectionPanel(this.utils);
		
		//register network listeners:
		utils.serviceRegistrar.registerService(netSelect, NetworkAddedListener.class, new Properties());
		utils.serviceRegistrar.registerService(netSelect, NetworkDestroyedListener.class, new Properties());
		
		mainPanel = new JPanel();

		expandOptionPanel = new JPanel();
		expandComboBox = new JComboBox();
		expandComboBox.addItem("Expand From All Seeds");
		expandComboBox.addItem("Expand From Selected Seeds");
		expandComboBox.setSelectedItem("Expand From All Seeds");
		expandOptionPanel.add(expandComboBox, BorderLayout.CENTER);
		
		
		expand = new JRadioButton("Expand from Selected Seeds");
		findMost = new JRadioButton("Find Most Correlated Module");
		options = new ButtonGroup();
		options.add(expand);
		options.add(findMost);
		//expandOptionPanel.add(expand);
		//expandOptionPanel.add(findMost);
		
		testPanel = new JPanel();
		logRank = new JRadioButton("Survival (Log Rank Test)");
		fisher = new JRadioButton("Categorical Variable (Fisher's Test)");
		logRank.addActionListener(this);
		fisher.addActionListener(this);
		statTest = new ButtonGroup();
		statTest.add(logRank);
		statTest.add(fisher);
		testPanel.add(logRank);
		testPanel.add(fisher);
		
		shufflePanel = new JPanel();
		shuffle = new JLabel("Shuffle Number:");
		nShuffled = new JTextField("100", 5);
		shufflePanel.add(shuffle, BorderLayout.WEST);
		shufflePanel.add(nShuffled, BorderLayout.EAST);
		shufflePanel.setPreferredSize(new Dimension(350, 40));
		shufflePanel.setMinimumSize(new Dimension(350, 40));
		shufflePanel.setMaximumSize(new Dimension(350, 40));
		shufflePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		mainPanel.add(expandOptionPanel);
		mainPanel.add(testPanel);
		mainPanel.add(shufflePanel);
		

		samplePanelScrollPane = new JScrollPane();
		samplePanel = new JPanel();
		samplePanel.setLayout(new BoxLayout(samplePanel, BoxLayout.Y_AXIS));
		loadSamples = new JButton("Load Mutation Data");
		loadSamples.addActionListener(this);
		loadSamples.setPreferredSize(new Dimension(150, 23));
		
		JPanel buttonheader = new JPanel();
		buttonheader.setLayout(new GridLayout(1,3));
		buttonheader.setPreferredSize( new Dimension (300, 30));
		buttonheader.setMaximumSize( new Dimension (300, 30));
		
		//samplePanel.add(loadSamples);
		headers = new JCheckBox("CSV has Headers");
		//samplePanel.add(headers);
		//headers.setSelected(true);
		headers.addActionListener(this);
		
		//sort = new JButton("sort");
		//sort.addActionListener(this);
		buttonheader.add(loadSamples);
		buttonheader.add(headers);
		//buttonheader.add(sort);

		samplePanel.add(buttonheader);
		loadSamplePanel = new CollapsiblePanel("Samples");
		loadSamplePanel.setPreferredSize(new Dimension(3000, 280));
		loadSamplePanel.setMaximumSize(new Dimension(3000, 280));
		samplePanel.setPreferredSize(new Dimension(300, 300));
		samplePanel.setMaximumSize(new Dimension(300, 300));
		samplePanel.add(loadSamplePanel);
		
		samplePanelScrollPane.setViewportView(samplePanel);
		samplePanelScrollPane.setPreferredSize(new Dimension(350, 150));
		
		sampleScrollPane = new JScrollPane();
		sampleScrollPane.setPreferredSize(new Dimension(300, 215));
		sampleScrollPane.setMaximumSize(new Dimension(300, 215));
		loadSamplePanel.getContentPane().add(sampleScrollPane, BorderLayout.NORTH);
		resetSamplePanel(null);
		

		clinicalPanelScrollPane = new JScrollPane();
		
		clinicalPanel = new JPanel();
		clinicalPanel.setLayout(new BoxLayout(clinicalPanel, BoxLayout.Y_AXIS));
		
		JPanel headerplusbutton = new JPanel();
		headerplusbutton.setLayout(new GridLayout(1,2));
		headerplusbutton.setPreferredSize(new Dimension(300, 30));
		headerplusbutton.setMaximumSize(new Dimension(300, 30));
		
		
		loadClinicalData = new JButton("Load Clinical Data");
		loadClinicalData.addActionListener(this);
		loadClinicalData.setPreferredSize(new Dimension(150, 23));
		//clinicalPanel.add(loadClinicalData);
		clinicalheaders = new JCheckBox("CSV has Headers");
		//clinicalPanel.add(clinicalheaders);
		//clinicalheaders.setSelected(true);
		clinicalheaders.addActionListener(this);
		
		headerplusbutton.add(loadClinicalData);
		headerplusbutton.add(clinicalheaders);
		
		clinicalPanel.add(headerplusbutton);
		
		JPanel dds = new JPanel();
		dds.setLayout(new GridLayout(3, 2));
		
		
		
		JLabel lrpatientlabel = new JLabel("patients: ");
		//clinicalPanel.add(lrpatientlabel, BorderLayout.WEST);
		lrpatients = new JComboBox();
		//clinicalPanel.add(lrpatients, BorderLayout.EAST);
		lrpatients.setPreferredSize(new Dimension(150, 23));
		lrpatients.setMaximumSize(new Dimension(150, 23));
		
		JLabel lrvitalstatuslabel = new JLabel("vital status: ");
		//clinicalPanel.add(lrvitalstatuslabel, BorderLayout.WEST);
		lrvitalstatus = new JComboBox();
		//clinicalPanel.add(lrvitalstatus, BorderLayout.EAST);
		lrvitalstatus.setPreferredSize(new Dimension(150, 23));
		lrvitalstatus.setMaximumSize(new Dimension(150, 23));
		
		JLabel lrdflabel = new JLabel("followup times: ");
		//clinicalPanel.add(lrdflabel, BorderLayout.WEST);
		lrdaysfollowup = new JComboBox();
		//clinicalPanel.add(lrdaysfollowup, BorderLayout.EAST);
		lrdaysfollowup.setPreferredSize(new Dimension(150, 23));
		lrdaysfollowup.setMaximumSize(new Dimension(150, 23));
		

		dds.add(lrpatientlabel);
		dds.add(lrpatients);
		dds.add(lrvitalstatuslabel);
		dds.add(lrvitalstatus);
		dds.add(lrdflabel);
		dds.add(lrdaysfollowup);
		dds.setPreferredSize(new Dimension(300, 90));
		dds.setMaximumSize(new Dimension(300, 90));
		
		clinicalPanel.add(dds);
		
		loadClinicalPanel = new CollapsiblePanel("Clinical Data");
		clinicalScrollPane = new JScrollPane();
		clinicalScrollPane.setPreferredSize(new Dimension(300, 240));
		clinicalScrollPane.setMaximumSize(new Dimension(300, 240));
		loadClinicalPanel.getContentPane().add(clinicalScrollPane, BorderLayout.NORTH);
		loadClinicalPanel.setPreferredSize(new Dimension(3000, 280));
		loadClinicalPanel.setMaximumSize(new Dimension(3000, 280));
		clinicalPanel.setPreferredSize(new Dimension(300, 420));
		clinicalPanel.setMaximumSize(new Dimension(300, 420));
		clinicalPanel.add(loadClinicalPanel);

		clinicalPanelScrollPane.setViewportView(clinicalPanel);
		clinicalPanelScrollPane.setPreferredSize(new Dimension(350, 250));
		
		
		mainPanel.add(samplePanelScrollPane);
		mainPanel.add(clinicalPanelScrollPane);
		
		runPanel = new JPanel();
		run = new JButton("Run Algorithm");
		run.addActionListener(this);
		runPanel.add(run);
	}
	
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
	
	public void resetSamplePanel(JTable table){
		if (allGeneSamples!=null){
			allGeneSamples.getTableHeader().addMouseListener(this);
		}
		sampleScrollPane.setViewportView(table);
	}
	
	private List<FileChooserFilter> getFilters()
	{
		List<FileChooserFilter> filters = new ArrayList<FileChooserFilter>();
    	filters.add(new FileChooserFilter("CSV", "csv"));
    	filters.add(new FileChooserFilter("MAF", "maf"));
    	filters.add(new FileChooserFilter("MAF.TXT", "maf.txt"));
    	filters.add(new FileChooserFilter("TXT", "txt"));
    	return filters;
	}
	
	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public String getTitle() {
		return "HyperModules";
	}
	/**
	 * extracts data from MAF (mutation annotation format) standard 
	 */
	public void extractDataFromMaf(){
		Multimap<String, String> map = ArrayListMultimap.create();
		System.out.println(this.genes2samplesvalues.size());
		System.out.println(this.genes2samplesvalues.get(0).length);
		System.out.println(this.genes2samplesvalues.get(1).length);
		System.out.println(this.genes2samplesvalues.get(2).length);
		for (int i=1; i<this.genes2samplesvalues.size(); i++){
			if (this.genes2samplesvalues.get(i).length>15){
				String[] split = genes2samplesvalues.get(i)[15].split("-");
				String s = split[0] + "-" + split[1] + "-" + split[2];
				map.put(this.genes2samplesvalues.get(i)[0], s);
			}
		}
		
		 HashMap<String, String> map2 = new HashMap<String, String>();
		 for (String s : map.keySet()){
			 Collection<String> st = map.get(s);
			 Iterator<String> it = st.iterator();
			 String t = it.next();
			 while (it.hasNext()){
				t = t + ":" + it.next();
			 }
			 map2.put(s, t);
		 }
		
		 ArrayList<String[]> newgenesamples = new ArrayList<String[]>();
		 for (String s : map2.keySet()){
			 String[] st = new String[2];
			 st[0] = s;
			 st[1] = map2.get(s);
			 System.out.println(st[0] + "-" + st[1]);
			 newgenesamples.add(st);
		 }
		 
		 this.genes2samplesvalues = newgenesamples;
	}
	
	public void setClinicalPanelLogRank(Boolean header, int flag){
		
		clinicalPanel = new JPanel();
		clinicalPanel.setLayout(new BoxLayout(clinicalPanel, BoxLayout.Y_AXIS));
		loadClinicalData = new JButton("Load Clinical Data");
		loadClinicalData.addActionListener(this);
		loadClinicalData.setPreferredSize(new Dimension(150, 23));
		

		JPanel headerplusbutton = new JPanel();
		headerplusbutton.setLayout(new GridLayout(1,2));
		headerplusbutton.setPreferredSize(new Dimension(300, 30));
		headerplusbutton.setMaximumSize(new Dimension(300, 30));
		
		
		//clinicalPanel.add(loadClinicalData);
		clinicalheaders = new JCheckBox("CSV has Headers");
		//clinicalPanel.add(clinicalheaders);
		if (header){
			clinicalheaders.setSelected(true);
		}
		clinicalheaders.addActionListener(this);
		

		headerplusbutton.add(loadClinicalData);
		headerplusbutton.add(clinicalheaders);
		
		clinicalPanel.add(headerplusbutton);
		
		JPanel dropdowns = new JPanel();
		dropdowns.setLayout(new GridLayout(3,2));
		
		JLabel lrpatientlabel = new JLabel("patients: ");
		//clinicalPanel.add(lrpatientlabel, BorderLayout.WEST);
		lrpatients = new JComboBox();

		//clinicalPanel.add(lrpatients, BorderLayout.EAST);
		lrpatients.setPreferredSize(new Dimension(150, 23));
		lrpatients.setMaximumSize(new Dimension(150, 23));
		
		JLabel lrvitalstatuslabel = new JLabel("vital status: ");
		//clinicalPanel.add(lrvitalstatuslabel, BorderLayout.WEST);
		lrvitalstatus = new JComboBox();

		//clinicalPanel.add(lrvitalstatus, BorderLayout.EAST);
		lrvitalstatus.setPreferredSize(new Dimension(150, 23));
		lrvitalstatus.setMaximumSize(new Dimension(150, 23));
		
		JLabel lrdflabel = new JLabel("followup times: ");
		//clinicalPanel.add(lrdflabel, BorderLayout.WEST);
		lrdaysfollowup = new JComboBox();

		//clinicalPanel.add(lrdaysfollowup, BorderLayout.EAST);
		lrdaysfollowup.setPreferredSize(new Dimension(150, 23));
		lrdaysfollowup.setMaximumSize(new Dimension(150, 23));
		
		dropdowns.add(lrpatientlabel);
		dropdowns.add(lrpatients);
		dropdowns.add(lrvitalstatuslabel);
		dropdowns.add(lrvitalstatus);
		dropdowns.add(lrdflabel);
		dropdowns.add(lrdaysfollowup);
		dropdowns.setPreferredSize(new Dimension(300, 90));
		dropdowns.setMaximumSize(new Dimension(300, 90));
		
		clinicalPanel.add(dropdowns);

		if (flag==1){
		for (String h : columnIndices.keySet()){
			System.out.println(h);
			lrpatients.addItem(h);
			lrvitalstatus.addItem(h);
			lrdaysfollowup.addItem(h);
		}
		
		for (String h : columnIndices.keySet()){
			if (columnIndices.get(h)==0){
				lrpatients.setSelectedItem(h);
			}
			if (columnIndices.get(h)==1){
				lrvitalstatus.setSelectedItem(h);
			}
			if (columnIndices.get(h)==2){
				lrdaysfollowup.setSelectedItem(h);
			}
		}
		}
		
		loadClinicalPanel = new CollapsiblePanel("Clinical Data");
		clinicalScrollPane = new JScrollPane();
		clinicalScrollPane.setPreferredSize(new Dimension(300, 240));
		clinicalScrollPane.setMaximumSize(new Dimension(300, 240));
		loadClinicalPanel.getContentPane().add(clinicalScrollPane, BorderLayout.NORTH);
		loadClinicalPanel.setPreferredSize(new Dimension(3000, 280));
		loadClinicalPanel.setMaximumSize(new Dimension(3000, 280));
		clinicalPanel.setPreferredSize(new Dimension(300, 420));
		clinicalPanel.setMaximumSize(new Dimension(300, 420));
		clinicalPanel.add(loadClinicalPanel);
		
		clinicalPanelScrollPane.setViewportView(clinicalPanel);
		
		
		if (flag==1){
		int col1 = columnIndices.get(lrpatients.getSelectedItem());
		int col2 = columnIndices.get(lrvitalstatus.getSelectedItem());
		int col3 = columnIndices.get(lrdaysfollowup.getSelectedItem());
		setlrtable(col1, col2, col3);
		
		lrpatients.addActionListener(this);
		lrvitalstatus.addActionListener(this);
		lrdaysfollowup.addActionListener(this);
		}

	}
	
	public void setClinicalPanelFisher(Boolean header, int flag){

		clinicalPanel = new JPanel();
		clinicalPanel.setLayout(new BoxLayout(clinicalPanel, BoxLayout.Y_AXIS));
		loadClinicalData = new JButton("Load Clinical Data");
		loadClinicalData.addActionListener(this);
		loadClinicalData.setPreferredSize(new Dimension(150, 23));

		JPanel headerplusbutton = new JPanel();
		headerplusbutton.setLayout(new GridLayout(1,2));
		headerplusbutton.setPreferredSize(new Dimension(300, 30));
		headerplusbutton.setMaximumSize(new Dimension(300, 30));
		
		//clinicalPanel.add(loadClinicalData);
		clinicalheaders = new JCheckBox("CSV has Headers");
		//clinicalPanel.add(clinicalheaders);
		if (header){
			clinicalheaders.setSelected(true);
		}
		clinicalheaders.addActionListener(this);

		headerplusbutton.add(loadClinicalData);
		headerplusbutton.add(clinicalheaders);
		
		clinicalPanel.add(headerplusbutton);
		
		
		JPanel drops = new JPanel();
		drops.setLayout(new GridLayout(2,2));
		
		JLabel fpatientlabel = new JLabel("patients: ");
		fpatients = new JComboBox();
		fpatients.setPreferredSize(new Dimension(300, 23));
		fpatients.setMaximumSize(new Dimension(300, 23));
		
		JLabel fvariablelabel = new JLabel("clinical variable: ");
		fvariable = new JComboBox();
		fvariable.setPreferredSize(new Dimension(150, 23));
		fvariable.setMaximumSize(new Dimension(150, 23));
		
		drops.add(fpatientlabel);
		drops.add(fpatients);
		drops.add(fvariablelabel);
		drops.add(fvariable);
		drops.setPreferredSize(new Dimension(300, 60));
		drops.setMaximumSize(new Dimension(300, 60));
		
		clinicalPanel.add(drops);
		
		
		if (flag==1){
		for (String h : columnIndices.keySet()){
			fpatients.addItem(h);
			fvariable.addItem(h);
		}
		
		for (String h : columnIndices.keySet()){
			if (columnIndices.get(h)==0){
				fpatients.setSelectedItem(h);
			}
			if (columnIndices.get(h)==1){
				fvariable.setSelectedItem(h);
			}
		}
		}
		
		loadClinicalPanel = new CollapsiblePanel("Clinical Data");
		clinicalScrollPane = new JScrollPane();
		clinicalScrollPane.setPreferredSize(new Dimension(300, 240));
		clinicalScrollPane.setMaximumSize(new Dimension(300, 240));
		loadClinicalPanel.getContentPane().add(clinicalScrollPane, BorderLayout.NORTH);
		loadClinicalPanel.setPreferredSize(new Dimension(3000, 280));
		loadClinicalPanel.setMaximumSize(new Dimension(3000, 280));
		clinicalPanel.setPreferredSize(new Dimension(300, 420));
		clinicalPanel.setMaximumSize(new Dimension(300, 420));
		clinicalPanel.add(loadClinicalPanel);
		
		clinicalPanelScrollPane.setViewportView(clinicalPanel);
		
		if (flag==1){
		int col1 = columnIndices.get(fpatients.getSelectedItem());
		int col2 = columnIndices.get(fvariable.getSelectedItem());
		setfishertable(col1, col2);
		
		fpatients.addActionListener(this);
		fvariable.addActionListener(this);
		}
		
	}

	public void setfishertable(int one, int two){
		otherValues = new ArrayList<String[]>();
		for (int i=0; i<allClinicalData.size(); i++){
			String[] add = new String[2];
			add[0] = allClinicalData.get(i)[one];
			add[1] = allClinicalData.get(i)[two];
			otherValues.add(add);
		}
		
		String[] t = {"Patient ID", "Clinical Variable"};
		MyModel nm = new MyModel(t);
		nm.AddCSVData(otherValues);
		clinicalTable = new JTable();
		clinicalTable.setModel(nm);
		clinicalScrollPane.setViewportView(clinicalTable);	
	}
	
	public void setlrtable(int one, int two, int three){
		clinicalValues = new ArrayList<String[]>();
		for (int i=0; i<allClinicalData.size(); i++){
			String[] add = new String[3];
			add[0] = allClinicalData.get(i)[one];
			add[1] = allClinicalData.get(i)[two];
			add[2] = allClinicalData.get(i)[three];
			clinicalValues.add(add);
		}
		
		String[] h = {"Patient ID", "Vital", "Followup Times"};
		MyModel nm = new MyModel(h);
		nm.AddCSVData(clinicalValues);
		clinicalTable = new JTable();
		clinicalTable.setModel(nm);
		clinicalScrollPane.setViewportView(clinicalTable);
	}
	
	
	@Override
	public void actionPerformed(ActionEvent ae) {
		
		if (ae.getSource() == lrpatients || ae.getSource() == lrvitalstatus || ae.getSource() == lrdaysfollowup){
			int col1 = columnIndices.get(lrpatients.getSelectedItem());
			int col2 = columnIndices.get(lrvitalstatus.getSelectedItem());
			int col3 = columnIndices.get(lrdaysfollowup.getSelectedItem());
			setlrtable(col1, col2, col3);
		}
		
		if (ae.getSource() == fpatients || ae.getSource() == fvariable){
			int col1 = columnIndices.get(fpatients.getSelectedItem());
			int col2 = columnIndices.get(fvariable.getSelectedItem());
			setfishertable(col1, col2);
		}
		
		if (ae.getSource() == logRank){
			if (allClinicalData!=null && allClinicalData.get(0).length>=3){
				setClinicalPanelLogRank(clinicalheaders.isSelected(), 1);
			}
			else if (allClinicalData!=null && allClinicalData.get(0).length < 3){
				ErrorDialog ed = new ErrorDialog(utils, "There are not enough columns in the CSV for log rank test.");
				ed.setLocationRelativeTo(null);
				ed.setVisible(true);
				System.out.println("Not enough columns for log rank!");
				fisher.setSelected(true);
			}
			else{
				setClinicalPanelLogRank(clinicalheaders.isSelected(), 0);
			}
		}
		
		if (ae.getSource() == fisher){
			if (allClinicalData!=null){
				setClinicalPanelFisher(clinicalheaders.isSelected(), 1);
			}
			else{
				setClinicalPanelFisher(clinicalheaders.isSelected(), 0);
			}
		}
		
		if (ae.getSource() == headers){
			if (headers.isSelected()){
				if (genes2samplesvalues!=null){
					if (header == null && genes2samplesvalues!=null){
						header = genes2samplesvalues.get(0);
						genes2samplesvalues.remove(0);
					}

	        		MyModel NewModel = new MyModel(header);
	           	 	NewModel.AddCSVData(genes2samplesvalues);
	           	 	allGeneSamples = new JTable();
	           	 	allGeneSamples.setModel(NewModel);
	           	 	resetSamplePanel(allGeneSamples);
					
				}	
				
			}
			else{
				if (genes2samplesvalues!=null){
					if (header!=null){
						genes2samplesvalues.add(0, header);
						header = null;
						String[] c = {"genes", "samples"};
						MyModel NewModel = new MyModel(c);
						NewModel.AddCSVData(genes2samplesvalues);
			        	allGeneSamples = new JTable();
			        	allGeneSamples.setModel(NewModel);
			        	resetSamplePanel(allGeneSamples);
					}
				}
				
			}
		}
		
		if (ae.getSource() == clinicalheaders){
			if (clinicalheaders.isSelected()){
				if (allClinicalData!=null){
					if (clinicalheader == null){
						clinicalheader = allClinicalData.get(0);
						allClinicalData.remove(0);
					}
					if (logRank.isSelected()){
						
						lrpatients.removeActionListener(this);
						lrvitalstatus.removeActionListener(this);
						lrdaysfollowup.removeActionListener(this);
						
						lrpatients.removeAllItems();
						lrvitalstatus.removeAllItems();
						lrdaysfollowup.removeAllItems();
						
						columnIndices = new HashMap<String, Integer>();
						for (int u=0; u<clinicalheader.length; u++){
							columnIndices.put(clinicalheader[u], u);
						}
						
						for (String s : columnIndices.keySet()){
							lrpatients.addItem(s);
							lrvitalstatus.addItem(s);
							lrdaysfollowup.addItem(s);
						}
						
						for (String h : columnIndices.keySet()){
							if (columnIndices.get(h)==0){
								lrpatients.setSelectedItem(h);
							}
							if (columnIndices.get(h)==1){
								lrvitalstatus.setSelectedItem(h);
							}
							if (columnIndices.get(h)==2){
								lrdaysfollowup.setSelectedItem(h);
							}
						}
						
						int col1 = columnIndices.get(lrpatients.getSelectedItem());
						int col2 = columnIndices.get(lrvitalstatus.getSelectedItem());
						int col3 = columnIndices.get(lrdaysfollowup.getSelectedItem());
						setlrtable(col1, col2, col3);

						lrpatients.addActionListener(this);
						lrvitalstatus.addActionListener(this);
						lrdaysfollowup.addActionListener(this);

						
					}
					else if (fisher.isSelected()){
						fpatients.removeActionListener(this);
						fvariable.removeActionListener(this);
						
						fpatients.removeAllItems();
						fvariable.removeAllItems();
						
						columnIndices = new HashMap<String, Integer>();
						for (int u = 0; u<clinicalheader.length; u++){
							columnIndices.put(clinicalheader[u], u);
						}
						
						for (String s : columnIndices.keySet()){
							fpatients.addItem(s);
							fvariable.addItem(s);
						}
						
						for (String h : columnIndices.keySet()){
							if (columnIndices.get(h)==0){
								fpatients.setSelectedItem(h);
							}
							if (columnIndices.get(h)==1){
								fvariable.setSelectedItem(h);
							}
						}
						
						int col1 = columnIndices.get(fpatients.getSelectedItem());
						int col2 = columnIndices.get(fvariable.getSelectedItem());
						setfishertable(col1, col2);
						
						fpatients.addActionListener(this);
						fvariable.addActionListener(this);
					}
				}
				
			}
			else{
				if (allClinicalData!=null){
					if (clinicalheader!=null){
						allClinicalData.add(0, clinicalheader);
						clinicalheader = null;
						if (logRank.isSelected()){
							
							lrpatients.removeActionListener(this);
							lrvitalstatus.removeActionListener(this);
							lrdaysfollowup.removeActionListener(this);
							
							lrpatients.removeAllItems();
							lrvitalstatus.removeAllItems();
							lrdaysfollowup.removeAllItems();
							
							columnIndices = new HashMap<String, Integer>();
							for (int u=0; u<allClinicalData.get(0).length; u++){
								columnIndices.put("Column " + (u+1), u);
							}
							
							for (String s : columnIndices.keySet()){
								lrpatients.addItem(s);
								lrvitalstatus.addItem(s);
								lrdaysfollowup.addItem(s);
							}
							
							for (String h : columnIndices.keySet()){
								if (columnIndices.get(h)==0){
									lrpatients.setSelectedItem(h);
								}
								if (columnIndices.get(h)==1){
									lrvitalstatus.setSelectedItem(h);
								}
								if (columnIndices.get(h)==2){
									lrdaysfollowup.setSelectedItem(h);
								}
							}
							
							int col1 = columnIndices.get(lrpatients.getSelectedItem());
							int col2 = columnIndices.get(lrvitalstatus.getSelectedItem());
							int col3 = columnIndices.get(lrdaysfollowup.getSelectedItem());
							setlrtable(col1, col2, col3);

							lrpatients.addActionListener(this);
							lrvitalstatus.addActionListener(this);
							lrdaysfollowup.addActionListener(this);
						}
						else if (fisher.isSelected()){
							fpatients.removeActionListener(this);
							fvariable.removeActionListener(this);
							
							fpatients.removeAllItems();
							fvariable.removeAllItems();
							
							columnIndices = new HashMap<String, Integer>();
							for (int u = 0; u<allClinicalData.get(0).length; u++){
								columnIndices.put("Column " + (u+1), u);
							}
							
							for (String s : columnIndices.keySet()){
								fpatients.addItem(s);
								fvariable.addItem(s);
							}
							
							for (String h : columnIndices.keySet()){
								if (columnIndices.get(h)==0){
									fpatients.setSelectedItem(h);
								}
								if (columnIndices.get(h)==1){
									fvariable.setSelectedItem(h);
								}
							}
							
							int col1 = columnIndices.get(fpatients.getSelectedItem());
							int col2 = columnIndices.get(fvariable.getSelectedItem());
							setfishertable(col1, col2);
							
							fpatients.addActionListener(this);
							fvariable.addActionListener(this);
						}	
					}
				}
			}
			
		}
		
		if (ae.getSource() == loadClinicalData){
			File DataFile = utils.fileUtil.getFile(utils.swingApp.getJFrame(), "Load Clinical Data", FileUtil.LOAD, getFilters());
			CSVFile Rd = new CSVFile();
			allClinicalData = Rd.ReadCSVfile(DataFile);
			columnIndices = new HashMap<String, Integer>();
			if (allClinicalData.get(0).length<3 && logRank.isSelected()){
				ErrorDialog ed = new ErrorDialog(utils, "There are not enough columns in the CSV for log rank test.");
				ed.setLocationRelativeTo(null);
				ed.setVisible(true);
				System.out.println("Not enough columns!");
				return;
			}
			if (clinicalheaders.isSelected()){
				clinicalheader = allClinicalData.get(0);
				allClinicalData.remove(0);
				for (int y = 0; y<clinicalheader.length; y++){
					columnIndices.put(clinicalheader[y], y);
				}
			}
			else{
				for (int y = 0; y<allClinicalData.get(0).length; y++){
					columnIndices.put("Column " + (y+1), y);
				}
			}
			
			if (logRank.isSelected()){
				setClinicalPanelLogRank(clinicalheaders.isSelected(), 1);
			}
			else if (fisher.isSelected()){
				setClinicalPanelFisher(clinicalheaders.isSelected(), 1);
			}
			loadClinicalPanel.setCollapsed(false);
		}
		
		if (ae.getSource() == loadSamples){
			
        	File DataFile = utils.fileUtil.getFile(utils.swingApp.getJFrame(), "Load Samples", FileUtil.LOAD, getFilters());
        	CSVFile Rd = new CSVFile();
        	genes2samplesvalues = Rd.ReadCSVfile(DataFile);
        	MyModel NewModel = null;
        	if (headers.isSelected()){
        		header = genes2samplesvalues.get(0);
        		NewModel = new MyModel(header);
        		genes2samplesvalues.remove(0);
        	}
        	else{
        		String[] c = { "genes", "samples"};
        		NewModel = new MyModel(c);
        	}
        	if (genes2samplesvalues.get(0)[0].equals("Hugo_Symbol")){
        		extractDataFromMaf();
        	}
        	
        	/*
        	if (!genes2samplesvalues.get(0)[1].equals("no_sample")){
        		if (genes2samplesvalues.get(0)[1].length()<5){
            		genes2samplesvalues.remove(0);
        		}
        		else if (!genes2samplesvalues.get(0)[1].substring(0,4).equals("TCGA")) {
            		genes2samplesvalues.remove(0);
        		}

        	}
        	*/
        	
        	
        	
        	 NewModel.AddCSVData(genes2samplesvalues);
        	 allGeneSamples = new JTable();
        	 allGeneSamples.setModel(NewModel);
        	 resetSamplePanel(allGeneSamples);
        	 loadSamplePanel.setCollapsed(false);
        	 //otherValues = new ArrayList<String[]>();
		}
		/*
		if (ae.getSource()==sort){
			if (genes2samplesvalues!=null){
				Multimap<String, String> reference = ArrayListMultimap.create();
				for (int i=0; i<genes2samplesvalues.size(); i++){
					reference.put(genes2samplesvalues.get(i)[0], genes2samplesvalues.get(i)[1]);
				}
				
				ArrayList<String> keys = new ArrayList<String>();
				for (int i=0; i<genes2samplesvalues.size(); i++){
					keys.add(genes2samplesvalues.get(i)[0]);
				}
				
				Collections.sort(keys);
				
				ArrayList<String[]> newGeneTable = new ArrayList<String[]>();
				for (int i=0; i<keys.size(); i++){
					for (String value : reference.get(keys.get(i))){
						String[] entry = new String[2];
						entry[0] = keys.get(i);
						entry[1] = value;
						newGeneTable.add(entry);
					}
				}
				
				
				MyModel NewModel = null;
	        	if (headers.isSelected()){
	        		NewModel = new MyModel(header);
	        	}
	        	else{
	        		String[] c = { "genes", "samples"};
	        		NewModel = new MyModel(c);
	        	}
	        	
	        	 NewModel.AddCSVData(newGeneTable);
	        	 allGeneSamples = new JTable();
	        	 allGeneSamples.setModel(NewModel);
	        	 resetSamplePanel(allGeneSamples);
				
			}
			
		}
		*/
		if (ae.getSource() == run){

			String shuffleNumber = nShuffled.getText();
			int number = 0;
			try{
				number = Integer.parseInt(shuffleNumber);
			}
			catch (Exception e){
				System.out.println("Please Enter a Number!");
				return;
			}
			
				String expandOption = "default";
				if (expandComboBox.getSelectedItem().equals("Expand From Selected Seeds")){
					expandOption = "expand";
				}
				else{
					expandOption = "findMost";
				}

				String stat = "default";
				if (logRank.isSelected()){
					stat = "logRank";
				}
				else if (fisher.isSelected()){
					stat = "fisher";
				}
				
				CyNetwork currNet = netSelect.getSelectedNetwork();
				
				
				
				if (genes2samplesvalues!=null){
					genes2samplesvaluescopy = new ArrayList<String[]>();
					for (int i=0; i<genes2samplesvalues.size(); i++){
						genes2samplesvaluescopy.add(genes2samplesvalues.get(i));
					}
					handleSampleValues(genes2samplesvaluescopy);
				}
				
			if (stat.equals("logRank") && genes2samplesvaluescopy!=null && clinicalValues!=null){
				if (handleSurvivalExceptions()){
					utils.taskMgr.execute(new TaskIterator(new AlgorithmTask(currNet, number,expandOption, stat, genes2samplesvaluescopy, clinicalValues, otherValues, utils)));
				}
			}
			else if (stat.equals("fisher") && genes2samplesvaluescopy!=null && otherValues!=null){
				if (handleClinicalVariableExceptions()){
					utils.taskMgr.execute(new TaskIterator(new AlgorithmTask(currNet, number,expandOption, stat, genes2samplesvaluescopy, clinicalValues, otherValues, utils)));
				}
			}
			else{
				ErrorDialog ed = new ErrorDialog(utils, "Load mutation table AND clinical table before running the algorithm.");
				ed.setLocationRelativeTo(null);
				ed.setVisible(true);
				System.out.println("Load Table!");
			}
		}
	}
	
	public void handleSampleValues(ArrayList<String[]> samples){
		for (int i=0; i<samples.size(); i++){
			if (samples.get(i)[1]!=null){
				if (samples.get(i)[1].equals(" "))
				samples.get(i)[1] = "no_sample";
			}
		}
		
	}

	public boolean handleSurvivalExceptions(){
		boolean valid = true;
		HashSet<String> vitals = new HashSet<String>();
		for (int i=0; i<clinicalValues.size(); i++){
			if (clinicalValues.get(i)[1]!=null){
				if (!clinicalValues.get(i)[1].isEmpty()){
					vitals.add(clinicalValues.get(i)[1]);
				}

			}
			
			//System.out.println(clinicalValues.get(i)[1]);
			if (vitals.size() > 2){
				ErrorDialog ed = new ErrorDialog(utils, "INPUT ERROR: Vital Status must be either DECEASED or ALIVE");
				ed.setLocationRelativeTo(null);
				ed.setVisible(true);
				System.out.println("INPUT ERROR: Vital Status must be either DECEASED or ALIVE");
				return false;
			}
			if (clinicalValues.get(i)[2]!=null){
				if (!clinicalValues.get(i)[2].equals("NA")){
					try{
						Double.valueOf(clinicalValues.get(i)[2]);
					}
					catch (Exception e){
						valid = false;
					}
				}
			}
		}
		if (valid == false){
			ErrorDialog ed = new ErrorDialog(utils, "INPUT ERROR: All followup times entries must be a number");
			ed.setLocationRelativeTo(null);
			ed.setVisible(true);
			System.out.println("INPUT ERROR: All followup times entries must be a number");
		}
		return valid;
	}
	
	public boolean handleClinicalVariableExceptions(){
		boolean valid = true;
		HashSet<String> variables = new HashSet<String>();
		for (int  i=0; i<otherValues.size(); i++){
			if (otherValues.get(i)[1]!=null){
				if (otherValues.isEmpty()){
					otherValues.remove(i);
				}
				else{
					variables.add(otherValues.get(i)[1]);
					//System.out.println(otherValues.get(i)[1]);
				}
			}

			if (variables.size() > 5){
				ErrorDialog ed = new ErrorDialog(utils, "INPUT ERROR: Please pick a clinical variable with fewer categories");
				ed.setLocationRelativeTo(null);
				ed.setVisible(true);
				System.out.println("INPUT ERROR: Please pick a clinical variable with fewer categories");
				return false;
			}
		}
		System.out.println("Number of fisher categories: " + variables.size());
		return valid;
	}
	
	

	/**
	 * CSVFile class - represents/reads data from a comma separated value file
	 * @author alvinleung
	 *
	 */
	public class CSVFile {
	     private ArrayList<String[]> Rs = new ArrayList<String[]>();
	     private String[] OneRow;

	        public ArrayList<String[]> ReadCSVfile (File DataFile) {
	            try {
	            BufferedReader brd = new BufferedReader (new FileReader(DataFile));

	            String st = brd.readLine();
	            
	            	while (st!=null) {
	            			OneRow = st.split(",|;|\t");
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
	            			//System.out.println (Arrays.toString(OneRow));
	            			st = brd.readLine();
	                } 
	            } 
	            catch (Exception e) {
	                String errmsg = e.getMessage();
	                System.out.println ("File not found:" +errmsg);
	            }                  
	        return Rs;
	        }
	 }
	
	
	
	
	private class MyModel extends AbstractTableModel{

		private static final long serialVersionUID = 1L;
		private String[] columnNames;
		private ArrayList<String[]> data =  new ArrayList<String[]>();
		
		public MyModel(String[] columns){
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
	      public Object getValueAt(int row, int col)
	      {
	          return data.get(row)[col];
	      } 
	}




	@Override
	public void mouseClicked(MouseEvent arg0) {

	    int col = allGeneSamples.columnAtPoint(arg0.getPoint());
	    
	    if (genes2samplesvalues!=null && col==0 && state ==2 ){
			MyModel NewModel = null;
        	if (headers.isSelected()){
        		NewModel = new MyModel(header);
        	}
        	else{
        		String[] c = { "genes", "samples"};
        		NewModel = new MyModel(c);
        	}
        	
        	 NewModel.AddCSVData(genes2samplesvalues);
        	 allGeneSamples = new JTable();
        	 allGeneSamples.setModel(NewModel);
        	 resetSamplePanel(allGeneSamples);
        	 
        	 state = 0;
	    }
	    
	    
	    if (genes2samplesvalues!=null && col==0 && state ==1 ){
	    	ArrayList<String[]> newGeneTableReversed = new ArrayList<String[]>();
	    	for (int i= newGeneTable.size()-1; i>=0; i--){
	    		newGeneTableReversed.add(newGeneTable.get(i));
	    	}
	    	
			MyModel NewModel = null;
        	if (headers.isSelected()){
        		NewModel = new MyModel(header);
        	}
        	else{
        		String[] c = { "genes", "samples"};
        		NewModel = new MyModel(c);
        	}
        	
        	 NewModel.AddCSVData(newGeneTableReversed);
        	 allGeneSamples = new JTable();
        	 allGeneSamples.setModel(NewModel);
        	 resetSamplePanel(allGeneSamples);
        	 
        	 state = 2;
	    	
	    }
	    
		if (genes2samplesvalues!=null && col==0 && state ==0){
			Multimap<String, String> reference = ArrayListMultimap.create();
			for (int i=0; i<genes2samplesvalues.size(); i++){
				reference.put(genes2samplesvalues.get(i)[0], genes2samplesvalues.get(i)[1]);
			}
			
			ArrayList<String> keys = new ArrayList<String>();
			for (int i=0; i<genes2samplesvalues.size(); i++){
				keys.add(genes2samplesvalues.get(i)[0]);
			}
			
			Collections.sort(keys);
			
			newGeneTable = new ArrayList<String[]>();
			for (int i=0; i<keys.size(); i++){
				for (String value : reference.get(keys.get(i))){
					String[] entry = new String[2];
					entry[0] = keys.get(i);
					entry[1] = value;
					newGeneTable.add(entry);
				}
			}
			
			
			MyModel NewModel = null;
        	if (headers.isSelected()){
        		NewModel = new MyModel(header);
        	}
        	else{
        		String[] c = { "genes", "samples"};
        		NewModel = new MyModel(c);
        	}
        	
        	 NewModel.AddCSVData(newGeneTable);
        	 allGeneSamples = new JTable();
        	 allGeneSamples.setModel(NewModel);
        	 resetSamplePanel(allGeneSamples);
        	 
        	 state = 1;
			
		}
		
		
	}


	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
