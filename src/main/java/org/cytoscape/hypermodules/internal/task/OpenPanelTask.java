package org.cytoscape.hypermodules.internal.task;

import java.util.Properties;

import javax.swing.BorderFactory;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.hypermodules.internal.gui.MainPanel;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

/**
 * 
 * Opens the main hypermodules panel in the left control panel, if it is not already opened.
 * @author alvinleung
 *
 */
public class OpenPanelTask implements Task{

	private CytoscapeUtils utils;
	private CySwingApplication swingApp;
	
	public OpenPanelTask( CySwingApplication swingApp, CytoscapeUtils utils){
		this.utils = utils;
		this.swingApp = swingApp;
	}
	
	
	
	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}
	

	@Override
	public void run(TaskMonitor monitor) throws Exception {
		synchronized (this){
			if (!utils.isMainOpened()){
				MainPanel mainPanel = new MainPanel(swingApp, utils);
				utils.serviceRegistrar.registerService(mainPanel, CytoPanelComponent.class, new Properties());
				mainPanel.setBorder(BorderFactory.createRaisedBevelBorder());
			}
			
			/*
			CytoPanel cytoPanel = swingApp.getCytoPanel(CytoPanelName.WEST);
			int index = cytoPanel.indexOfComponent(mainPanel);
			cytoPanel.setSelectedIndex(index);
			*/
		}
	}

}
