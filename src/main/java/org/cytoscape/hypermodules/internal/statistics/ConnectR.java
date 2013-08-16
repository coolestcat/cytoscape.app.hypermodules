package org.cytoscape.hypermodules.internal.statistics;


import java.util.HashMap;
import java.util.HashSet;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
/**
 * 
 * A class used for testing the validity of my implementation of the statistical tests - log rank, coxph,
 * and FDR permutation. It connects to R through the Rserve interface, where it runs R commands equivalent
 * to the java code in HypermodulesHeuristicAlgorithm.testModuleClinical.
 * @author alvinleung
 *
 */

public class ConnectR {
	
	private double[] followupDays;
	private double[] censor;
	private double[] group;
	private double[] age;
	
	private HashMap<HashSet<String>, Double> this_true;
	private HashMap<HashSet<String>, Double> this_rand;
	
	private int[][] contingencyTable;
	
	public ConnectR(HashMap<HashSet<String>, Double> this_true, HashMap<HashSet<String>, Double> this_rand){
		this.this_true = this_true;
		this.this_rand = this_rand;
	}
	
	
	public ConnectR(double[] followupDays, double[] censor, double[] group, double[] age){
		this.followupDays = followupDays;
		this.censor = censor;
		this.group = group;
		this.age = age;
	}
	
	public ConnectR(int[][] contingencyTable){
		
	}
	
	public double fisher() throws REngineException{
		RConnection c;
		double p=0;
		
		
		
		
		
		
		
		
		return p;
	}
	
	
	public double coxph() throws REngineException{
		RConnection c;
		double p=0;
		try {
			c = new RConnection();

			//REXP x = c.eval("R.version.string");
			//System.out.println(x.asString());
			
			c.assign("time",  followupDays);
			c.assign("censor", censor);
			c.assign("covariate1", group);
			c.assign("covariate2", age);
			c.eval("library(survival)");
			c.eval("dfr = data.frame(time, censor, covariate1, covariate2)");
			c.eval("surv = Surv(dfr$time, dfr$censor)");
			c.eval("cox1 = coxph(surv ~ covariate1 + covariate2, data = dfr)$loglik[2]");
			c.eval("cox2 = coxph(surv ~ covariate2, data = dfr)$loglik[2]");
			c.eval("p = pchisq(2*(cox1-cox2), 1, lower.tail=F)");
			p = c.eval("p").asDouble();
			return p;
			
		} catch (RserveException e) {
			e.printStackTrace();
		} catch (REXPMismatchException e) {
			e.printStackTrace();
		}
		
		return p;

	}
	
	public HashMap<HashSet<String>, Double> fdrAdjust() throws REngineException, REXPMismatchException{
		
		HashMap<HashSet<String>, Double> adjustedResults = new HashMap<HashSet<String>, Double>();
		RConnection c;
		try {
			c = new RConnection();

			REXP x = c.eval("R.version.string");
			System.out.println(x.asString());
			
			
			HashMap<Double, HashSet<String>> reversedResults = new HashMap<Double, HashSet<String>>();
			HashMap<Double, HashSet<String>> reversedRandomResults = new HashMap<Double, HashSet<String>>();
			
			
			for (HashSet<String> reverse : this_true.keySet()){
				reversedResults.put(this_true.get(reverse), reverse);
			}
			
			for (HashSet<String> reverse : this_rand.keySet()){
				reversedRandomResults.put(this_rand.get(reverse), reverse);
			}
			
			
			double[] resultDoubles = new double[this_true.size()];
			double[] randomResultDoubles = new double[this_rand.size()];
			
			int k=0;
			for (HashSet<String> result : this_true.keySet()){
				resultDoubles[k]=Double.valueOf(this_true.get(result));
				k++;
			}
			
			k=0;
			for(HashSet<String> result : this_rand.keySet()){
				randomResultDoubles[k]=Double.valueOf(this_rand.get(result));
				k++;
			}
			
			
			c.assign("this_true", resultDoubles);
			c.assign("this_rand", randomResultDoubles);
			/*
			double[] toAdjust = c.eval("sapply(this_true, function(x) length(which(x>=this_rand))/length(this_rand))").asDoubles();
			double lengthOfRand = c.eval("length(this_rand)").asDouble();
			double[] lengthWhich = c.eval("sapply(this_true, function(x) length(which(x>=this_rand)))").asDoubles();
			
			System.out.println(lengthOfRand);
			System.out.println(lengthWhich[0]);
			
			System.out.println("toAdjust for R: ");
			for (int i=0; i<toAdjust.length; i++){
				System.out.println(toAdjust[i]);
			}
			*/
			
			
			c.eval("this_true_est = p.adjust(sapply(this_true, function(x) length(which(x>=this_rand))/length(this_rand)), method=\"fdr\")");
			
			double[] adjustedDoubles = c.eval("this_true_est").asDoubles();
			
			
			
			for (int i=0; i<adjustedDoubles.length; i++){
				adjustedResults.put(reversedResults.get(resultDoubles[i]), adjustedDoubles[i]);
			}
			

			
		} catch (RserveException e) {
			e.printStackTrace();
		}
		

		return adjustedResults;
		
		
		
	}
	
	

}
