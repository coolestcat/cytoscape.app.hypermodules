package org.cytoscape.hypermodules.internal.statistics;

import JSci.maths.statistics.NormalDistribution;
import JSci.maths.statistics.ChiSqrDistribution;

/**
 * 
 * An implementation of fitting the cox proportional hazards model to a set of survival data
 * with n covariates. Copied directly from C source code of the survival library of R.
 * All methods and fields are the same. Very slow and inefficient for the purposes of this app.
 * @author alvinleung
 *
 */
public class CoxPh {
	
	private static final int LARGE = 22;
	private static final int SMALL = -200;
	private static int maxiter = 20;
	private static final int method = 1;
	private static final double EPS = Math.pow(10,-9);
	private static final double toler = Math.pow(2.220446*Math.pow(10, -16), 0.75);
	private double[] weights;
	private double[] time;
	private double[] status;
	private double[] covar1;
	private double ll0;
	
	public CoxPh(int length, double[] time, double[] status, double[] covar1){
		this.weights = new double[length];
		for (int i=0; i<weights.length; i++){
			weights[i]=1;
		}
		this.time = time;
		this.status = status;
		this.covar1 = covar1;
	}
	
	public void coxInit(){
		double[][] covar2 = new double[1][covar1.length];
		/*
		double[] covarCopy = new double[covar1.length];
		for (int i=0; i<covar1.length; i++){
			covarCopy[i] = covar1[i];
		}*/
		covar2[0] = covar1;
		double[] coxResult2 = coxfit(time, status, covar2);
		this.ll0 = coxResult2[1];
	}
	
	public double cox(double[] covar0){

		/*
		double[] covarCopy = new double[covar1.length];
		for (int i=0; i<covar1.length; i++){
			covarCopy[i] = covar1[i];
		}
		*/
		
		double[][] covar = new double[2][covar1.length];
		covar[0] = covar0;
		covar[1] = covar1;
		
		this.maxiter = 20;

		double[] coxResult = coxfit(time, status, covar);
		System.out.println(coxResult[1]);
		double subtract = coxResult[1]-ll0;
		double pValue = 1-new ChiSqrDistribution(1).cumulative(Math.abs(2*subtract));
		return pValue;
		
	}
	
