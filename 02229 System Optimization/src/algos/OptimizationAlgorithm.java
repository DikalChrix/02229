package algos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import objectClasses.EDPTuple;
import objectClasses.testFormat;
import java.util.Random;
import java.util.stream.IntStream;
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


// FOR TESTING OPTIMIZING PARTITIONS OF POLLINGS SERVERS

public ArrayList<ArrayList<testFormat>> testPollingStuff(ArrayList<testFormat> eventTasks) {
	
	// Get size of tasks
	int n = eventTasks.size();
	
	ArrayList<testFormat> partition1 = new ArrayList<testFormat>();
	ArrayList<testFormat> partition2 = new ArrayList<testFormat>();
	
	for(int i=0; i<n/2; i++) {
		partition1.add(eventTasks.get(i));
	}
	
	for(int i=n/2; i<n; i++) {
		partition2.add(eventTasks.get(i));
	}
	
	
	ArrayList<ArrayList<testFormat>> result = new ArrayList<ArrayList<testFormat>>();
	result.add(partition1);
	result.add(partition2);
	
	return result;

}

public ArrayList<ArrayList<testFormat>> simulatedAnnealingPollingServers(ArrayList<ArrayList<testFormat>> eventTasks, int Tstart, double alpha, int minIdlePeriod) {
	
	//Count number of polling tasks needed
	int n = eventTasks.size();
	
	Instant startTime = Instant.now();
	
	double t = Tstart;
	int delta = 0;
	
	// Test initial solution, check how good the runtimes of each polling server is
	EDPAlgorithm runEDP = new EDPAlgorithm();
	boolean resultBoolean = false;
	int bestTotalResponseTime = 0;
	int currentTotalResponseTime = 0;
	
	
	
	
	for(int i=0; i<n; i++) {
		EDPTuple resultInitial = runEDP.algorithm(minIdlePeriod/n, 1000/n, 1000/n, eventTasks.get(i));
		if(!resultInitial.isResult()) {
			resultBoolean = false;
			break; 
		} else {
			resultBoolean = true;
			bestTotalResponseTime = bestTotalResponseTime + resultInitial.getResponseTime();
		}
	}
	
	// Now we know the total response time of our initial solution & if it works
	
	//System.out.println("Initial WCRT: "+solutionResponseTime);
	
	currentTotalResponseTime = bestTotalResponseTime;
	ArrayList<ArrayList<testFormat>> currentPartition = eventTasks;
	ArrayList<ArrayList<testFormat>> bestPartition = eventTasks;
	
	
	while(t>0.01) {
		
		ArrayList<ArrayList<testFormat>> neighbourPartition = new ArrayList<ArrayList<testFormat>>();
		if(n>2) {
			neighbourPartition = swapN(eventTasks);
		} else {
			neighbourPartition = swap2(eventTasks);
		}
	
		
		
				
		int newTotalResponseTime = 0;
		
		resultBoolean = false;
		
		//Test newly generated solution; 
		for(int i=0; i<n; i++) {
			EDPTuple resultInitial = runEDP.algorithm(minIdlePeriod/n, 1000/n, 1000/n, eventTasks.get(i));
			if(!resultInitial.isResult()) {
				resultBoolean = false;
				System.out.print("\t "+resultInitial.isResult()+" \t");
				break;
			} else {
				resultBoolean = true;
				newTotalResponseTime = newTotalResponseTime + resultInitial.getResponseTime();
			}
		}
		
		// Print out the new partition
		
		delta = bestTotalResponseTime - newTotalResponseTime; // If delta is positive, the Neighbour is a better solution
		
		if( delta>0 || probabilityFunc(delta, t) ) {
			currentPartition = neighbourPartition;
			currentTotalResponseTime = newTotalResponseTime;
			
			System.out.print("\t Current Total Response Time: "+currentTotalResponseTime+"\t");
			
			if(resultBoolean && newTotalResponseTime>0 && newTotalResponseTime<bestTotalResponseTime)  {
				bestPartition = neighbourPartition;
				bestTotalResponseTime = newTotalResponseTime;
				//System.out.print("Current best, correct Total Response time: "+bestTotalResponseTime+" with partition: ?");
			}
			
			
		} else {
			System.out.print("\t\t\t\t");
		}
		t = t*alpha; //Change of temperature for each round
		
		System.out.print("\t Temperature: "+t+" \t");
		printOutPartitions(currentPartition);
		System.out.println("");
	}
	
	
	
	return bestPartition;


}

