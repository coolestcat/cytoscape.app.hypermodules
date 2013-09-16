package org.cytoscape.hypermodules.internal.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
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

/**
 * A JPanel for selecting the network to run the algorithm on
 * @author user
 *
 */
public class NetworkSelectionPanel extends JPanel implements NetworkAddedListener, NetworkDestroyedListener {

	
	private static final long serialVersionUID = 1L;
	private JComboBox comboBox;
	private CytoscapeUtils utils;
	
	/**
	 * constructor
	 * @param utils
	 */
	public NetworkSelectionPanel(CytoscapeUtils utils){
		super();
		this.utils = utils;
		this.comboBox = new JComboBox();
		//comboBox.setPreferredSize(new java.awt.Dimension(comboBox.getPreferredSize().width, 
		//		comboBox.getPreferredSize().height));
		add(comboBox, BorderLayout.CENTER);
		comboBox.setPreferredSize(new Dimension(200, 23));
		comboBox.setMaximumSize(new Dimension(200, 23));
		updateNetworkList();
	}
	
	/**
	 * update the list of networks in the JComboBox
	 */
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
	
	/**
	 * @return CyNetwork associated with the selected name
	 */
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

	/**
	 * handles network destroy events - updates network list in JComboBox
	 */
	@Override
	public void handleEvent(NetworkDestroyedEvent nde) {
		System.out.println("network destroyed!");
		updateNetworkList();
	}

	/**
	 * handles network addition events - updates network list in JComboBox
	 */
	@Override
	public void handleEvent(NetworkAddedEvent nae) {
		System.out.println("network added!");
		updateNetworkList();
	}

	
}
