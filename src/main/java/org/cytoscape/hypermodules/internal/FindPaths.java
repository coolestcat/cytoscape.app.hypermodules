package org.cytoscape.hypermodules.internal;

import gnu.trove.set.hash.TIntHashSet;

import java.util.*;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyEdge;
/**
 * 
 * Class that goes through the current network and finds all paths of length two from the given seed node.
 * @author alvinleung
 *
 */
public class FindPaths {

	private CyNetwork net;
	private int level;
	
	/**
	 * Constructor
	 * @param network the current network in cytoscape
	 * @param level level of recursion
	 */
	public FindPaths(CyNetwork network, int level){
		this.net = network;
		this.level = level;
		
	}

	public HashSet<HashSet<String>> getAllPaths(CyNode seed){
		HashSet<HashSet<String>> ret = new HashSet<HashSet<String>>();
		String nameOfSeed = "default";
		nameOfSeed = this.net.getRow(seed).get(CyNetwork.NAME, String.class);

		for (CyNode node : this.net.getNeighborList(seed, CyEdge.Type.ANY)){
			if (!this.net.getNeighborList(node, CyEdge.Type.ANY).isEmpty()){
				for (CyNode node2 : this.net.getNeighborList(node, CyEdge.Type.ANY)){
					HashSet<String> thisPath = new HashSet<String>();
					thisPath.add(this.net.getRow(node2).get(CyNetwork.NAME, String.class));
					thisPath.add(this.net.getRow(node).get(CyNetwork.NAME, String.class));
					thisPath.add(nameOfSeed);
					ret.add(thisPath);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * A HashSet is used so only unique genes in path added. This method is for getting paths of length 1 from the seed
	 * @param seed the seed node
	 * @return hashset of string representing the names of all the genes in all the paths of length 1 from the seed node
	 */
	
	public HashSet<String> getAllPaths1(CyNode seed){
		HashSet<HashSet<String>> set = new HashSet<HashSet<String>>();
		String nameOfSeed = "default";
		nameOfSeed = this.net.getRow(seed).get(CyNetwork.NAME, String.class);

		for (CyNode node : this.net.getNeighborList(seed, CyEdge.Type.ANY)){
					HashSet<String> thisPath = new HashSet<String>();
					thisPath.add(this.net.getRow(node).get(CyNetwork.NAME, String.class));
					thisPath.add(nameOfSeed);
					set.add(thisPath);
				
			
		}
		HashSet<String> ret = new HashSet<String>();
		for (HashSet<String> setElement : set){
			String allString = new String();
			allString = "";
			int i=0;
			for (String element : setElement){
				if (i==setElement.size()-1){
				allString = allString + element;
				}
				else{
				allString = allString + element + ":";
				}
				i++;
			}
			ret.add(allString);
		}
		return ret;
		
		
	}
	/**
	 * A HashSet is used so only unique genes in path added. This method is for getting paths of length 2 from the seed
	 * @param seed the seed node
	 * @return hashset of string representing the names of all the genes in all the paths of length 2 from the seed node
	 */
	
	public HashSet<String> getAllPaths2(CyNode seed){
		HashSet<HashSet<String>> set = new HashSet<HashSet<String>>();
		String nameOfSeed = "default";
		nameOfSeed = this.net.getRow(seed).get(CyNetwork.NAME, String.class);

		for (CyNode node : this.net.getNeighborList(seed, CyEdge.Type.ANY)){
			if (!this.net.getNeighborList(node, CyEdge.Type.ANY).isEmpty()){
				for (CyNode node2 : this.net.getNeighborList(node, CyEdge.Type.ANY)){
					HashSet<String> thisPath = new HashSet<String>();
					thisPath.add(this.net.getRow(node2).get(CyNetwork.NAME, String.class));
					thisPath.add(this.net.getRow(node).get(CyNetwork.NAME, String.class));
					thisPath.add(nameOfSeed);
					set.add(thisPath);
				}
			}
		}
		HashSet<String> ret = new HashSet<String>();
		for (HashSet<String> setElement : set){
			String allString = new String();
			allString = "";
			int i=0;
			for (String element : setElement){
				if (i==setElement.size()-1){
				allString = allString + element;
				}
				else{
				allString = allString + element + ":";
				}
				i++;
			}
			ret.add(allString);
		}
		return ret;
		
	}
	
	/**
	 * A HashSet is used so only unique genes in path added. This method is for getting paths of length 2 from the seed
	 * This is experimenting with using a TIntHashSet instead of a Hashset of String - it didn't make the algorithm a lot faster
	 * @param seed the seed node
	 * @return hashset of string representing the names of all the genes in all the paths of length 2 from the seed node
	 */
	
	public HashSet<TIntHashSet> getAllPaths3(CyNode seed){
		HashSet<HashSet<String>> set = new HashSet<HashSet<String>>();
		String nameOfSeed = "default";
		nameOfSeed = this.net.getRow(seed).get(CyNetwork.NAME, String.class);

		for (CyNode node : this.net.getNeighborList(seed, CyEdge.Type.ANY)){
			if (!this.net.getNeighborList(node, CyEdge.Type.ANY).isEmpty()){
				for (CyNode node2 : this.net.getNeighborList(node, CyEdge.Type.ANY)){
					HashSet<String> thisPath = new HashSet<String>();
					thisPath.add(this.net.getRow(node2).get(CyNetwork.NAME, String.class));
					thisPath.add(this.net.getRow(node).get(CyNetwork.NAME, String.class));
					thisPath.add(nameOfSeed);
					set.add(thisPath);
				}
			}
		}
		
		HashSet<TIntHashSet> ret = new HashSet<TIntHashSet>();
		/*
		for (HashSet<String> setElement : set){
			TIntHashSet newT = new TIntHashSet();
			for (String element : setElement){
				newT.add(genes2numbers.get(element));
			}
			ret.add(newT);
		}
		*/
		return ret;
		
		
	}
	
}
