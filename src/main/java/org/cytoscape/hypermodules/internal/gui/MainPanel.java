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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.hypermodules.internal.CytoscapeUtils;
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

/**
 * 
 * the main hypermodules panel in the cytoscape control panel. contains all the options.
 * @author alvinleung
 *
 */
public class MainPanel extends JPanel implements CytoPanelComponent, ActionListener{
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
		private JPanel otherPanel;
		private JScrollPane otherPanelScrollPane;
			private CollapsiblePanel loadOtherPanel;
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
	 * paths of length 1 from seed
	 */
	private JRadioButton one;
	/**
	 * paths of length 2 from seed
	 */
	private JRadioButton two;
	/**
	 * length options group
	 */
	private ButtonGroup lengthOptions;
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
	 * load gene-sample association - we have a jscrollpane to view data
	 */
	private JButton loadSamples;
		private JTable allGeneSamples;
		private JScrollPane sampleScrollPane;
	/**
	 * load clinical survival data
	 */
	private JButton loadSurvivalData;
		private JTable survivalTable;
		private JScrollPane survivalScrollPane;
	/**
	 * load other clinical variable data
	 */
	private JButton loadOtherData;
		private JTable otherDataTable;
		private JScrollPane otherScrollPane;
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
		testPanel.setBorder(BorderFactory.createTitledBorder("Statistical Test:"));
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
		logRank = new JRadioButton("Log Rank Test");
		fisher = new JRadioButton("Fisher's Exact Test (Discrete)");
		statTest = new ButtonGroup();
		statTest.add(logRank);
		statTest.add(fisher);
		testPanel.add(logRank);
		testPanel.add(fisher);
		
		
		shufflePanel = new JPanel();
		shuffle = new JLabel("Shuffle Number:");
		nShuffled = new JTextField("0", 5);
		shufflePanel.add(shuffle, BorderLayout.WEST);
		shufflePanel.add(nShuffled, BorderLayout.EAST);
		shufflePanel.setPreferredSize(new Dimension(350, 40));
		shufflePanel.setMinimumSize(new Dimension(350, 40));
		shufflePanel.setMaximumSize(new Dimension(350, 40));
		shufflePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		
		mainPanel.add(expandOptionPanel);
		mainPanel.add(testPanel);
		mainPanel.add(shufflePanel);
		
		samplePanel = new JPanel();
		samplePanelScrollPane = new JScrollPane();
		
		samplePanel.setLayout(new BoxLayout(samplePanel, BoxLayout.Y_AXIS));
		loadSamples = new JButton("Load Sample Data");
		loadSamples.addActionListener(this);
		loadSamples.setPreferredSize(new Dimension(150, 23));
		samplePanel.add(loadSamples);
		
		loadSamplePanel = new CollapsiblePanel("Samples");
		allGeneSamples = new JTable(new MyModel1());   
		sampleScrollPane = new JScrollPane(allGeneSamples);
		sampleScrollPane.setPreferredSize(new Dimension(300, 240));
		sampleScrollPane.setMaximumSize(new Dimension(300, 240));
		loadSamplePanel.getContentPane().add(sampleScrollPane, BorderLayout.NORTH);
		loadSamplePanel.setPreferredSize(new Dimension(3000, 280));
		loadSamplePanel.setMaximumSize(new Dimension(3000, 280));
		samplePanel.setPreferredSize(new Dimension(300, 300));
		samplePanel.setMaximumSize(new Dimension(300, 300));
		samplePanel.add(loadSamplePanel);
		
		samplePanelScrollPane.setViewportView(samplePanel);
		samplePanelScrollPane.setPreferredSize(new Dimension(350, 200));

		clinicalPanel = new JPanel();
		clinicalPanelScrollPane = new JScrollPane();
		
		clinicalPanel.setLayout(new BoxLayout(clinicalPanel, BoxLayout.Y_AXIS));
		loadSurvivalData = new JButton("Load Clinical Survival Data");
		loadSurvivalData.addActionListener(this);
		loadSurvivalData.setPreferredSize(new Dimension(150, 23));
		clinicalPanel.add(loadSurvivalData);

		loadClinicalPanel = new CollapsiblePanel("Clinical Survival Data");
		survivalTable = new JTable(new MyModel2());
		survivalScrollPane = new JScrollPane(survivalTable);
		loadClinicalPanel.getContentPane().add(survivalScrollPane, BorderLayout.NORTH);
		loadClinicalPanel.setPreferredSize(new Dimension(250, 175));
		clinicalPanel.add(loadClinicalPanel);
		
		clinicalPanelScrollPane.setViewportView(clinicalPanel);
		clinicalPanelScrollPane.setPreferredSize(new Dimension(350, 200));
		