	public double[] coxfit(double[] time, double[] status, double[][] covar){

		int[] strata = new int[time.length];
		int i,j,k,person;
		int iter;
		int nused = time.length;
		int nvar = covar.length;
		double denom = 0.0;
		double zbeta;
		double risk;
		double temp;
		double temp2;
		double ndead;
		double newlk = 0.0;
		double d2;
		double efron_wt;
		int halving;
		int flag;
		double[] a = new double[nvar];
		double[] newbeta = new double[nvar];
		double[] a2 = new double[nvar];
		double[] mark = new double[nused];
		double[] wtave = new double[nused];
		double[] means = new double[nvar];
		double[][] imat = new double[nvar][nvar];
		double[][] cmat = new double[nvar][nvar];
		double[][] cmat2 = new double[nvar][nvar];
		double[] loglik = new double[2];
		double[] u = new double[nvar];
		double[] offset = new double[nused];
		double[] beta = new double[nvar];

		int sctest = method;
		
	    temp=0;
	    j=0;
	    for (i=nused-1; i>0; i--) {
	    	if ((time[i]==time[i-1]) && (strata[i-1] != 1)) {
	    	    j += status[i];
	    	    temp += status[i]* weights[i];
	    	    mark[i]=0;
	    	}
	    	
	    	else  {
	    	    mark[i] = j + status[i];
	    	    if (mark[i] >0) {
	    	    	wtave[i] = (temp+ status[i]*weights[i])/ mark[i];
	    	    }
	    	    temp=0; j=0;
	    	}
	    }
	    
	    mark[0]  = j + status[0];
	    if (mark[0]>0) wtave[0] = (temp +status[0]*weights[0])/ mark[0];

	    
	    for (i=0; i<nvar; i++) {
	    	temp=0;
	    	for (person=0; person<nused; person++) {
	    		temp += covar[i][person];
	    	}
	    	temp /= nused;
	    	means[i] = temp;
	    	for (person=0; person<nused; person++){
	    		covar[i][person] -=temp;
	    	}
	    }
	    
	    strata[nused-1]=1;
	    loglik[1] =0;
	    
	    for (i=0; i<nvar; i++) {
	    	u[i] =0;
			for (j=0; j<nvar; j++){
				imat[i][j] =0 ;
			}
		}
	    
	    efron_wt =0;
	    for (person=nused-1; person>=0; person--) {
	    	int person2 = person;
	    	if (strata[person] == 1) {
	    		denom = 0;
	    		for (i=0; i<nvar; i++) {
	    			a[i] = 0;
	    			a2[i]=0 ;
	    			for (j=0; j<nvar; j++) {
	    				cmat[i][j] = 0;
	    				cmat2[i][j]= 0;
	    			}
	    		}
		    }
	    	
	    	zbeta = offset[person]; 
	    	for (i=0; i<nvar; i++){
	    		zbeta += beta[i]*covar[i][person];
	    	}

	    	zbeta = coxsafe(zbeta);

	    	risk = Math.exp(zbeta) * weights[person];

		    
	    	denom += risk;

	    	efron_wt += status[person] * risk;  /*sum(denom) for tied deaths*/
	    	
	    	for (i=0; i<nvar; i++) {
	    	    a[i] += risk*covar[i][person];
	    	    
	    	    for (j=0; j<=i; j++){
	    	    	
	    	    	cmat[i][j] += risk*covar[i][person]*covar[j][person];
	    	    	
	    	    }
	    	
	    	}
	    	
	    	if (status[person]==1) {
	    	    loglik[1] += weights[person]*zbeta;
	    	    for (i=0; i<nvar; i++) {
	    		    u[i] += weights[person]*covar[i][person];
	    		    a2[i] +=  risk*covar[i][person];
	    		for (j=0; j<=i; j++)
	    		    cmat2[i][j] += risk*covar[i][person]*covar[j][person];
	    		}
	    	}
	    	if (mark[person] >0) {  
	    	    ndead = mark[person];
	    	    for (k=0; k<ndead; k++) {
	    	    	temp = (double)k * method / ndead;

	    			d2= denom - temp*efron_wt;

	    			loglik[1] -= wtave[person] * Math.log(d2);
	    			for (i=0; i<nvar; i++) {

	    			    temp2 = (a[i] - temp*a2[i])/ d2;

	    			    u[i] -= wtave[person] *temp2;

	    			    for (j=0; j<=i; j++){
	    			    	
	    			    	imat[j][i] +=  wtave[person]*(
	    					 (cmat[i][j] - temp*cmat2[i][j]) /d2 -
	    						  temp2*(a[j]-temp*a2[j])/d2);
	    			    	
	    			    }
	    			}
	    			
	    			
	    	    }
	    	    efron_wt =0;
	    	    for (i=0; i<nvar; i++) {
	    	    	a2[i]=0;
	    	    	for (j=0; j<nvar; j++) {
	    	    		cmat2[i][j]=0;
	    	    	}
	    	    }
	    		
	    	}
	    	
	    	 loglik[0] = loglik[1]; 

	    	 for (i=0; i<nvar; i++){

	    		 a[i] = u[i];/*use 'a' as a temp to save u0, for the score test*/
	    	 }

	    	
	    	 flag= cholesky2(imat, nvar, this.toler);

	    	 chsolve2(imat,nvar,a);   

	    	sctest=0;
	    	for (i=0; i<nvar; i++){
		    	sctest +=  u[i]*a[i];
	    	}

	        for (i=0; i<nvar; i++) {

	        	newbeta[i] = beta[i] + a[i];
	        }

	        if (maxiter==0) {
	        	chinv2(imat,nvar);
	        	for (i=1; i<nvar; i++)
	        	    for (j=0; j<i; j++)  imat[i][j] = imat[j][i];
	        	return null;   /* and we leave the old beta in peace */
	        }
	        
	        halving =0 ;   
	        for (iter=1; iter<=maxiter; iter++) {
	        	newlk =0;
	        	for (i=0; i<nvar; i++) {
	        	    u[i] =0;
	        	    for (j=0; j<nvar; j++){
		        		imat[i][j] =0;
	        	    }
	        	}
	        	
	        	for (person=nused-1; person>=0; person--) {
	        	    if (strata[person] == 1) { /* rezero temps for each strata */
	        		efron_wt =0;
	        		denom = 0;
	        			for (i=0; i<nvar; i++) {
	        				a[i] = 0;
	        				a2[i]=0 ;
	        				for (j=0; j<nvar; j++) {
	        					cmat[i][j] = 0;
	        					cmat2[i][j]= 0;
	        				}
	        		    }
	        		}
	        	    
	        	    zbeta = offset[person];
	        	    for (i=0; i<nvar; i++){
	        	    	zbeta += newbeta[i]*covar[i][person];
	        	    }
	        	    zbeta = coxsafe(zbeta);
	        	    risk = Math.exp(zbeta ) * weights[person];
	        	    denom += risk;
	        	    efron_wt += status[person] * risk;  /* sum(denom) for tied deaths*/
	        	    
	        	    for (i=0; i<nvar; i++) {
	        			a[i] += risk*covar[i][person];
	        			for (j=0; j<=i; j++){
	        				 cmat[i][j] += risk*covar[i][person]*covar[j][person];
	        			}
	        			 
	        		}
	        	    
	        	    if (status[person]==1) {
	        			newlk += weights[person] *zbeta;
	        			for (i=0; i<nvar; i++) {
	        			    u[i] += weights[person] *covar[i][person];
	        			    a2[i] +=  risk*covar[i][person];
	        			    for (j=0; j<=i; j++){
		        				cmat2[i][j] += risk*covar[i][person]*covar[j][person];
	        			    }

	        			}
	        	    }
	        	    
	        	    if (mark[person] >0) {
	        	    	for (k=0; k<mark[person]; k++) {
	        	    		
	        			    temp = (double)k* method /mark[person];
	        			    d2= denom - temp*efron_wt;
	        			    newlk -= wtave[person] *Math.log(d2);
	        			    for (i=0; i<nvar; i++) {
	        				temp2 = (a[i] - temp*a2[i])/ d2;
	        				u[i] -= wtave[person] *temp2;
	        					for (j=0; j<=i; j++){
	        						imat[j][i] +=  wtave[person] *(
	 	        					       (cmat[i][j] - temp*cmat2[i][j]) /d2 -
	 	        						      temp2*(a[j]-temp*a2[j])/d2);
	        					}
	        						
	        				}
	        			 }
	        	    	efron_wt =0;
	        			for (i=0; i<nvar; i++) {
	        			    a2[i]=0;
	        			    for (j=0; j<nvar; j++){
	        			    	cmat2[i][j]=0;
	        			    }
	        			}
	        	    	
	        	    }
	        	}
	        	
	        	flag = cholesky2(imat, nvar, this.toler);
	        	
	        	if (Math.abs(1-(loglik[1]/newlk))<=EPS && halving==0) { /* all done */
	        	    loglik[1] = newlk;
	        	    chinv2(imat, nvar);     /* invert the information matrix */
	        	    for (i=1; i<nvar; i++){
	        	    	for (j=0; j<i; j++){
	        	    		imat[i][j] = imat[j][i];
	        	    	}
	        	    }
	        		
	        	    for (i=0; i<nvar; i++){
		        		beta[i] = newbeta[i];
	        	    }
	        	    maxiter = iter;

	        	    return loglik;
	        	 }
	        	
	        	if (iter==maxiter){
	        		break;
	        	}

	        	if (newlk < loglik[1])   {
	        		halving =1;
	        		for (i=0; i<nvar; i++){
	        		    newbeta[i] = (newbeta[i] + beta[i]) /2; /*half of old increment */
	        		}
	        	}
	        	
	            else {
	        		halving=0;
	        		loglik[1] = newlk;
	        		chsolve2(imat,nvar,u);

	        		j=0;
	        		for (i=0; i<nvar; i++) {

	        		    beta[i] = newbeta[i];

	        		    newbeta[i] = newbeta[i] +  u[i];
	        		 }


	            }

	        }
	       person = person2;
	    }

	    loglik[1] = newlk;
	    chinv2(imat, nvar);
	    for (i=1; i<nvar; i++)
		for (j=0; j<i; j++)  imat[i][j] = imat[j][i];
	    for (i=0; i<nvar; i++)
		beta[i] = newbeta[i];
	   // System.out.println(imat[0][0]);
	    flag= 1000;

		return loglik;
	}
	
