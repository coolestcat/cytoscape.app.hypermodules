package org.cytoscape.hypermodules.internal.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

public class GenerateNetworkTask implements Task{

	private HashMap<String, ArrayList<HashMap<String, Double>>> generation;
	private CyNetwork network;
	private CytoscapeUtils utils;
	
	public GenerateNetworkTask(HashMap<String, ArrayList<HashMap<String, Double>>> generation, CyNetwork originalNetwork, CytoscapeUtils utils){
		this.generation = generation;
		this.network = originalNetwork;
		this.utils = utils;
	}
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		//TODO: configure taskMonitor
		HashMap<String, CyNode> nameAndNode = new HashMap<String, CyNode>();
		for (CyNode nameNode : this.network.getNodeList()){
			if (generation.keySet().contains(this.network.getRow(nameNode).get(CyNetwork.NAME, String.class))){
				nameAndNode.put(this.network.getRow(nameNode).get(CyNetwork.NAME, String.class), nameNode);
			}
		}
		
		
		for (String seedNode : generation.keySet()){
			ArrayList<HashMap<String, Double>> ahsd = generation.get(seedNode);
			HashMap<String, Double> firstMap = ahsd.get(0);
			CyNode seed = nameAndNode.get(seedNode);
			
			for (String start : firstMap.keySet()){
				String[] deconstructed = start.split(":");
				HashSet<String> oneNetwork = new HashSet<String>();
				for (int k=0; k<deconstructed.length; k++){
					oneNetwork.add(deconstructed[k]);
				}
				CyNetwork myNet = utils.networkFactory.createNetwork();
				HashSet<String> myNetNames = new HashSet<String>();
				seed = myNet.addNode();
				long id = seed.getSUID();
				myNet.getRow(seed).set(CyNetwork.NAME, seedNode);
				seed = nameAndNode.get(seedNode);
				myNetNames.add(seedNode);
				
				for (CyEdge edge : this.network.getAdjacentEdgeList(seed, CyEdge.Type.ANY)){
					seed = myNet.getNode(id);
					CyNode target = edge.getTarget();
					long targetID = target.getSUID();
					String targetName = this.network.getRow(target).get(CyNetwork.NAME, String.class);
					if (oneNetwork.contains(targetName) && !myNetNames.contains(targetName)){
						target = myNet.addNode();
						myNet.addEdge(seed, target, true);
						myNet.getRow(target).set(CyNetwork.NAME, targetName);
						long myNetTargetID = target.getSUID();
						myNetNames.add(targetName);
						target = this.network.getNode(targetID);
						for (CyEdge edge2 : this.network.getAdjacentEdgeList(target, CyEdge.Type.ANY)){
							CyNode target2 = edge2.getTarget();
							String targetName2 = this.network.getRow(target2).get(CyNetwork.NAME, String.class);

							if (oneNetwork.contains(targetName2) && !myNetNames.contains(targetName2)){
									target2 = myNet.addNode();
									target = myNet.getNode(myNetTargetID);
									myNet.addEdge(target, target2, true);
									myNet.getRow(target2).set(CyNetwork.NAME, targetName2);
									myNetNames.add(targetName2);
							}
							target = this.network.getNode(targetID);
						}
					}


				}

				myNet.getDefaultNetworkTable().getRow(myNet.getSUID()).set("name", this.utils.networkNaming.getSuggestedNetworkTitle("Correlated Module Expanded From " + seedNode + " with P Value " + firstMap.get(oneNetwork)));
				this.utils.netMgr.addNetwork(myNet);
				CyNetworkView myView = this.utils.netViewFactory.createNetworkView(myNet);

				this.utils.netViewMgr.addNetworkView(myView);
			}
		}
	
	}

	@Override
	public void cancel() {
		
	}

}