		otherPanel = new JPanel();
		otherPanelScrollPane = new JScrollPane();
		
		
		otherPanel.setLayout(new BoxLayout(otherPanel, BoxLayout.Y_AXIS));
		loadOtherData = new JButton("Load Other Clinical Data");
		loadOtherData.addActionListener(this);
		loadOtherData.setPreferredSize(new Dimension(150, 23));
		otherPanel.add(loadOtherData);

		
		loadOtherPanel = new CollapsiblePanel("Clinical Variable Data");
		otherDataTable = new JTable(new MyModel3());
		otherScrollPane = new JScrollPane(otherDataTable);
		loadOtherPanel.getContentPane().add(otherScrollPane, BorderLayout.NORTH);
		loadOtherPanel.setPreferredSize(new Dimension(250, 125));
		otherPanel.add(loadOtherPanel);
		otherPanelScrollPane.setViewportView(otherPanel);
		otherPanelScrollPane.setPreferredSize(new Dimension(350, 150));

		
		mainPanel.add(samplePanelScrollPane);
		mainPanel.add(clinicalPanelScrollPane);
		mainPanel.add(otherPanelScrollPane);
		
		runPanel = new JPanel();
		run = new JButton("Run Algorithm");
		run.addActionListener(this);
		runPanel.add(run);
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
	
	@Override
	public void actionPerformed(ActionEvent ae) {
		
		
		if (ae.getSource()==loadSamples){
			
        	File DataFile = utils.fileUtil.getFile(utils.swingApp.getJFrame(), "Load Samples", FileUtil.LOAD, getFilters());
        	CSVFile Rd = new CSVFile();
        	MyModel1 NewModel = new MyModel1();
        	genes2samplesvalues = Rd.ReadCSVfile(DataFile);
        	if (genes2samplesvalues.get(0)[0].equals("Hugo_Symbol")){
        		extractDataFromMaf();
        	}
        	
        	if (!genes2samplesvalues.get(0)[1].equals("no_sample")){
        		if (genes2samplesvalues.get(0)[1].length()<5){
            		genes2samplesvalues.remove(0);
        		}
        		else if (!genes2samplesvalues.get(0)[1].substring(0,4).equals("TCGA")) {
            		genes2samplesvalues.remove(0);
        		}

        	}
        	
        	 NewModel.AddCSVData(genes2samplesvalues);
        	 allGeneSamples.setModel(NewModel);
        	 otherValues = new ArrayList<String[]>();
		}
		
		if (ae.getSource()==loadSurvivalData){
        	File DataFile2 = utils.fileUtil.getFile(utils.swingApp.getJFrame(), "Load Survival Data", FileUtil.LOAD, getFilters());
        	CSVFile Rd2 = new CSVFile();
        	MyModel2 NewModel2 = new MyModel2();
        	clinicalValues = Rd2.ReadCSVfile(DataFile2);
        	try{
        		double d = Double.valueOf(clinicalValues.get(0)[2]);
        	}
        	catch(NumberFormatException e){
        		clinicalValues.remove(0);
        	}
        	 NewModel2.AddCSVData(clinicalValues);
        	 survivalTable.setModel(NewModel2);
		}
		
		if (ae.getSource()==loadOtherData){
			File DataFile3 = utils.fileUtil.getFile(utils.swingApp.getJFrame(), "Load Other Clinical Data", FileUtil.LOAD, getFilters());
			CSVFile Rd3 = new CSVFile();
			MyModel3 NewModel3 = new MyModel3();
			otherValues = Rd3.ReadCSVfile(DataFile3);
			NewModel3.AddCSVData(otherValues);
			otherDataTable.setModel(NewModel3);
		}
		
		
		if (ae.getSource()==run){
			
			String shuffleNumber = nShuffled.getText();
			int number = Integer.parseInt(shuffleNumber);
			
			if (genes2samplesvalues!=null && clinicalValues!=null){
				
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
				
			//	AlgorithmTaskFactory algtaskfac = new AlgorithmTaskFactory(number, lengthOption, expandOption, stat, genes2samplesvalues, clinicalValues, otherValues, utils);
			//	algtaskfac.createTaskIterator();
				CyNetwork currNet = netSelect.getSelectedNetwork();
				utils.taskMgr.execute(new TaskIterator(new AlgorithmTask(currNet, number,expandOption, stat, genes2samplesvalues, clinicalValues, otherValues, utils)));
				//utils.taskMgr.execute(new TaskIterator(new AlgorithmTask3(number, expandOption, stat, genes2samplesvalues, clinicalValues, otherValues, utils)));
				//utils.taskMgr.execute(new TaskIterator(new ImprovedAlgorithmTask(number, expandOption, stat, genes2samplesvalues, clinicalValues, utils)));
			}
			else{
				System.out.println("Load Table!");
			}

		}
		
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
	            			OneRow = st.split(",|\\s|;|\t");
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
	
	/**
	 * 
	 * MyModel classes - extends tables to include the column names and number of columns we want.
	 * @author alvinleung
	 *
	 */
	
	private class MyModel1 extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private String[] columnNames = { "genes", "samples"};
      private ArrayList<String[]> data =  new ArrayList<String[]>();
   //   private Class[] columnTypes = {String.class, String.class};

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
	
  private class MyModel2 extends AbstractTableModel {
 	 
		private static final long serialVersionUID = 1L;
		private String[] columnNames = {"Patient ID", "Vital", "Days Followup", "Days Birth"};
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
  
	private class MyModel3 extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private String[] columnNames = { "Patient ID", "Clinical Variable"};
      private ArrayList<String[]> data =  new ArrayList<String[]>();
   //   private Class[] columnTypes = {String.class, String.class};

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
	
	
}
