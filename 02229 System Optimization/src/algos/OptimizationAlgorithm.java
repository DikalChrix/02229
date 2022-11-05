package algos;

import java.util.ArrayList;
import objectClasses.EDPTuple;
import objectClasses.testFormat;
import java.util.Random;
import java.time.Duration;
import java.time.Instant;

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
		int choice = (int) rand.nextInt(3);
		
		//Pick to either increment or decrement
		int operation = (int) rand.nextInt(1);
		int res;
		int[] resArray = {budget, period, deadline};
		switch(choice) {
			case 0: //Change budget
				if(operation == 0 && budget!=1) {
					res = budget -1;
				} else if (operation == 1 && budget<period) {
					res = budget +1;
				} else if (operation == 0)  {
					res = budget +1;
				} else {
					res = budget -1;
				}
				resArray[0] = res;
				return resArray;
			case 1: //Change period
				if(operation == 0 && period!=1 && deadline<period && budget<period) {
					res = period -1;
				} else if (operation == 1 && period<12000) { //Hyperperiod
					res = period +1;
				} else if (operation == 0)  {
					res = period +1;
				} else {
					res = period -1;
				}
				resArray[1] = res;
				return resArray;
			case 2: //Change deadline
				if(operation == 0 && deadline!=1) {
					res = deadline -1;
				} else if (operation == 1 && deadline<period) {
					res = deadline +1;
				} else if (operation == 0)  {
					res = deadline +1;
				} else {
					res = deadline -1;
				}
				resArray[2] = res;
				return resArray;
		}
		return resArray;		
	}
	
	public int[] simulatedAnnealing(int[] initialSolution, int Tstart, double alpha, ArrayList<testFormat> eventTasks, int min) {
		
		
		Instant startTime = Instant.now();
		
		double t = Tstart;
		int delta = 0;
		EDPAlgorithm runEDP = new EDPAlgorithm();
		
		//Test WCRT of initial solution:
		EDPTuple resultInitial = runEDP.algorithm(initialSolution[0], initialSolution[1], initialSolution[2], eventTasks);
		
		int solutionResponseTime = resultInitial.getResponseTime();
		int[] bestCorrectSolution = initialSolution;
		int bestCorrectResponseTime = resultInitial.getResponseTime();
		
		System.out.println("Initial WCRT: "+solutionResponseTime);
		
		while(t>0.01) {
			int[] neighbour = generateNeighbourTest(initialSolution[0], initialSolution[1], initialSolution[2], min);
					
			
			//Test WCRT of neighbour solution: 
			EDPTuple resultNeighbour = runEDP.algorithm(neighbour[0], neighbour[1], neighbour[2], eventTasks);
			
			if(resultNeighbour.getResponseTime()==0) {
				resultNeighbour = runEDP.algorithm(neighbour[0], neighbour[1], neighbour[2], eventTasks);
			}
			
			System.out.println("Neighbour parameters: "+neighbour[0]+" "+neighbour[1]+" "+neighbour[2]);
			
			delta = resultInitial.getResponseTime() - resultNeighbour.getResponseTime(); // If delta is positive, the Neighbour is a better solution
			
			if( delta>0 || probabilityFunc(delta, t) ) {
				initialSolution = neighbour;
				solutionResponseTime = resultNeighbour.getResponseTime();
				resultInitial = resultNeighbour;
				
				if(resultNeighbour.isResult() && resultNeighbour.getResponseTime()>0 && resultNeighbour.getResponseTime()<bestCorrectResponseTime)  {
					bestCorrectSolution = neighbour;
					bestCorrectResponseTime = resultNeighbour.getResponseTime();
					System.out.println("Current best, correct Response time: "+bestCorrectResponseTime+" with parameters: "+neighbour[0]+" "+neighbour[1]+" "+neighbour[2]);
				}
				
				
			}
			t = t*alpha; //Change of temperature for each round
			
			System.out.println("Temperature: "+t);
		}
		
		System.out.println("Best WCRT with Simulated Annealing: "+bestCorrectResponseTime);
		
		Instant endTime = Instant.now();
		
		System.out.println("Simulated Annealing duration: "+Duration.between(startTime,endTime).toMinutes()+" minutes");
		
		return bestCorrectSolution;
		
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
	
	public boolean checkParameterConstraints(int[] parameters) {
		
		if(parameters[1]>=parameters[0] && parameters[1]>=parameters[2]) {
			return true;
		} else {
			return false;
		}
	}
	
public int[] generateNeighbourNoPeriod(int budget, int period, int deadline) {
		
		//Pick random parameter to change
		Random rand = new Random();
		int choice = (int) rand.nextInt(3);
		
		//Pick to either increment or decrement
		int operation = (int) rand.nextInt(1);
		int res;
		int[] resArray = {budget, period, deadline};
		switch(choice) {
			case 0: //Change budget
				if(operation == 0 && budget!=1) {
					res = budget -1;
				} else if (operation == 1 && budget<period) {
					res = budget +1;
				} else if (operation == 0)  {
					res = budget +1;
				} else {
					res = budget -1;
				}
				resArray[0] = res;
				return resArray;
			case 1: //Change deadline
				if(operation == 0 && deadline!=1) {
					res = deadline -1;
				} else if (operation == 1 && deadline<period) {
					res = deadline +1;
				} else if (operation == 0)  {
					res = deadline +1;
				} else {
					res = deadline -1;
				}
				resArray[2] = res;
				return resArray;
		}
		return resArray;		
	}

public int[] generateNeighbourTest(int budget, int period, int deadline, int min) {
	
	//Pick random parameter to change
	Random rand = new Random();
	
	//Pick to either increment or decrement
	int operation = (int) rand.nextInt(1);
	int res;
	int[] resArray = {budget, period, deadline};
			if(operation == 0 && budget!=1 && budget-1!=min) {
				res = budget -1;
			} else if (operation == 1 && budget<period) {
				res = budget +1;
			} else if (operation == 0)  {
				res = budget +1;
			} else {
				res = budget -1;
			}
			resArray[0] = res;
			resArray[2] = res;
			return resArray;	
}



}