	public int cholesky2(double[][] matrix, int n, double toler)
    {
		double temp;
		int  i,j,k;
		double eps, pivot;
		int rank;
		int nonneg;
   
		nonneg=1;
		eps =Math.pow(10, -15);
		for (i=0; i<n; i++) {
			if (matrix[i][i] > eps)  {
				eps = matrix[i][i];
			}
			for (j=(i+1); j<n; j++)  {
				matrix[j][i] = matrix[i][j];
			}
		}
		eps *= toler;
		
		rank =0;
		for (i=0; i<n; i++) {
			
			pivot = matrix[i][i];
			if (pivot < eps) {
				matrix[i][i] =0;
				if (pivot < -8*eps) {
					nonneg= -1;
				}
			}
			else  {
				rank++;
				for (j=(i+1); j<n; j++) {
					temp = matrix[j][i]/pivot;
					matrix[j][i] = temp;
					matrix[j][j] -= temp*temp*pivot;
					for (k=(j+1); k<n; k++) {
						matrix[k][j] -= temp*matrix[k][i];
					}
				}
			}
			 //System.out.println(matrix[0][0] + " : " + matrix[0][1] + " : " + matrix[1][0] + " : " + matrix[1][1]);
		}
		return(rank * nonneg);
    }
	
	//TODO: probably doesn't work.
	public void chsolve2(double[][] matrix, int n, double[] y){
    int i,j;
    double temp;

    /*
    ** solve Fb =y
    */
    for (i=0; i<n; i++) {
	  temp = y[i] ;
	  	for (j=0; j<i; j++){
		  temp -= y[j] * matrix[i][j] ;
	  	}  
	  y[i] = temp ;

	 }
    /*
    ** solve DF'z =b
    */
    for (i=(n-1); i>=0; i--) {

	  if (matrix[i][i]==0)  {

		  y[i] =0;

	  }
	  else {
	      temp = y[i]/matrix[i][i];
	      for (j= i+1; j<n; j++){
	    	   temp -= y[j]*matrix[j][i];
	      }
	      y[i] = temp;

	      }
	  }
    }
	
