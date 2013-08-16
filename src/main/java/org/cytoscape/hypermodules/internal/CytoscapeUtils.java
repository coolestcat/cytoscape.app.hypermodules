package org.cytoscape.hypermodules.internal;

import java.util.HashSet;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.hypermodules.internal.gui.MainPanel;
import org.cytoscape.hypermodules.internal.gui.ResultsPanel;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskManager;

/**
 * 
 * A convenience class that groups together all the cytoscape services (already registered in cyActivator) that need to be used.
 * @author alvinleung
 *
 */

public class CytoscapeUtils {
	
	/**
	 * Application Manager
	 */
	public CyApplicationManager appMgr;
	/**
	 * Creates new Task Iterators to run cytoscape tasks
	 */
	public TaskManager<?,?> taskMgr;
	/**
	 * Manages cynetwork views
	 */
	public CyNetworkViewManager netViewMgr;
	/**
	 * Manages cynetworks
	 */
	public CyNetworkManager netMgr;
	/**
	 * Allows us to register services (like cytopanels)
	 */
	public CyServiceRegistrar serviceRegistrar;
	/**
	 * eventHelper
	 */
	public CyEventHelper eventHelper;
	/**
	 * Naming class
	 */
	public CyNetworkNaming networkNaming;
	/**
	 * I/O
	 */
	public FileUtil fileUtil;
	/**
	 * Opens default browser for hyperlinks
	 */
	public OpenBrowser openBrowser;
	/**
	 * generates network views
	 */
	public CyNetworkViewFactory netViewFactory;
	/**
	 * manages root network
	 */
	public CyRootNetworkManager rootNetworkMgr;
	/**
	 * cySwingApp
	 */
	public CySwingApplication swingApp;
	/**
	 * generates networks
	 */
	public CyNetworkFactory networkFactory;
	/**
	 * manages vizmapper
	 */
	public VisualMappingManager vmmServiceRef;
	/**
	 * generates visual styles
	 */
	public VisualStyleFactory visualStyleFactoryServiceRef;
	/**
	 * continuous vizmapping functions
	 */
	public VisualMappingFunctionFactory vmfFactoryC;
	/**
	 * discrete vizmapping functions
	 */
	public VisualMappingFunctionFactory vmfFactoryD;
	/**
	 * passthrough vizmapping functions
	 */
	public VisualMappingFunctionFactory vmfFactoryP;
	/**
	 * manages layouts
	 */
	public CyLayoutAlgorithmManager cyLayoutManager;
	
	public CytoscapeUtils(CyApplicationManager appMgr, 
			TaskManager<?,?> taskMgr,
			CyNetworkViewManager netViewMgr, 
			CyNetworkManager netMgr,
			CyServiceRegistrar serviceRegistrar,
			CyEventHelper eventHelper,
			CyNetworkNaming networkNaming,
			FileUtil fileUtil,
			OpenBrowser openBrowser,
			CyNetworkViewFactory netViewFactory,
			CyRootNetworkManager rootNetworkMgr,
			CySwingApplication swingApp,
			CyNetworkFactory networkFactory,
			VisualMappingManager vmmServiceRef,
			VisualStyleFactory visualStyleFactoryServiceRef,
			VisualMappingFunctionFactory vmfFactoryC,
			VisualMappingFunctionFactory vmfFactoryD,
			VisualMappingFunctionFactory vmfFactoryP,
			CyLayoutAlgorithmManager cyLayoutManager){
		
		this.appMgr = appMgr;
		this.taskMgr = taskMgr;
		this.netViewMgr = netViewMgr;
		this.netMgr = netMgr;
		this.serviceRegistrar = serviceRegistrar;
		this.eventHelper = eventHelper;
		this.networkNaming = networkNaming;
		this.fileUtil = fileUtil;
		this.openBrowser = openBrowser;
		this.netViewFactory = netViewFactory;
		this.rootNetworkMgr = rootNetworkMgr;
		this.swingApp = swingApp;
		this.networkFactory = networkFactory;
		this.vmmServiceRef = vmmServiceRef;
		this.visualStyleFactoryServiceRef = visualStyleFactoryServiceRef;
		this.vmfFactoryC = vmfFactoryC;
		this.vmfFactoryD = vmfFactoryD;
		this.vmfFactoryP = vmfFactoryP;
		this.cyLayoutManager = cyLayoutManager;
	}
	
	/**
	 * tests if the main panel is opened or not
	 * @return true if opened, false if not
	 */
	public boolean isMainOpened(){
		if (this.getMainPanel()==null){
			return false;
		}
		return true;
	}
	
	/**
	 * gets the main HyperModules panel in the control pane
	 * @return MainPanel
	 */
	public MainPanel getMainPanel(){
		CytoPanel cytoPanel = swingApp.getCytoPanel(CytoPanelName.WEST);
		int count = cytoPanel.getCytoPanelComponentCount();

		for (int i = 0; i < count; i++) {
			if (cytoPanel.getComponentAt(i) instanceof MainPanel)
				return (MainPanel) cytoPanel.getComponentAt(i);
		}
		
		return null;
		
	}
	
	/**
	 * tests if the results panel is opened or not
	 * @return true if opened, false if not
	 */
	public boolean isResultOpened(){
		if (this.getResultsPanel()==null){
			return false;
		}
		
		return true;
		
	}
	
	/**
	 * fetches results panels
	 * @return CytoPanelEAST
	 */
	public CytoPanel getCytoPanelEast(){
		return swingApp.getCytoPanel(CytoPanelName.EAST);
	}
	
	
	/**
	 * fetches a result panel
	 * @return ResultsPanel
	 */
	public ResultsPanel getResultsPanel(){
		CytoPanel cytoPanel = swingApp.getCytoPanel(CytoPanelName.EAST);
		int count = cytoPanel.getCytoPanelComponentCount();

		for (int i = 0; i < count; i++) {
			if (cytoPanel.getComponentAt(i) instanceof ResultsPanel)
				return (ResultsPanel) cytoPanel.getComponentAt(i);
		}
		
		return null;
	}
	
	/**
	 * fetches all opened results panels
	 * @return HashSet<ResultsPanel> rph
	 */
	public HashSet<ResultsPanel> getAllResultsPanels(){
		CytoPanel cytoPanel = swingApp.getCytoPanel(CytoPanelName.EAST);
		int count = cytoPanel.getCytoPanelComponentCount();
		HashSet<ResultsPanel> rph = new HashSet<ResultsPanel>();
		
		for (int i = 0; i < count; i++) {
			if (cytoPanel.getComponentAt(i) instanceof ResultsPanel)
				rph.add((ResultsPanel) cytoPanel.getComponentAt(i));
		}
		
		return rph;
		
	}
	
	/**
	 * discards a result panel
	 */
	public void discardResults(ResultsPanel rp){
		serviceRegistrar.unregisterService(rp, CytoPanelComponent.class);
	}
	
	
	
}