public ArrayList<ArrayList<testFormat>> swap2(ArrayList<ArrayList<testFormat>> eventTasks){
	
	//Extract the tasks of the two polling servers:
	ArrayList<testFormat> pollTasks1 = eventTasks.get(0);
	ArrayList<testFormat> pollTasks2 = eventTasks.get(1);
	
	//Generate random indices to swap:
	Random rand = new Random();
	int index1 = (int) rand.nextInt(pollTasks1.size());
	int index2 = (int) rand.nextInt(pollTasks2.size());
	
	//Swaps the elements
	testFormat temp = pollTasks1.get(index1);
	pollTasks1.set(index1,pollTasks2.get(index2));
	pollTasks2.set(index2, temp);
	
	//Packs up the arraylist
	ArrayList<ArrayList<testFormat>> result = new ArrayList<ArrayList<testFormat>>();
	result.add(pollTasks1);
	result.add(pollTasks2);
	
	
	return result;
}

public ArrayList<ArrayList<testFormat>> swapN(ArrayList<ArrayList<testFormat>> eventTasks){

	//Get number of polling servers
	int n = eventTasks.size();
	
	//Generate random order of swapping
	// Generate the list arraylist of integers from 1 to n
	ArrayList<Integer> orderList = new ArrayList<Integer>();
	for(int i=0; i<n; i++) {
		orderList.add(i);
	}
	
	
	Collections.shuffle(orderList); // Shuffles the list
	
	
	//Convert order of swapping to pairs
	ArrayList<ArrayList<Integer>> swapPairs = extractPairsFromList(orderList, n);
	
	//System.out.println("Number of pairs: "+swapPairs.size());
	
	
	//
	ArrayList<ArrayList<testFormat>> result = eventTasks;

	//printOutPartitions(result);
	//System.out.println("\n");
	
	for(int i=0; i<n; i++) {
		
		//Extract number for first pair
		ArrayList<Integer> pair = swapPairs.get(i);
		
		ArrayList<ArrayList<testFormat>> inputSwap = new ArrayList<ArrayList<testFormat>>();
		inputSwap.add(result.get(pair.get(0)));
		inputSwap.add(result.get(pair.get(1)));
		
		System.out.println("Before: ");
		printOutPartitions(result);
		System.out.println("\n");
		
		//Perform swapping and put result back into result-arraylist
		ArrayList<ArrayList<testFormat>> swappedResult = swap2(result);
		result.set(pair.get(0),swappedResult.get(0));
		result.set(pair.get(1),swappedResult.get(1));
		
		System.out.println("After: ");
		printOutPartitions(result);
		System.out.println("\n");
		break;
 		
	}
	
	//printOutPartitions(result);
	//System.out.println("\n");
	
	return result;
}

public ArrayList<ArrayList<Integer>>extractPairsFromList(ArrayList<Integer> order, int n){
	
	// Result
	ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
	
	for(int i=0;i<n-1;i++) {
		ArrayList<Integer> temp = new ArrayList<Integer>();
		temp.add(order.get(i));
		temp.add(order.get(i+1));
		result.add(temp);
	}
	
	//Last pair between first and last partition in order:
	ArrayList<Integer> temp = new ArrayList<Integer>();
	temp.add(order.get(n-1));
	temp.add(order.get(0));
	result.add(temp);
	
	return result;
	
}

public void printOutPartitions(ArrayList<ArrayList<testFormat>> partitions) {
	
	
	
	// Number of partitions:
	int n = partitions.size();
	
	//System.out.println("Size: "+n);
	
	for(int i=0; i<n; i++) {
		
		// Get # of tasks in single partition
		int m = partitions.get(i).size();
		
		System.out.print("\t Tasks in parition "+i+": \t");
		
		for(int j=0; j<m; j++) {
			System.out.print(" "+partitions.get(i).get(j).getName());
			
			// Make comma between tasks, except at last one:
			if(j!=m-1) {
				System.out.print(",");
			}
		}
	}
	
}

// FOR TESTING PARAMETERS OF INDIVIDUAL POLLING SERVERS

