package org.cytoscape.hypermodules.internal;

import java.util.HashSet;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
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
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskManager;

public class CytoscapeUtils {
	
	public CyApplicationManager appMgr;
	public TaskManager<?,?> taskMgr;
	public CyNetworkViewManager netViewMgr;
	public CyNetworkManager netMgr;
	public CyServiceRegistrar serviceRegistrar;
	public CyEventHelper eventHelper;
	public CyNetworkNaming networkNaming;
	public FileUtil fileUtil;
	public OpenBrowser openBrowser;
	public CyNetworkViewFactory netViewFactory;
	public CyRootNetworkManager rootNetworkMgr;
	public CySwingApplication swingApp;
	public CyNetworkFactory networkFactory;
	
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
			CyNetworkFactory networkFactory){
		
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
		
		
	}
	
	
	public boolean isMainOpened(){
		if (this.getMainPanel()==null){
			return false;
		}
		return true;
		/*CytoPanel cytoPanel = swingApp.getCytoPanel(CytoPanelName.WEST);
		int count = cytoPanel.getCytoPanelComponentCount();

		for (int i = 0; i < count; i++) {
			if (cytoPanel.getComponentAt(i) instanceof MainPanel)
				return true;
		}

		return false;
		*/
	}
	
	public MainPanel getMainPanel(){
		CytoPanel cytoPanel = swingApp.getCytoPanel(CytoPanelName.WEST);
		int count = cytoPanel.getCytoPanelComponentCount();

		for (int i = 0; i < count; i++) {
			if (cytoPanel.getComponentAt(i) instanceof MainPanel)
				return (MainPanel) cytoPanel.getComponentAt(i);
		}
		
		return null;
		
	}
	
	
	public boolean isResultOpened(){
		if (this.getResultsPanel()==null){
			return false;
		}
		
		return true;
		
	}
	
	public CytoPanel getCytoPanelEast(){
		return swingApp.getCytoPanel(CytoPanelName.EAST);
	}
	
	
	public ResultsPanel getResultsPanel(){
		CytoPanel cytoPanel = swingApp.getCytoPanel(CytoPanelName.EAST);
		int count = cytoPanel.getCytoPanelComponentCount();

		for (int i = 0; i < count; i++) {
			if (cytoPanel.getComponentAt(i) instanceof ResultsPanel)
				return (ResultsPanel) cytoPanel.getComponentAt(i);
		}
		
		return null;
	}
	
	
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
	
	public void discardResults(){
		
	}
	
	
	
}
