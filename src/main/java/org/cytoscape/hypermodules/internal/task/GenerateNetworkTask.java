package org.cytoscape.hypermodules.internal.task;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

/**
 * 
 * Called by ResultsPanel. This class generates a new network view in cytoscape which has all the most correlated
 * modules arranged in a spring-embedded layout by taking as input the string forms of the modules
 * and expanding from the seed to obtain the edges
 * @author alvinleung
 *
 */
public class GenerateNetworkTask extends AbstractTask implements Task{

	private String[] sas;
	private CyNetwork runNetwork;
	private CyNetwork generated;
	private CytoscapeUtils utils;
	private ArrayList<String[]> sampleValues;
	private HashSet<String> allSeeds;
	private HashMap<String, Double> allSamplesMap;
	private HashSet<String> inputHash;
	
	public GenerateNetworkTask(String[] sas, CyNetwork originalNetwork, CytoscapeUtils utils, ArrayList<String[]> sampleValues){
		this.sas = sas;
		this.runNetwork = originalNetwork;
		this.utils = utils;
		this.sampleValues = sampleValues;
		this.allSeeds = new HashSet<String>();
		for (int i=0; i<sampleValues.size(); i++){
			if (!sampleValues.get(i)[1].equals("no_sample")){
				allSeeds.add(sampleValues.get(i)[0]);
			}
		}
		this.allSamplesMap = new HashMap<String, Double>();
		
		HashMap<String, String> genes2samples = new HashMap<String, String>();
		
		for (int i=0; i<sampleValues.size(); i++){
			if (genes2samples.get(sampleValues.get(i)[0])!=null){
				String sti = genes2samples.get(sampleValues.get(i)[0]);
				sti = sti + ":" + sampleValues.get(i)[1];
				genes2samples.put(sampleValues.get(i)[0], sti);
			}
			else{
			//System.out.println(sampleValues.get(i)[1]);
			genes2samples.put(sampleValues.get(i)[0], sampleValues.get(i)[1]);
			}
		}
		
		for (String s : genes2samples.keySet()){
			String d = genes2samples.get(s);
			String[] st = d.split(":");
			allSamplesMap.put(s, (double) st.length);
		}
	}
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		this.generated = utils.networkFactory.createNetwork();
		//visualize(sas[0], sas[1]);
		generate(sas[0], sas[1]);
		applyVisualStyle();
	}
	
	public void applyVisualStyle(){
		
		generated.getDefaultNetworkTable().getRow(generated.getSUID()).set("name", this.utils.networkNaming.getSuggestedNetworkTitle(sas[1]));
		this.utils.netMgr.addNetwork(generated);
		CyNetworkView myView = this.utils.netViewFactory.createNetworkView(generated);
		this.utils.netViewMgr.addNetworkView(myView);
		
		VisualStyle vs = utils.visualStyleFactoryServiceRef.createVisualStyle("My Visual Style");

		String ctrAttrName1 = "name";
		PassthroughMapping<String, ?> pMapping = (PassthroughMapping<String, ?>) utils.vmfFactoryP.createVisualMappingFunction(ctrAttrName1, String.class, BasicVisualLexicon.NODE_LABEL);
		vs.addVisualMappingFunction(pMapping);
		//vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.orange);
		for (CyNode c : generated.getNodeList()){
			if (generated.getRow(c).get(CyNetwork.NAME,  String.class).equals(sas[0])){
				View<CyNode> v = myView.getNodeView(c);
				v.setLockedValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.DIAMOND);
			}
			if (allSeeds.contains(generated.getRow(c).get(CyNetwork.NAME, String.class))){
				View<CyNode> v = myView.getNodeView(c);
				v.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.red);
				double d = 50.0;
				if (allSamplesMap.get(generated.getRow(c).get(CyNetwork.NAME, String.class))!=null){
					v.setLockedValue(BasicVisualLexicon.NODE_SIZE, (d + 2*allSamplesMap.get(generated.getRow(c).get(CyNetwork.NAME, String.class))));
				}
			}
			else{
				View<CyNode> v = myView.getNodeView(c);
				v.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.gray);
			}
		}
		utils.vmmServiceRef.addVisualStyle(vs);
		vs.apply(myView);
		myView.updateView();
		
		//CyLayoutAlgorithm layout = utils.cyLayoutManager.getLayout("attribute-circle");

		CyLayoutAlgorithm layout = utils.cyLayoutManager.getLayout("force-directed");
		String layoutAttribute = null;
		insertTasksAfterCurrentTask(layout.createTaskIterator(myView, layout.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, layoutAttribute));
		
	}
	
	
	public void generate(String s, String t){
		//filter unique strings:
		String[] inputStrings = t.split(":|;");
		
		this.inputHash = new HashSet<String>();
		for (int i=0; i<inputStrings.length; i++){
			inputHash.add(inputStrings[i]);
		}
		
		//TODO: changed
		//inputHash.remove(s);

		//visualizeRecursive(s, null, 3);
		newVisualize();
		
	}
	
	
	public void newVisualize(){
		List<CyNode> cc = runNetwork.getNodeList();
		HashSet<CyNode> inNodes = new HashSet<CyNode>();
		
		for (int k=0; k<cc.size(); k++){
			CyNode nod = null;
			if (inputHash.contains(this.runNetwork.getRow(cc.get(k)).get(CyNetwork.NAME, String.class))){
				nod = cc.get(k);
				inNodes.add(nod);
			}
		}
		
		
		for (CyNode c : inNodes){
			String curNodeName = this.runNetwork.getRow(c).get(CyNetwork.NAME, String.class);
			long curNodeID = c.getSUID();
			c = generated.addNode();
			generated.getRow(c).set(CyNetwork.NAME, curNodeName);
			c = runNetwork.getNode(curNodeID);
		}
		
		for (CyNode c : inNodes){
			
			String curNodeName = this.runNetwork.getRow(c).get(CyNetwork.NAME, String.class);
			
			for (CyEdge edge : runNetwork.getAdjacentEdgeList(c, CyEdge.Type.ANY)){
				
				//nodes in generated
				List<CyNode> mycc = generated.getNodeList();
				
				CyNode target = edge.getTarget();
				String targetString = runNetwork.getRow(target).get(CyNetwork.NAME, String.class);
				
				if (inputHash.contains(targetString) && !curNodeName.equals(targetString)){
					CyNode cInGenerated = null;
					CyNode foundTarget = null;
					
					for (int k=0; k<mycc.size(); k++){
						if (curNodeName.equals(generated.getRow(mycc.get(k)).get(CyNetwork.NAME, String.class))){
							cInGenerated = mycc.get(k);
						}
						if (targetString.equals(generated.getRow(mycc.get(k)).get(CyNetwork.NAME, String.class))){
							foundTarget = mycc.get(k);
						}
					}
					
					if(!generated.containsEdge(cInGenerated, foundTarget)){
						generated.addEdge(cInGenerated, foundTarget, true);
					}
				}
				
				
				
				target = edge.getSource();
				targetString = runNetwork.getRow(target).get(CyNetwork.NAME, String.class);
				
				if (inputHash.contains(targetString) && !curNodeName.equals(targetString)){
					CyNode cInGenerated = null;
					CyNode foundTarget = null;
					
					for (int k=0; k<mycc.size(); k++){
						if (curNodeName.equals(generated.getRow(mycc.get(k)).get(CyNetwork.NAME, String.class))){
							cInGenerated = mycc.get(k);
						}
						if (targetString.equals(generated.getRow(mycc.get(k)).get(CyNetwork.NAME, String.class))){
							foundTarget = mycc.get(k);
						}
					}
					
					if(!generated.containsEdge(cInGenerated, foundTarget)){
						generated.addEdge(cInGenerated, foundTarget, true);
					}
				}

				
			}

			
		}
		
	}
	
	public void visualizeRecursive(String s, String parent, int level){
		
		
		
		//get Seed node to start expanding
		List<CyNode> cc = runNetwork.getNodeList();
		CyNode seed = null;
		for (int k=0; k<cc.size(); k++){
			if (s.equals(this.runNetwork.getRow(cc.get(k)).get(CyNetwork.NAME, String.class))){
				seed = cc.get(k);
			}
		}
		
		//get seed SUID
		long seedID = seed.getSUID();
		String seedString = s;
		
		seed = generated.addNode(); //add seed to generated
		generated.getRow(seed).set(CyNetwork.NAME, seedString);
		long mySeedID = seed.getSUID(); //save SUID of seed in my network
		
		//add edge from this node to parent

		if (parent!=null){
			CyNode par = null;
			List<CyNode> gg = generated.getNodeList();
			//find parent in generated
			for (int k=0; k<gg.size(); k++){
				if (parent.equals(generated.getRow(gg.get(k)).get(CyNetwork.NAME, String.class))){
					par = gg.get(k);
				}
			}
			generated.addEdge(par, seed, true);
		}
	
		seed = runNetwork.getNode(seedID); //seed = seed of original network
		
		//expand one layer away:
		for (CyEdge edge : runNetwork.getAdjacentEdgeList(seed, CyEdge.Type.ANY)){
			CyNode seedHolder = generated.getNode(mySeedID);
			
			//target node info
			CyNode target = edge.getTarget();
			//long targetID = target.getSUID();
			String targetString = runNetwork.getRow(target).get(CyNetwork.NAME, String.class);
			//System.out.println(seedString + " : " + targetString);
			
			if (inputHash.contains(targetString)){
				inputHash.remove(targetString); //remove from global hashset
				
				if (level == 0){
					target = generated.addNode();
					generated.addEdge(seedHolder, target, true);
					generated.getRow(target).set(CyNetwork.NAME, targetString);
					//System.out.println("Adding Terminal Nodes: " + targetString);
				}
				else{
					//System.out.println("Recursing on: " + targetString);
					visualizeRecursive(targetString, seedString, level-1);	
				}
			}
			//System.out.println("||");
			//source node info
			target = edge.getSource();
			//long targetID = target.getSUID();
			targetString = runNetwork.getRow(target).get(CyNetwork.NAME, String.class);
			//System.out.println(seedString + " : " + targetString);
			
			if (inputHash.contains(targetString)){
				inputHash.remove(targetString); //remove from global hashset
				
				if (level == 0){
					target = generated.addNode();
					generated.addEdge(seedHolder, target, true);
					generated.getRow(target).set(CyNetwork.NAME, targetString);
					//System.out.println("Adding Terminal Nodes: " + targetString);
				}
				else{
					//System.out.println("Recursing on: " + targetString);
					visualizeRecursive(targetString, seedString, level-1);	
				}
			}
			
			
		}
		
		//System.out.println("|||||||||||||");
		
	}

	public void visualize(String s, String t){
		System.out.println("visualizing network...");

		String[] inputStrings = t.split(":");
		HashMap<String, CyNode> inputNodes = new HashMap<String, CyNode>();
		List<CyNode> cc = runNetwork.getNodeList();
		CyNode seed = null;
		for (int k=0; k<cc.size(); k++){
			if (s.equals(this.runNetwork.getRow(cc.get(k)).get(CyNetwork.NAME, String.class))){
				seed = cc.get(k);
			}
		}
		
		long seedID = seed.getSUID();
		String seedString = runNetwork.getRow(seed).get(CyNetwork.NAME, String.class);
		
		HashSet<String> inputHash = new HashSet<String>();
		for (int i=0; i<inputStrings.length; i++){
			inputHash.add(inputStrings[i]);
		}
		
		

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

	}
	@Override
	public void cancel() {
		
	}

}
