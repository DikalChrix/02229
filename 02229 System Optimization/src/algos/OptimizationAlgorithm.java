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

	int[] initialValues = {250, 1200, 1200}; // 250, 1000, 1000
	private int[] initialParameters;
	private int maxTimeDuration;
	private int numberPollingServers;
	
	public OptimizationAlgorithm(int numberPollingServers, int maxTimeDuration) {
		this.maxTimeDuration = maxTimeDuration;
		this.numberPollingServers = numberPollingServers;
	}
	
	public void setNumberPollingServers(int numberPollingServers) {
		this.numberPollingServers = numberPollingServers;
		this.initialParameters = generateInitialParameters(0, numberPollingServers, maxTimeDuration);
	}
	
	// Main function for finding optimal number of polling servers:
	public ArrayList<ArrayList<testFormat>> findNumberPollingServers(ArrayList<testFormat> eventTasks) {

		int bestNumberPolling = 0;
		int bestTotalWCRT = Integer.MAX_VALUE;
		ArrayList<ArrayList<testFormat>> bestPartitions = new ArrayList<ArrayList<testFormat>>();

		for (int i = 1; i < 11; i++) {

			// Deepcopy eventTasks
			ArrayList<testFormat> eventTasksCopy = new ArrayList<testFormat>();
			for (int j = 0; j < eventTasks.size(); j++) {
				eventTasksCopy.add(eventTasks.get(j).clone());
			}

			ArrayList<ArrayList<testFormat>> currentPartitions = getPartitionsPollingServers(eventTasksCopy, i);

			// optimizeAlgo.printOutPartitions(currentPartitions);

			int currentWCRT = testPartitions(currentPartitions);

			//System.out.println("\nCurrent response time: " + currentWCRT+ " Number polling servers "+i);

			if (currentWCRT < bestTotalWCRT) {
				bestTotalWCRT = currentWCRT;
				bestNumberPolling = i;
				bestPartitions = currentPartitions;
			}

			//System.out.println("Best WCRT after finding # of polling server: " + bestTotalWCRT);
			//System.out.println("Optimal number of polling servers: " + bestNumberPolling);
			//printOutPartitions(bestPartitions);

		}
		
		//System.exit(0);

		return bestPartitions;

	}

	// Helper functions:
		// Returns partitions, based on the given event tasks and the number of polling
		// servers requested
		public ArrayList<ArrayList<testFormat>> getPartitionsPollingServers(ArrayList<testFormat> eventTasks,
				int numPolling) {
	
			// System.out.println("Size: "+eventTasks.size());
	
			ArrayList<testFormat> sortedEventTasks = sortTasksDuration(eventTasks);
			ArrayList<ArrayList<testFormat>> result = new ArrayList<ArrayList<testFormat>>();
			int[] polDura = new int[numPolling];
			if (sortedEventTasks.size() < numPolling) {
				for (int i = 0; i < sortedEventTasks.size(); i++) {
					ArrayList<testFormat> temp = new ArrayList<testFormat>();
					temp.add(sortedEventTasks.get(i));
					result.add(i, temp);
				}
			} else {
				for (int i = 0; i < numPolling; i++) {
					ArrayList<testFormat> temp = new ArrayList<testFormat>();
					temp.add(sortedEventTasks.get(i));
					polDura[i] = temp.get(0).getDuration();
					result.add(temp);
				}
				for (int i = 0; i < numPolling; i++) {
					sortedEventTasks.remove(i);
				}
				while (!sortedEventTasks.isEmpty()) {
					int index = 0;
					int durCheck = 15000;
					for (int i = 0; i < numPolling; i++) {
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

			// Sorts the tasks based on duration
			public ArrayList<testFormat> sortTasksDuration(ArrayList<testFormat> ET) {
				ArrayList<testFormat> result = new ArrayList<testFormat>();
				while (!ET.isEmpty()) {
					int i = 0;
					int mainDura = ET.get(i).getDuration();
					int mainIndex = 0;
					for (int j = 1; j < ET.size(); j++) {
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
		
		// Test the partitions by returning the average WCRT
		public int testPartitions(ArrayList<ArrayList<testFormat>> partitions) {
	
			// Number of polling servers:
			int n = partitions.size();
	
			EDPAlgorithm runEDP = new EDPAlgorithm();
			int totalResponseTime = 0;
	
			for (int i = 0; i < n; i++) {
				EDPTuple resultInitial = runEDP.algorithm(initialValues[0] / n, initialValues[1] / n, initialValues[2] / n, partitions.get(i));
	
				// System.out.println("Result: "+resultInitial.isResult());
	
				if (!resultInitial.isResult()) {
					return Integer.MAX_VALUE; // If one polling server can not satisfy the tasks it is given, let it fail
				} else {
					totalResponseTime = totalResponseTime + resultInitial.getResponseTime();
				}
			}
	
			return totalResponseTime;
		}
	
	
	// Main function for finding optimal partitions for polling servers (Uses Simulated Annealing): 
	public ArrayList<ArrayList<testFormat>> findOptimalPartitions(
			ArrayList<ArrayList<testFormat>> eventTasks, int Tstart, double alpha) {

		double t = Tstart;
		int delta = 0;

		// Test initial solution, check how good the runtimes of each polling server is
		EDPAlgorithm runEDP = new EDPAlgorithm();
		boolean resultBoolean = false;
		int bestAvgWCRT = 0;
		//int currentAvgWCRT = 0;
		//int[] initialParameters = generateInitialParameters(n, maxTimeDuration);

		for (int i = 0; i < numberPollingServers; i++) {
			EDPTuple resultInitial = runEDP.algorithm(initialParameters[0], initialParameters[1], initialParameters[2], eventTasks.get(i));
			if (!resultInitial.isResult()) {
				resultBoolean = false;
				break;
			} else {
				resultBoolean = true;
				bestAvgWCRT = bestAvgWCRT + resultInitial.getResponseTime();
			}
		}

		// Now we know the total response time of our initial solution & if it works

		// System.out.println("Initial WCRT: "+solutionResponseTime);

		//currentAvgWCRT = bestAvgWCRT;
		//ArrayList<ArrayList<testFormat>> currentPartition = eventTasks;
		ArrayList<ArrayList<testFormat>> bestPartition = eventTasks;

		while (t > 0.01) {

			ArrayList<ArrayList<testFormat>> neighbourPartition = new ArrayList<ArrayList<testFormat>>();
			if (numberPollingServers > 2) {
				neighbourPartition = swapN(eventTasks);
			} else {
				neighbourPartition = swap2(eventTasks);
			}

			int newAvgWCRT = 0;

			resultBoolean = false;

			// Test newly generated solution;
			for (int i = 0; i < numberPollingServers; i++) {
				EDPTuple resultInitial = runEDP.algorithm(initialParameters[0], initialParameters[1], initialParameters[2], eventTasks.get(i));
				if (!resultInitial.isResult()) {
					resultBoolean = false;
					//System.out.print("\t " + resultInitial.isResult() + " \t");
					break;
				} else {
					resultBoolean = true;
					newAvgWCRT = newAvgWCRT + resultInitial.getResponseTime();
				}
			}
			//newAvgWCRT = newAvgWCRT;

			// Print out the new partition

			delta = bestAvgWCRT - newAvgWCRT; // If delta is positive, the Neighbour is a better
																	// solution

			if (delta > 0 || probabilityFunc(delta, t)) {
				//currentPartition = neighbourPartition;
				//currentAvgWCRT = newAvgWCRT;

				//System.out.print("\t Current Total Response Time: " + currentAvgWCRT + "\t");

				if (resultBoolean && newAvgWCRT > 0 && newAvgWCRT < bestAvgWCRT) {
					bestPartition = neighbourPartition;
					bestAvgWCRT = newAvgWCRT;
					System.out.println("Best, correct partitions:");
					printOutPartitions(bestPartition);
					// System.out.print("Current best, correct Total Response time:
					// "+bestTotalResponseTime+" with partition: ?");
				}

			} else {
				//System.out.print("\t\t\t\t");
			}
			t = t * alpha; // Change of temperature for each round

			//System.out.print("\t Temperature: " + t + " \t");
			//printOutPartitions(currentPartition);
			//System.out.println("");
		}

		//System.out.println("Best avg response time after parition optimization: " + bestAvgWCRT);

		return bestPartition;

	}
		
	// Helper functions
		// Swap event tasks between two partitions
		public ArrayList<ArrayList<testFormat>> swap2(ArrayList<ArrayList<testFormat>> eventTasks) {
	
			// Extract the tasks of the two polling servers:
			ArrayList<testFormat> pollTasks1 = eventTasks.get(0);
			ArrayList<testFormat> pollTasks2 = eventTasks.get(1);
	
			// Generate random indices to swap:
			Random rand = new Random();
			int index1 = (int) rand.nextInt(pollTasks1.size());
			int index2 = (int) rand.nextInt(pollTasks2.size());
	
			// Swaps the elements
			testFormat temp = pollTasks1.get(index1);
			pollTasks1.set(index1, pollTasks2.get(index2));
			pollTasks2.set(index2, temp);
	
			// Packs up the arraylist
			ArrayList<ArrayList<testFormat>> result = new ArrayList<ArrayList<testFormat>>();
			result.add(pollTasks1);
			result.add(pollTasks2);
	
			return result;
		}
	
		// Swap event tasks between N partitions
		public ArrayList<ArrayList<testFormat>> swapN(ArrayList<ArrayList<testFormat>> eventTasks) {

			// Generate random order of swapping
			// Generate the list arraylist of integers from 1 to n
			ArrayList<Integer> orderList = new ArrayList<Integer>();
			for (int i = 0; i < numberPollingServers; i++) {
				orderList.add(i);
			}

			Collections.shuffle(orderList); // Shuffles the list

			// Convert order of swapping to pairs
			ArrayList<ArrayList<Integer>> swapPairs = extractPairsFromList(orderList);

			// System.out.println("Number of pairs: "+swapPairs.size());

			//
			ArrayList<ArrayList<testFormat>> result = eventTasks;

			// printOutPartitions(result);
			// System.out.println("\n");

			for (int i = 0; i < numberPollingServers; i++) {

				// Extract number for first pair
				ArrayList<Integer> pair = swapPairs.get(i);

				ArrayList<ArrayList<testFormat>> inputSwap = new ArrayList<ArrayList<testFormat>>();
				inputSwap.add(result.get(pair.get(0)));
				inputSwap.add(result.get(pair.get(1)));

				// System.out.println("Before: Pair:"+pair.get(0)+" "+pair.get(1));
				// printOutPartitions(result);
				// System.out.println("\n");

				// Perform swapping and put result back into result-arraylist
				ArrayList<ArrayList<testFormat>> swappedResult = swap2(inputSwap);
				result.set(pair.get(0), swappedResult.get(0));
				result.set(pair.get(1), swappedResult.get(1));

				// System.out.println("After: ");
				// printOutPartitions(result);
				// System.out.println("\n");

			}

			// printOutPartitions(result);
			// System.out.println("\n");

			return result;
		}

			// Given a list of partitions in an order, extract pairs to know which partitions should swap event tasks with each other
			public ArrayList<ArrayList<Integer>> extractPairsFromList(ArrayList<Integer> order) {
	
				// Result
				ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
	
				for (int i = 0; i < numberPollingServers - 1; i++) {
					ArrayList<Integer> temp = new ArrayList<Integer>();
					temp.add(order.get(i));
					temp.add(order.get(i + 1));
					result.add(temp);
				}
	
				// Last pair between first and last partition in order:
				ArrayList<Integer> temp = new ArrayList<Integer>();
				temp.add(order.get(numberPollingServers - 1));
				temp.add(order.get(0));
				result.add(temp);
	
				return result;
	
			}
	
	// Main function for finding optimal parameters for polling servers:
	public int[][] findOptimalParameters(ArrayList<ArrayList<testFormat>> partitions) {

		// # of polling servers
		int n = partitions.size();

		// Output
		int[][] result = new int[3][n];

		// Find best parameters for each polling server
		//int[] initialParameters = initialValues;

		for (int i = 0; i < n; i++) {

			// Get minimum budget parameter, based on minimum ticks needed to complete all partitions
			int min = getMinSupply(partitions.get(i));
			int[] initialParameters = generateInitialParameters(min,n, maxTimeDuration);
			
			//System.out.println("\n Min supply: "+min);
			//printOutPartitions(partitions);

			int[] optimalParameters = simulatedAnnealing(initialParameters, 100000, 0.5, partitions.get(i), min);
			result[0][i] = optimalParameters[0]; // Budget
			result[1][i] = optimalParameters[1]; // Period
			result[2][i] = optimalParameters[2]; // Deadline
			
			System.out.println("Optimal Parameters for polling server "+i+": Budget: "+optimalParameters[0]+" Period: "+optimalParameters[1]+" Deadline: "+optimalParameters[2]);
		}
		return result;
	}

	
	// Helper functions
		// Get minimum supply needed to successfully complete all event tasks in partition
		public int getMinSupply(ArrayList<testFormat> eventTasks) {
	
			int result = 0;
	
			for (int i = 0; i < eventTasks.size(); i++) {
				result = result + eventTasks.get(i).getDuration();
			}
	
			return result;
		}
				
		// Simulated Annealing to find optimal parameters:
		public int[] simulatedAnnealing(int[] initialSolution, int Tstart, double alpha, ArrayList<testFormat> eventTasks,
				int min) {
	
			Instant startTime = Instant.now();
	
			double t = Tstart;
			int delta = 0;
			EDPAlgorithm runEDP = new EDPAlgorithm();
	
			// Test WCRT of initial solution:
			EDPTuple resultInitial = runEDP.algorithm(initialSolution[0], initialSolution[1], initialSolution[2],
					eventTasks);
	
			int[] bestCorrectSolution = initialSolution;
			int bestCorrectResponseTime = resultInitial.getResponseTime();
			
			
			System.out.println("Initial result: "+resultInitial.isResult() + " Constraint: "+checkParameterConstraint(initialSolution));
	
			while (t > 0.01) {
				int[] neighbour = generateNeighbourNew(initialSolution[0], initialSolution[1], initialSolution[2], min);
	
				// Test WCRT of neighbour solution:
				EDPTuple resultNeighbour = runEDP.algorithm(neighbour[0], neighbour[1], neighbour[2], eventTasks);
	
				if (resultNeighbour.getResponseTime() == 0) {
					resultNeighbour = runEDP.algorithm(neighbour[0], neighbour[1], neighbour[2], eventTasks);
				}
	
				// System.out.println("Neighbour parameters: "+neighbour[0]+" "+neighbour[1]+"
				// "+neighbour[2]);
	
				delta = resultInitial.getResponseTime() - resultNeighbour.getResponseTime(); // If delta is positive, the
																								// Neighbour is a better
																								// solutio
				
				if (delta > 0 && resultNeighbour.isResult() && checkParameterConstraint(neighbour) || probabilityFunc(delta, t)) {
					initialSolution = neighbour;
					resultInitial = resultNeighbour;
	
					
					//System.out.println("Result: "+resultNeighbour.isResult());
					//System.out.println("Budget: "+neighbour[0]+" Period "+neighbour[1]+" Deadline: "+neighbour[2]);
					//System.out.println("Special constraint: "+checkParameterConstraint(neighbour));
					
					if (resultNeighbour.isResult() && resultNeighbour.getResponseTime() > 0
							&& resultNeighbour.getResponseTime() < bestCorrectResponseTime && checkParameterConstraint(neighbour)) {
						bestCorrectSolution = neighbour;
						bestCorrectResponseTime = resultNeighbour.getResponseTime();
						System.out.println("Current best, correct Response time: " + bestCorrectResponseTime
								+ " with parameters: " + neighbour[0] + " " + neighbour[1] + " " + neighbour[2]);
					}
	
				}
				t = t * alpha; // Change of temperature for each round
	
				// System.out.println("Temperature: "+t);
			}
	
			// System.out.println("Best WCRT with Simulated Annealing:
			// "+bestCorrectResponseTime);
	
			Instant endTime = Instant.now();
	
			// System.out.println("Simulated Annealing duration:
			// "+Duration.between(startTime,endTime).toMinutes()+" minutes");
	
			return bestCorrectSolution;
	
		}

			// Function to generate new solution from neighborhood
			public int[] generateNeighbourNew(int budget, int period, int deadline, int min) {
	
				// Pick random parameter to change
				Random rand = new Random();
				int choice = (int) rand.nextInt(3);
	
				// Pick to either increment or decrement
				int operation = (int) rand.nextInt(1);
				int res;
				int[] resArray = { budget, period, deadline };
				switch (choice) {
				case 0: // Change budget
					if (operation == 0 && budget > min) {
						res = budget - 1;
					} else if (operation == 1 && budget < period && budget < deadline) {
						res = budget + 1;
					} else {
						res = budget;
					}
					//System.out.println("reduced");
					
					resArray[0] = res;
					return resArray;
				case 1: // Change period
					if (operation == 0 && period != min && deadline < period && budget < period) {
						res = period - 1;
					} else if (operation == 1 && period < 12000) { // Hyperperiod
						res = period + 1;
					} else {
						res = period;
					}
					resArray[1] = res;
					return resArray;
				case 2: // Change deadline
					if (operation == 0 && deadline != min && budget < deadline - 1) {
						res = deadline - 1;
					} else if (operation == 1 && deadline < period) {
						res = deadline + 1;
					} else {
						res = deadline;
					}
					resArray[2] = res;
					return resArray;
				}
				return resArray;
			}
	
			
	// Main function for running the polling servers with their optimal parameters and partitions to return their individual WCRTs
	public int[] optimalPollingServerRun(ArrayList<ArrayList<testFormat>> partitions, int[][] parameters) {

		int[] resultWCRTs = new int[numberPollingServers];

		EDPAlgorithm runEDP = new EDPAlgorithm();

		for (int i = 0; i < numberPollingServers; i++) {
			EDPTuple result = runEDP.algorithm(parameters[0][i], parameters[1][i], parameters[2][i], partitions.get(i));
			resultWCRTs[i] = result.getResponseTime();
		}

		return resultWCRTs;

	}
	
	
	// Main function to get the average WCRT of all polling servers
	public int finalEventWCRT(int[] eventTasksWCRTs, ArrayList<ArrayList<testFormat>> partitions, ArrayList<testFormat> eventTasks) { // We are taking the average

		int result = 0;
		
		// Number of event tasks
		int n = eventTasks.size();

		for (int i = 0; i < eventTasksWCRTs.length; i++) {
			result = result + (eventTasksWCRTs[i]*(partitions.get(i).size()/n));
		}
		return result;
	}
	
	
	// Main function to get the final, average WCRT of the first EDF run and all the EDP-runs using the polling servers
	public int finalWCRT(int EDFWCRT, int EDPWCRT, int timeTasksSize, int eventTasksSize) {

		double timeTasksWeight = (double) timeTasksSize / (timeTasksSize + eventTasksSize);
		double eventTasksWeight = (double) eventTasksSize / (timeTasksSize + eventTasksSize);

		return (int) Math.ceil(timeTasksWeight * EDFWCRT + eventTasksWeight * EDPWCRT);
	}

	
	// Main function to convert polling servers to time tasks for last EDF run
	public ArrayList<testFormat> createPollingServerTasks(ArrayList<testFormat> timeTasks, int[][] parameters) {

		int n = parameters[0].length;

		for (int i = 0; i < n; i++) {
			testFormat pollingServerTask = new testFormat("pollingServer " + i, parameters[0][i], parameters[1][i],
					"TT", 7, parameters[2][i], 0);
			timeTasks.add(pollingServerTask);
		}

		return timeTasks;
	}

	
	// Helper functions used by multiple functions	
	// Prints out partitions in a readable format
	public void printOutPartitions(ArrayList<ArrayList<testFormat>> partitions) {

		// Number of partitions:
		int n = partitions.size();

		// System.out.println("Size: "+n);

		for (int i = 0; i < n; i++) {

			// Get # of tasks in single partition
			int m = partitions.get(i).size();

			System.out.print("\t Tasks in parition " + i + ": \t");

			for (int j = 0; j < m; j++) {
				System.out.print(" " + partitions.get(i).get(j).getName());

				// Make comma between tasks, except at last one:
				if (j != m - 1) {
					System.out.print(",");
				}
			}
		}

	}

	
	// Probability function for Simulated Annealing algorithm
	public boolean probabilityFunc(int delta, double t) {
		double probability = Math.exp(-(1 / t) * delta);

		Random rand = new Random();
		int pick = (int) rand.nextInt(100);

		if (probability >= (double) pick) {
			return true;
		} else {
			return false;
		}

	}

	public boolean checkParameterConstraint(int[] parameters) {
		
		if(parameters[0]*numberPollingServers+maxTimeDuration<parameters[2]) {
			return true;
		} else {
			return false;
		}
		
	}

	public int[] generateInitialParameters(int min, int numberPollingServers, int maxTimeDuration) {
		
		
		
		
		//int[] result = {(1000-maxTimeDuration)/numberPollingServers, 1000, 1000 };
		
		if(min==0) {
			int[] result = {(1000-maxTimeDuration)/numberPollingServers, 1000, 1000 };
			//System.out.println("Initial generated budget: "+result[0]);
			return result;
		} else {
			int[] result = {(int) Math.ceil(min*1.2), (min*numberPollingServers+maxTimeDuration), (min*numberPollingServers+maxTimeDuration)};
			//System.out.println("Initial generated budget: "+result[0]);
			return result;
		}
	}

	// Function to check seperatino constraint of event tasks is satisfied
	public boolean checkSeperationConstraint(ArrayList<ArrayList<testFormat>> partitions) {
		
		for(int i = 0; i<numberPollingServers; i++) {
			
			int seperationNumber = 0;
			int tasksPollingServer = partitions.get(i).size();
			boolean firstSeperationNumberFound = false;
			
			for( int j = 0; j<tasksPollingServer; j++) {
				
				
				if(partitions.get(i).get(j).getSeparation() != seperationNumber && partitions.get(i).get(j).getSeparation() != 0) {
					return false;
				}				
				
				if(partitions.get(i).get(j).getSeparation() != 0 && !firstSeperationNumberFound) {
					seperationNumber =  partitions.get(i).get(j).getSeparation();
					firstSeperationNumberFound = true;
				}
				
			}
			
		}
		
		return true;
		
		
	}
	
}