public void optimizePollingParameters(ArrayList<ArrayList<testFormat>> partitions, int initialBudget, int initialDeadline, int initialPeriod, int min, int max) {
	
	int n = partitions.size();
	
	
	int maxBudget = initialBudget;
	//int maxDeadline = initialDeadline;
	//int maxPeriod = initialPeriod;
	
	ArrayList<int[]> result = new ArrayList<int[]>();
	
	for(int i = 0; i<n; i++) {
		result.add(optimizePollingPeriod(initialBudget, initialDeadline, initialPeriod, partitions.get(i)));
		
		//Adjust initial budget
		if(result.get(i)[1]<maxBudget) {
			initialBudget = initialBudget + (maxBudget - result.get(i)[1]);
		}
		
		
	}
	
	
	
}

public double finalWCRT(int EDFWCRT, int EDPWCRT, int timeTasksSize, int eventTasksSize) {
	
	double timeTasksWeight = timeTasksSize/(timeTasksSize+eventTasksSize);
	double eventTasksWeight = eventTasksSize/(timeTasksSize+eventTasksSize);
	
	return (timeTasksWeight*EDFWCRT+eventTasksWeight * EDPWCRT)/2;
	
	
	
	
	
}

public ArrayList<ArrayList<testFormat>> testingNumPollingServers(ArrayList<testFormat> eventTasks, int numPolling) {
	
	//System.out.println("Size: "+eventTasks.size());
	
	
	ArrayList<testFormat> sortedEventTasks = sortTasksDuration(eventTasks);
	ArrayList<ArrayList<testFormat>> result = new ArrayList<ArrayList<testFormat>>();
	int[] polDura = new int[numPolling];
	if (sortedEventTasks.size() < numPolling) {
		for (int i = 0; i<sortedEventTasks.size(); i++ ) {
			ArrayList<testFormat> temp = new ArrayList<testFormat>();
			temp.add(sortedEventTasks.get(i));
			result.add(i, temp);
		}
	}
	else {
		for (int i = 0; i < numPolling; i++) {
			ArrayList<testFormat> temp = new ArrayList<testFormat>();
			temp.add(sortedEventTasks.get(i));
			polDura[i] = temp.get(0).getDuration();
			result.add(temp);
		}
		for (int i = 0; i < numPolling; i++) {
			sortedEventTasks.remove(i);
		}
		while (!sortedEventTasks.isEmpty())
		{
			int index = 0;
			int durCheck = 15000;
			for(int i = 0; i<numPolling;i++) {
				if (durCheck > polDura[i]) {
					durCheck = polDura[i];
					index = i;
				}
			}
			ArrayList<testFormat> temp = result.get(index);
			temp.add(sortedEventTasks.get(0));
			polDura[index] += sortedEventTasks.get(0).getDuration();
			sortedEventTasks.remove(0);
			result.set(index, temp);
		}
	}
	
	return result;
}

public ArrayList<testFormat> sortTasksDuration (ArrayList<testFormat> ET) {
	ArrayList<testFormat> result = new ArrayList<testFormat>();
	while(!ET.isEmpty()) {
		int i = 0;
		int mainDura = ET.get(i).getDuration();
		int mainIndex = 0;
		for (int j = 1; j <ET.size();j++) {
			int tempDura = ET.get(j).getDuration();
			if (tempDura > mainDura) {
				mainDura = tempDura;
				mainIndex = j;
			}
		}
		result.add(ET.get(mainIndex));
		ET.remove(mainIndex);
	}
	
	return result;
}

public int testPartitionPolling(ArrayList<ArrayList<testFormat>> partitions, int minIdlePeriod) {
	
	// Number of polling servers:
	int n = partitions.size();
	
	EDPAlgorithm runEDP = new EDPAlgorithm();
	int totalResponseTime = 0;
	
	for(int i=0; i<n; i++) {	
		EDPTuple resultInitial = runEDP.algorithm(minIdlePeriod/n, 1000/n, 1000/n, partitions.get(i));
		
		//System.out.println("Result: "+resultInitial.isResult());
		
		if(!resultInitial.isResult()) {
			return Integer.MAX_VALUE; // If one polling server can not satisfy the tasks it is given, let it fail 
		} else {
			totalResponseTime = totalResponseTime + resultInitial.getResponseTime();
		}				
	}
	
	return totalResponseTime;
}





}