	public void chinv2(double[][] matrix , int n)
    {
   double temp;
   int i,j,k;

    /*
    ** invert the cholesky in the lower triangle
    **   take full advantage of the cholesky's diagonal of 1's
    */
    for (i=0; i<n; i++){
	  if (matrix[i][i] >0) {
	      matrix[i][i] = 1/matrix[i][i];   /*this line inverts D */
	      for (j= (i+1); j<n; j++) {
		   matrix[j][i] = -matrix[j][i];
		   for (k=0; k<i; k++)     /*sweep operator */
			matrix[j][k] += matrix[j][i]*matrix[i][k];
		   }
	      }
	  }

    /*
    ** lower triangle now contains inverse of cholesky
    ** calculate F'DF (inverse of cholesky decomp process) to get inverse
    **   of original matrix
    */
    for (i=0; i<n; i++) {
	  if (matrix[i][i]==0) {  /* singular row */
		for (j=0; j<i; j++){
			matrix[j][i]=0;
		}
		for (j=i; j<n; j++) {
			matrix[i][j]=0;
		}
	}
	  else {
	      for (j=(i+1); j<n; j++) {
		   temp = matrix[j][i]*matrix[j][j];
		   if (j!=i) matrix[i][j] = temp;
		   		for (k=i; k<j; k++){
		   			matrix[i][k] += temp*matrix[j][k];
		   		}
		   }
	      }
	  }
    }


	
	
	public double coxsafe(double a){
		if (a<SMALL){
			return SMALL;
		}
		if (a>LARGE){
			return LARGE;
		}
		return a;
	}
	
	
	
	
	
}
