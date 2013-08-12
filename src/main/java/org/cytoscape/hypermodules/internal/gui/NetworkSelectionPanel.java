package org.cytoscape.hypermodules.internal.gui;

import java.awt.BorderLayout;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.NetworkDestroyedEvent;
import org.cytoscape.model.events.NetworkDestroyedListener;

public class NetworkSelectionPanel extends JPanel implements NetworkAddedListener, NetworkDestroyedListener {

	
	private static final long serialVersionUID = 1L;
	private JComboBox comboBox;
	private CytoscapeUtils utils;
	
	public NetworkSelectionPanel(CytoscapeUtils utils){
		super();
		this.utils = utils;
		this.comboBox = new JComboBox();
		//comboBox.setPreferredSize(new java.awt.Dimension(comboBox.getPreferredSize().width, 
		//		comboBox.getPreferredSize().height));
		add(comboBox, BorderLayout.CENTER);
		updateNetworkList();
	}
	
	private void updateNetworkList() {
		final Set<CyNetwork> networks = utils.netMgr.getNetworkSet();
		final SortedSet<String> networkNames = new TreeSet<String>();

		for (CyNetwork net : networks)
			networkNames.add(net.getRow(net).get("name", String.class));

		// Clear the comboBox
		comboBox.setModel(new DefaultComboBoxModel());

		for (String name : networkNames)
			comboBox.addItem(name);

		CyNetwork currNetwork = utils.appMgr.getCurrentNetwork();
		if (currNetwork != null) {
			String networkTitle = currNetwork.getRow(currNetwork).get("name", String.class);
			comboBox.setSelectedItem(networkTitle);			
		}
	}
	
	public CyNetwork getSelectedNetwork() {
		for (CyNetwork net : utils.netMgr.getNetworkSet()) {
			String networkTitle = net.getRow(net).get("name", String.class);
			if (networkTitle.equals(comboBox.getSelectedItem()))
				return net;
		}

		return null;
	}
	
	public JComboBox getJCombobox(){
		return this.comboBox;
	}

	@Override
	public void handleEvent(NetworkDestroyedEvent nde) {
		updateNetworkList();
	}

	@Override
	public void handleEvent(NetworkAddedEvent nae) {
		updateNetworkList();
	}

	
}
