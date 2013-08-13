package org.cytoscape.hypermodules.internal.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

public class VisualizeNetworkTask implements Task {

	private CytoscapeUtils utils;
	private String input;
	private CyNetwork runNetwork;
	
	public VisualizeNetworkTask(CytoscapeUtils utils, String s){
		this.utils = utils;
		this.input = s;
	}
	
	
	@Override
	public void cancel() {

	}

	@SuppressWarnings("unused")
	@Override
	public void run(TaskMonitor tm) throws Exception {
		System.out.println("visualizing network...");
		this.runNetwork = utils.appMgr.getCurrentNetwork();
		String[] inputStrings = this.input.split(",");
		HashMap<String, CyNode> inputNodes = new HashMap<String, CyNode>();
		List<CyNode> cc = runNetwork.getNodeList();
		CyNode seed = null;
		for (int k=0; k<cc.size(); k++){
			if (inputStrings[0].equals(this.runNetwork.getRow(cc.get(k)).get(CyNetwork.NAME, String.class))){
				seed = cc.get(k);
			}
		}
		
		long seedID = seed.getSUID();
		String seedString = runNetwork.getRow(seed).get(CyNetwork.NAME, String.class);
		
		/*
		for(int i=0; i<inputStrings.length; i++){
			for (int k=0; k<cc.size(); k++){
				if (inputStrings[i].equals(runNetwork.getRow(cc.get(k)).get(CyNetwork.NAME, String.class))){
					inputNodes.put(inputStrings[i], cc.get(i));
				}
			}
		}
		*/
			
		if (seed == null){
			return;
		}
		
		HashSet<String> inputHash = new HashSet<String>();
		for (int i=0; i<inputStrings.length; i++){
			inputHash.add(inputStrings[i]);
		}
		
		for (String s : inputHash){
			System.out.println(s);
		}
		System.out.println("|");
		
		CyNetwork generated = utils.networkFactory.createNetwork();
		HashSet<String> myNetNames = new HashSet<String>();
		seed = generated.addNode();
		generated.getRow(seed).set(CyNetwork.NAME, seedString);
		long mySeedID = seed.getSUID();
		myNetNames.add(seedString);
		seed = runNetwork.getNode(seedID);
		
		for (CyEdge edge : runNetwork.getAdjacentEdgeList(seed, CyEdge.Type.ANY)){
			seed = generated.getNode(mySeedID);
			CyNode target = edge.getTarget();
			long targetID = target.getSUID();
			String targetString = runNetwork.getRow(target).get(CyNetwork.NAME, String.class);

			if (inputHash.contains(targetString)){
				if(!myNetNames.contains(targetString)){
					target = generated.addNode();
					generated.getRow(target).set(CyNetwork.NAME, targetString);
					myNetNames.add(targetString);
					generated.addEdge(seed, target, true);
					long myTargetID = target.getSUID();
					target = runNetwork.getNode(targetID);
					
					for (CyEdge innerEdge : runNetwork.getAdjacentEdgeList(target, CyEdge.Type.ANY)){
					CyNode innerTarget = innerEdge.getTarget();
					String innerTargetName = runNetwork.getRow(innerTarget).get(CyNetwork.NAME, String.class);
					
						if (inputHash.contains(innerTargetName)){
							target = generated.getNode(myTargetID);
							if (!myNetNames.contains(innerTargetName)){
								innerTarget = generated.addNode();
								generated.addEdge(target, innerTarget, true);
								generated.getRow(innerTarget).set(CyNetwork.NAME, innerTargetName);
								myNetNames.add(innerTargetName);
							}
							else{
								for (CyNode e : generated.getNodeList()){
									if (innerTargetName.equals(generated.getRow(e).get(CyNetwork.NAME, String.class))){
										innerTarget = e;
									}
								}
								
								if (!generated.containsEdge(target, innerTarget)){
									generated.addEdge(target, innerTarget, true);
								}
							}
							target = runNetwork.getNode(targetID);
						}
					}
				}
				else{
					for (CyNode c : generated.getNodeList()){
						if (targetString.equals(generated.getRow(c).get(CyNetwork.NAME, String.class))){
							target = c;
						}
					}
	
						long myTargetID = target.getSUID();
						if (!generated.containsEdge(seed, target)){
							generated.addEdge(seed, target, true);
						}
						target = runNetwork.getNode(targetID);
						
					for (CyEdge innerEdge : runNetwork.getAdjacentEdgeList(target, CyEdge.Type.ANY)){
						CyNode innerTarget = innerEdge.getTarget();
						String innerTargetName = runNetwork.getRow(innerTarget).get(CyNetwork.NAME, String.class);
						
						
						if(inputHash.contains(innerTargetName)){
							target = generated.getNode(myTargetID);
							if (!myNetNames.contains(innerTargetName)){
								innerTarget = generated.addNode();
								generated.addEdge(target, innerTarget, true);
								generated.getRow(innerTarget).set(CyNetwork.NAME, innerTargetName);
								myNetNames.add(innerTargetName);
							}
							else{
								for (CyNode d : generated.getNodeList()){
									if (innerTargetName.equals(generated.getRow(d).get(CyNetwork.NAME, String.class))){
										innerTarget = d;
									}
								}
								if (!generated.containsEdge(target,innerTarget)){
									generated.addEdge(target, innerTarget, true);
								}
								
							}
							target = runNetwork.getNode(targetID);
						}
					}
					
				}
			}
			seed = runNetwork.getNode(seedID);
		}
		
		generated.getDefaultNetworkTable().getRow(generated.getSUID()).set("name", this.utils.networkNaming.getSuggestedNetworkTitle("Visualization"));
		this.utils.netMgr.addNetwork(generated);
		CyNetworkView myView = this.utils.netViewFactory.createNetworkView(generated);
		this.utils.netViewMgr.addNetworkView(myView);
		
	}
}
