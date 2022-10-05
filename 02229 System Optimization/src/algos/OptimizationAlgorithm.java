package algos;

import java.util.ArrayList;
import objectClasses.EDPTuple;
import objectClasses.testFormat;
import java.util.Random;

public class OptimizationAlgorithm {

	public int[] optimizePollingPeriod(int fixedPollingBudget, int fixedPollingDeadline, int hyperPeriod, ArrayList<testFormat> eventTasks) {
			
		int optimalPeriod = 0;
		int optimalResponseTime = Integer.MAX_VALUE;
		EDPTuple result = null;
		EDPAlgorithm runEDP = new EDPAlgorithm();
		
		
		
		
		for(int i = fixedPollingDeadline; i<hyperPeriod; i++ ) {
			result = runEDP.algorithm(fixedPollingBudget, i, fixedPollingDeadline, eventTasks);
			if(optimalResponseTime>result.getResponseTime() && result.isResult()) {
				optimalPeriod = i;
				optimalResponseTime = result.getResponseTime();
			}
			
			//System.out.println("Testing period: "+i+" Response Time: "+result.getResponseTime()+" Works: "+result.isResult());
		}
		
		int[] resultArray = new int[2];
		resultArray[0] = optimalResponseTime;
		resultArray[1] = optimalPeriod;
		
		return resultArray;
		
	}
	
	public int[] optimizeBoth(int fixedPollingBudget, int hyperPeriod, ArrayList<testFormat> eventTasks, int incremental, int max) {
		
		int optimalResponseTime = Integer.MAX_VALUE;
		int optimalPeriod = 0;
		int optimalDeadline = 0;
		int[] resultArray = new int[2]; 
		
		for(int j=fixedPollingBudget; j<max; j=j+incremental) {
			resultArray = optimizePollingPeriod(fixedPollingBudget, j, hyperPeriod, eventTasks);
			if(resultArray[0]<optimalResponseTime) {
				optimalResponseTime = resultArray[0];
				optimalPeriod = resultArray[1];
				optimalDeadline = j;		
			}
			
			System.out.println("Testing period: "+j+" Response Time: "+resultArray[0]+" Period: "+resultArray[1]);
		}
		
		int returnArray[] = {optimalResponseTime, optimalPeriod, optimalDeadline};
		return returnArray;
	}
	
	public int[] generateNeighbour(int budget, int period, int deadline) {
		
		//Pick random parameter to change
		Random rand = new Random();
		int choice = (int) rand.nextInt(2);
		
		//Pick to either increment or decrement
		int operation = (int) rand.nextInt(1);
		int res;
		int[] resArray = {budget, period, deadline};
		switch(choice) {
			case 0: //Change budget
				if(operation == 0) {
					res = budget -1;
				} else {
					res = budget +1;
				}
				resArray[0] = res;
				return resArray;
			case 1: //Change period
				if(operation == 0) {
					res = period -1;
				} else {
					res = period +1;
				}
				resArray[1] = res;
				return resArray;
			case 2: //Change deadline
				if(operation == 0) {
					res = deadline -1;
				} else {
					res = deadline +1;
				}
				resArray[0] = res;
				return resArray;
		}
		return resArray;		
	}
	
	public int[] simulatedAnnealing(int[] initialSolution, int Tstart, double alpha, ArrayList<testFormat> eventTasks) {
		
		double t = Tstart;
		int delta = 0;
		EDPAlgorithm runEDP = new EDPAlgorithm();
		
		while(t>0.01) {
			int[] neighbour = generateNeighbour(initialSolution[0], initialSolution[1], initialSolution[2]);
			//Test WCRT of initial solution:
			EDPTuple resultInitial = runEDP.algorithm(initialSolution[0], initialSolution[1], initialSolution[2], eventTasks);
			
			//Test WCRT of neighbour solution: 
			EDPTuple resultNeighbour = runEDP.algorithm(initialSolution[0], initialSolution[1], initialSolution[2], eventTasks);
			
			delta = resultInitial.getResponseTime() - resultNeighbour.getResponseTime(); // If delta is positive, the Neighbour is a better solution
			
			if( delta>0 || probabilityFunc(delta, t) ) {
				initialSolution = neighbour;
			}
			t = t*alpha; //Change of temperature for each round
			
			System.out.println("Temperature: "+t);
		}
		
		return initialSolution;
		
	}

	public boolean probabilityFunc(int delta, double t) {
		double probability = Math.exp(-(1/t)*delta);
		
		Random rand = new Random();
		int pick = (int) rand.nextInt(100);
		
		if(probability >=(double) pick ) {
			return true;
		} else {
			return false;
		}
		
	}
	
}
