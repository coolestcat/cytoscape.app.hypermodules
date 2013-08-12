package org.cytoscape.hypermodules.internal;

import static org.cytoscape.work.ServiceProperties.ACCELERATOR;
import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.ENABLE_FOR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.hypermodules.internal.task.CloseTaskFactory;
import org.cytoscape.hypermodules.internal.task.OpenAboutTaskFactory;
import org.cytoscape.hypermodules.internal.task.OpenPanelTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;


import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.OpenBrowser;


import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.RenderingEngineFactory;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskManager;
import org.cytoscape.session.CyNetworkNaming;
import org.osgi.framework.BundleContext;


public class CyActivator extends AbstractCyActivator {

	public CyActivator() {
		super();
	}

	@Override
	public void start(BundleContext bc) throws Exception {
		
		CyApplicationManager appMgr = getService(bc,CyApplicationManager.class);
		TaskManager<?, ?> taskMgr = getService(bc, TaskManager.class);
		
		CyNetworkViewManager netViewMgr = getService(bc, CyNetworkViewManager.class);
		CyNetworkManager netMgr = getService(bc, CyNetworkManager.class);
		
		CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);
		CyEventHelper eventHelper = getService(bc, CyEventHelper.class);
		
		CyNetworkNaming networkNaming = getService(bc,CyNetworkNaming.class);
		
		FileUtil fileUtil = getService(bc, FileUtil.class);
		OpenBrowser openBrowser = getService(bc, OpenBrowser.class);
		
		CyNetworkViewFactory netViewFactory = getService(bc, CyNetworkViewFactory.class);
		CyRootNetworkManager rootNetworkMgr = getService(bc, CyRootNetworkManager.class);
		
		CySwingApplication swingApp = getService(bc, CySwingApplication.class);
		
		CyNetworkFactory networkFactory = getService(bc, CyNetworkFactory.class);
		
		CytoscapeUtils utils = new CytoscapeUtils(appMgr, taskMgr, netViewMgr, netMgr, serviceRegistrar, eventHelper, networkNaming, fileUtil, openBrowser, netViewFactory, rootNetworkMgr, swingApp, networkFactory);
		
		OpenPanelTaskFactory openTaskFactory = new OpenPanelTaskFactory(swingApp, utils);
		Properties openTaskFactoryProps = new Properties();
		openTaskFactoryProps.setProperty("preferredMenu", "Apps.HyperModules");
		openTaskFactoryProps.setProperty("title", "Open");
		openTaskFactoryProps.setProperty("menuGravity","1.0");
		
		registerService(bc, openTaskFactory, TaskFactory.class, openTaskFactoryProps);
		
		CloseTaskFactory closeTaskFactory = new CloseTaskFactory(utils);
		Properties closeTaskFactoryProps = new Properties();
		closeTaskFactoryProps.setProperty("preferredMenu", "Apps.HyperModules");
		closeTaskFactoryProps.setProperty("title", "Close");
		closeTaskFactoryProps.setProperty("menuGravity","2.0");
		
		registerService(bc, closeTaskFactory, TaskFactory.class, closeTaskFactoryProps);
		
		OpenAboutTaskFactory openAboutFactory = new OpenAboutTaskFactory(utils);
		Properties openAboutFactoryProps = new Properties();
		openAboutFactoryProps.setProperty("preferredMenu", "Apps.HyperModules");
		openAboutFactoryProps.setProperty("title", "About");
		openAboutFactoryProps.setProperty("menuGravity","3.0");
		
		registerService(bc, openAboutFactory, TaskFactory.class, openAboutFactoryProps);
	}

}
