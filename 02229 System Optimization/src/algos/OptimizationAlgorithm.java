package algos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import objectClasses.EDPTuple;
import objectClasses.testFormat;
import java.util.Random;
import java.util.stream.IntStream;
import java.time.Duration;
import java.time.Instant;

public class OptimizationAlgorithm {

	int[] initialValues = { 250, 1200, 1200 };
	private int[][] initialParameters;
	private int numberPollingServers;
	private int[] demands;
	private int[][] allParameters;
	ArrayList<ArrayList<int[]>> acceptableParameters = new ArrayList<ArrayList<int[]>>();
	private double utilizationTimeTasks;

	public OptimizationAlgorithm(int numberPollingServers, int[] demands,
			ArrayList<testFormat> timeTasks) {
		this.numberPollingServers = numberPollingServers;
		this.demands = demands;
		this.utilizationTimeTasks = calculateUtilization(timeTasks);
	}

	public void setNumberPollingServers(int numberPollingServers) {
		this.numberPollingServers = numberPollingServers;
		this.allParameters = new int[3][numberPollingServers];
		this.initialParameters = new int[3][numberPollingServers];

	}

	// Main function for finding optimal number of polling servers:
	public ArrayList<ArrayList<testFormat>> findNumberPollingServers(ArrayList<testFormat> eventTasks)
			throws Exception {

		int bestNumberPolling = 1;
		int bestTotalWCRT = Integer.MAX_VALUE;
		ArrayList<ArrayList<testFormat>> bestPartitions = new ArrayList<ArrayList<testFormat>>();
		int minNumPollingServers = getSeperationNum(eventTasks);
		for (int i = minNumPollingServers; i < 11; i++) {

			// Deepcopy eventTasks
			ArrayList<testFormat> eventTasksCopy = new ArrayList<testFormat>();
			for (int j = 0; j < eventTasks.size(); j++) {
				eventTasksCopy.add(eventTasks.get(j).clone());
			}

			ArrayList<ArrayList<testFormat>> currentPartitions = new ArrayList<ArrayList<testFormat>>();
			try {
				currentPartitions = getPartitionsPollingServers(eventTasksCopy, i);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// optimizeAlgo.printOutPartitions(currentPartitions);

			int currentWCRT = testPartitions(currentPartitions);

			// System.out.println("\nCurrent response time: " + currentWCRT+ " Number
			// polling servers "+i);

			if (currentWCRT < bestTotalWCRT) {
				bestTotalWCRT = currentWCRT;
				bestNumberPolling = i;
				bestPartitions = currentPartitions;
			}

			// System.out.println("Best WCRT after finding # of polling server: " +
			// bestTotalWCRT);
			// System.out.println("Optimal number of polling servers: " +
			// bestNumberPolling);

		}

		// System.exit(0);

		printOutPartitions(bestPartitions);
		System.out.print("\n");

		if (!checkSeparationConstraint(bestPartitions)) {
			throw new Exception("Paritions did not satisfy separation constraint");
		}

		// Get initial partitions

		return bestPartitions;

	}

	// Helper functions:
	// Returns partitions, based on the given event tasks and the number of polling
	// servers requested
	public ArrayList<ArrayList<testFormat>> getPartitionsPollingServers(ArrayList<testFormat> eventTasks,
			int numPolling) throws Exception {
		// System.out.println("Size: "+eventTasks.size());
		// dataHandler dataHandler = new dataHandler();
		ArrayList<ArrayList<testFormat>> result = new ArrayList<ArrayList<testFormat>>();
		ArrayList<testFormat> sortedEventTasks = sortTasksDuration(eventTasks);
		int numSeparation = getSeperationNum(sortedEventTasks) - 1;
		int[] polDura = new int[numPolling];

		ArrayList<Integer> removeIndexs = new ArrayList<Integer>();
		if (numSeparation > 0) {
			for (int j = 1; j <= numSeparation; j++) {
				ArrayList<testFormat> temp = new ArrayList<testFormat>();
				for (int i = 0; i < sortedEventTasks.size(); i++) {
					if (sortedEventTasks.get(i).getSeparation() == j) {
						temp.add(sortedEventTasks.get(i));
						removeIndexs.add(i);
						polDura[sortedEventTasks.get(i).getSeparation() - 1] += sortedEventTasks.get(i).getDuration();
					}
				}
				result.add(temp);
			}
			Collections.sort(removeIndexs, Collections.reverseOrder());
			for (int i = 0; i < removeIndexs.size(); i++) {
				sortedEventTasks.remove((int) removeIndexs.get(i));
			}

		}
		for (int i = numSeparation; i < numPolling; i++) {
			ArrayList<testFormat> temp = new ArrayList<testFormat>();
			temp.add(sortedEventTasks.get(0));
			polDura[i] = temp.get(0).getDuration();
			result.add(temp);
			sortedEventTasks.remove(0);
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
		/*
		 * System.out.println("-----------------------------\n"); for (int i = 0;
		 * i<result.size();i++) { ArrayList<testFormat> temp = result.get(i); int
		 * tempInt = 0; for (int j = 0; j< temp.size();j++) {
		 * System.out.print(temp.get(j).getName()+" is "+temp.get(j).getSeparation()+
		 * "  ---  "); tempInt += temp.get(j).getDuration(); }
		 * System.out.println(tempInt); }
		 */
		if (checkSeparationConstraint(result)) {
			return result;
		} else {
			throw new Exception("partition does not satisfy the seperartion constraint");
		}
	}

	public int getSeperationNum(ArrayList<testFormat> eventTasks) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < eventTasks.size(); i++) {
			if (!result.contains(eventTasks.get(i).getSeparation())) {
				result.add(eventTasks.get(i).getSeparation());
			}
		}
		return result.size();
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
			int[] para = findInitialSolution(partitions.get(i), 0);
			EDPTuple resultInitial = runEDP.algorithm(para[0], para[1], para[2], partitions.get(i));
			// System.out.println("Result: "+resultInitial.isResult());

			if (!resultInitial.isResult()) {
				return Integer.MAX_VALUE; // If one polling server can not satisfy the tasks it is given, let it fail
			} else {
				totalResponseTime = totalResponseTime + resultInitial.getResponseTime();
			}
		}

		return totalResponseTime / n;
	}

	// Main function for finding optimal parameters for polling servers:
	public int[][] findOptimalParameters(ArrayList<ArrayList<testFormat>> partitions, ArrayList<testFormat> eventTasks)
			throws Exception {

		for (int i = 0; i < numberPollingServers; i++) {

			int[][] initialGeneratedParameters = findIntialSolutionUpper(partitions);

			for (int j = 0; j < 3; j++) {
				allParameters[j][i] = initialGeneratedParameters[j][i];
				initialParameters[j][i] = initialGeneratedParameters[j][i];
			}

		}
		checkUtilizationConstraint(allParameters);
		if (checkParameterDemand(allParameters, demands)) {
			System.out.println("Works");
		}

		// return testedParameters;

		// Output
		int[][] result = new int[3][numberPollingServers];

		// Find best parameters for each polling server
		// int[] initialParameters = initialValues;

		// System.out.println("allparameters length: "+allParameters[0].length);

		for (int i = 0; i < numberPollingServers; i++) {

			int[] startingParameters = { allParameters[0][i], allParameters[1][i], allParameters[2][i] };

			int[] optimalParameters = simulatedAnnealing(startingParameters, 1000000, 0.99999, partitions.get(i), i);
			result[0][i] = optimalParameters[0]; // Budget
			result[1][i] = optimalParameters[1]; // Period
			result[2][i] = optimalParameters[2]; // Deadline

			System.out.println("Optimal Parameters for polling server " + i + ": Budget: " + optimalParameters[0]
					+ " Period: " + optimalParameters[1] + " Deadline: " + optimalParameters[2]);
		}

		result = findBestWorkingSolutions(partitions, eventTasks);

		return result;

	}

	public boolean checkParameterDemand(int[][] parameters, int[] demands) {

		boolean result = true;
		int pollingBudget1000 = 0;

		// Figure out the 1000-tick demand by the total budget of the polling tasks
		for (int i = 0; i < parameters[0].length; i++) {
			double multiple = 1000 / parameters[1][i];
			int budget1000 = (int) Math.ceil(parameters[0][i] * multiple);
			pollingBudget1000 = pollingBudget1000 + budget1000;

			// System.out.println("Budget per 1000: "+budget1000);
		}

		// Check if adding polling demand exceeds demand in any run of the EDF-algorithm
		for (int i = 1; i < 13; i++) {

			// System.out.println("New demand at
			// "+(i*1000)+":"+(demands[i-1]+pollingBudget1000*i));

			if (demands[i - 1] + pollingBudget1000 * i > i * 1000) {
				// System.out.print("Demand: False \t");
				return false;
			}
		}
		// System.out.print("Demand: True \t");
		return true;

	}

	public boolean checkSoftParameterConstraints(int[] parameters) {

		if (parameters[0] < parameters[2] && parameters[2] <= parameters[1] && parameters[0] < parameters[2]
				&& parameters[0] > 0 && parameters[1] > 0 && parameters[2] > 0 && parameters[0] < 12000
				&& parameters[1] < 12000 && parameters[2] < 12000) {
			return true;
		} else {
			return false;
		}

	}

	/*
	 * public int[] simulatedAnnealingNew(int[] initialSolution, int Tstart, double
	 * alpha, ArrayList<testFormat> eventTasks, int index) throws Exception {
	 * 
	 * Instant startTime = Instant.now();
	 * 
	 * double t = Tstart; int delta = 0; EDPAlgorithm runEDP = new EDPAlgorithm();
	 * 
	 * // Test WCRT of initial solution: EDPTuple currentResult =
	 * runEDP.algorithm(initialSolution[0], initialSolution[1], initialSolution[2],
	 * eventTasks);
	 * 
	 * int[] currentParameters = initialSolution; int currentWCRT =
	 * currentResult.getResponseTime();
	 * 
	 * int[] bestParameters = currentParameters; int bestWCRT = currentWCRT; boolean
	 * jumpChosen = true;
	 * 
	 * ArrayList<int[]> acceptableParametersThis = new ArrayList<int[]>();
	 * acceptableParametersThis.add(bestParameters);
	 * 
	 * System.out.println("Initial paramters: " + bestParameters[0] + " " +
	 * bestParameters[1] + " " + bestParameters[2] + " Initial WCRT: " + bestWCRT);
	 * 
	 * while (t > Tstart / 100) {
	 * 
	 * // Local search if large jump hit a solution if (jumpChosen &&
	 * currentResult.isResult()) {
	 * 
	 * // System.out.println("Neighbourhood hit!");
	 * 
	 * int[] bestLocalResult = localSearch(currentParameters, eventTasks,
	 * currentWCRT); currentParameters[0] = bestLocalResult[0]; currentParameters[1]
	 * = bestLocalResult[1]; currentParameters[2] = bestLocalResult[2]; currentWCRT
	 * = bestLocalResult[3]; currentResult = runEDP.algorithm(currentParameters[0],
	 * currentParameters[1], currentParameters[2], eventTasks);
	 * 
	 * int[][] testAllParameters = allParameters;
	 * 
	 * for (int i = 0; i < 3; i++) { testAllParameters[i][index] =
	 * currentParameters[i]; }
	 * 
	 * acceptableParametersThis.add(currentParameters); //
	 * System.out.println(" Added: " + currentParameters[0] + " " + //
	 * currentParameters[1] + " " + currentParameters[2]);
	 * 
	 * if (currentWCRT < bestWCRT && checkParameterConstraints(currentParameters) &&
	 * checkParameterDemand(testAllParameters, demands)) { bestWCRT = currentWCRT;
	 * bestParameters = currentParameters;
	 * 
	 * System.out.println("Best parameters: " + bestParameters[0] + " " +
	 * bestParameters[1] + " " + bestParameters[2] + " Initial WCRT: " + bestWCRT);
	 * 
	 * }
	 * 
	 * jumpChosen = false; }
	 * 
	 * // Large jump int[] neighbourParameters =
	 * generateNeighbourNew(currentParameters[0], currentParameters[1],
	 * currentParameters[2]); EDPTuple neighbourResult =
	 * runEDP.algorithm(neighbourParameters[0], neighbourParameters[1],
	 * neighbourParameters[2], eventTasks);
	 * 
	 * delta = currentResult.getResponseTime() - neighbourResult.getResponseTime();
	 * 
	 * if ((delta > 0 && neighbourResult.isResult() &&
	 * checkSoftParameterConstraints(currentParameters)) || probabilityFunc(delta,
	 * t)) { jumpChosen = true; currentParameters = neighbourParameters; currentWCRT
	 * = neighbourResult.getResponseTime(); currentResult = neighbourResult;
	 * 
	 * int[][] testAllParameters = allParameters;
	 * 
	 * for (int i = 0; i < 3; i++) { testAllParameters[i][index] =
	 * currentParameters[i]; }
	 * 
	 * 
	 * System.out.println(" " + currentParameters[0] + " " + currentParameters[1] +
	 * " " + currentParameters[2] + " " + currentWCRT + " " +
	 * currentResult.isResult() + " " + checkParameterDemand(testAllParameters,
	 * demands) + " " + checkParameterConstraints(currentParameters));
	 * 
	 * 
	 * if (delta > 0 && neighbourResult.isResult() &&
	 * checkSoftParameterConstraints(currentParameters)) {
	 * acceptableParametersThis.add(neighbourParameters); }
	 * 
	 * if (neighbourResult.isResult() && currentWCRT > 0 && currentWCRT < bestWCRT
	 * && checkParameterDemand(testAllParameters, demands) &&
	 * checkSoftParameterConstraints(currentParameters) &&
	 * checkParameterConstraints(currentParameters)) { bestParameters =
	 * currentParameters; bestWCRT = currentWCRT;
	 * System.out.println("Current best, correct Response time: " + bestWCRT +
	 * " with parameters: " + bestParameters[0] + " " + bestParameters[1] + " " +
	 * bestParameters[2] + " " + checkParameterConstraints(currentParameters)); }
	 * 
	 * } t = t * alpha; // Change of temperature for each round
	 * 
	 * // System.out.println("Temperature: "+t);
	 * 
	 * Instant endTime = Instant.now(); if (Duration.between(startTime,
	 * endTime).toMinutes() > 0) { break; }
	 * 
	 * }
	 * 
	 * // System.out.println("Best WCRT with Simulated Annealing: //
	 * "+bestCorrectResponseTime);
	 * 
	 * Instant endTime = Instant.now();
	 * 
	 * System.out.println( "Simulated Annealing duration:" +
	 * Duration.between(startTime, endTime).toMinutes() + " minutes");
	 * 
	 * System.out.println("Current best, correct Response time: " + bestWCRT +
	 * " with parameters: " + bestParameters[0] + " " + bestParameters[1] + " " +
	 * bestParameters[2]);
	 * 
	 * acceptableParameters.add(acceptableParametersThis);
	 * 
	 * return bestParameters;
	 * 
	 * }
	 */
	public int[] simulatedAnnealing(int[] initialSolution, int Tstart, double alpha, ArrayList<testFormat> eventTasks,
			int index) throws Exception {

		Instant startTime = Instant.now();

		double t = Tstart;
		int delta = 0;
		EDPAlgorithm runEDP = new EDPAlgorithm();

		// Test WCRT of initial solution:
		EDPTuple currentResult = runEDP.algorithm(initialSolution[0], initialSolution[1], initialSolution[2],
				eventTasks);

		int[] currentParameters = initialSolution;
		int currentWCRT = currentResult.getResponseTime();

		int[] bestParameters = currentParameters;
		int bestWCRT = currentWCRT;

		ArrayList<int[]> acceptableParametersThis = new ArrayList<int[]>();
		acceptableParametersThis.add(bestParameters);

		System.out.println("Initial paramters: " + bestParameters[0] + " " + bestParameters[1] + " " + bestParameters[2]
				+ " Initial WCRT: " + bestWCRT);

		while (true) {

			// Search neighbour
			int[] neighbourParameters = generateNeighbourNew(currentParameters[0], currentParameters[1],
					currentParameters[2], initialSolution);
			EDPTuple neighbourResult = runEDP.algorithm(neighbourParameters[0], neighbourParameters[1],
					neighbourParameters[2], eventTasks);

			delta = currentResult.getResponseTime() - neighbourResult.getResponseTime();

			if ((delta > 0 && neighbourResult.isResult() && checkParameterConstraints(currentParameters))
					|| probabilityFunc(delta, t)) {
				currentParameters = neighbourParameters;
				currentWCRT = neighbourResult.getResponseTime();
				currentResult = neighbourResult;

				int[][] testAllParameters = allParameters;

				// System.out.println(neighbourParameters[0]+" "+neighbourParameters[1]+"
				// "+neighbourParameters[2]+" "+neighbourResult.getResponseTime());
				// System.out.println("Temperature: "+t);
				// System.out.println(currentParameters[0]+" "+currentParameters[1]+"
				// "+currentParameters[2]+" "+currentWCRT);
				// System.out.println("");

				for (int i = 0; i < 3; i++) {
					testAllParameters[i][index] = currentParameters[i];
				}

				if (neighbourResult.isResult() && currentWCRT > 0 && currentWCRT < bestWCRT
						&& checkParameterConstraints(currentParameters)) {
					acceptableParametersThis.add(neighbourParameters);
					bestParameters = currentParameters;
					bestWCRT = currentWCRT;
					System.out.println("Current best, correct Response time: " + bestWCRT + " with parameters: "
							+ bestParameters[0] + " " + bestParameters[1] + " " + bestParameters[2] + " "
							+ checkParameterConstraints(currentParameters));
				}

			}
			t = t * alpha; // Change of temperature for each round

			// System.out.println("Temperature: "+t);

			Instant endTime = Instant.now();
			if (Duration.between(startTime, endTime).toSeconds() > 300) {
				break;
			}

		}

		// System.out.println("Best WCRT with Simulated Annealing:
		// "+bestCorrectResponseTime);

		Instant endTime = Instant.now();

		System.out.println(
				"Simulated Annealing duration:" + Duration.between(startTime, endTime).toSeconds() + " seconds");

		System.out.println("Current best, correct Response time: " + bestWCRT + " with parameters: " + bestParameters[0]
				+ " " + bestParameters[1] + " " + bestParameters[2]);

		acceptableParameters.add(acceptableParametersThis);

		return bestParameters;

	}

	public int[] generateNeighbourNew(int budget, int period, int deadline, int[] initialParameters) {

		// Pick random parameter to change
		Random rand = new Random();
		int choice = (int) rand.nextInt(3);

		// Pick to either increment or decrement
		int operation = (int) rand.nextInt(2);
		int res;
		int[] resArray = { budget, period, deadline };
		switch (choice) {
		case 0: // Change budget
			if (operation == 0 && budget > 1) {
				res = budget - 1;
			} else if (operation == 1 && budget < period && budget < deadline) {
				res = budget + 1;
			} else {
				res = budget;
			}

			// System.out.println("reduced");

			resArray[0] = res;
			return restartSimulatedAnnealing(resArray, initialParameters);
		case 1: // Change period
			if (operation == 0 && period > 1 && budget < period) {
				res = period - 1;
			} else if (operation == 1 && period < 12000) { // Hyperperiod
				res = period + 1;
			} else {
				res = period;
			}
			resArray[1] = res;
			return restartSimulatedAnnealing(resArray, initialParameters);
		case 2: // Change deadline
			if (operation == 0 && deadline > 1 && budget < deadline - 1) {
				res = deadline - 1;
			} else if (operation == 1) {
				res = deadline + 1;
			} else {
				res = deadline;
			}
			resArray[2] = res;
			return restartSimulatedAnnealing(resArray, initialParameters);
		}
		return restartSimulatedAnnealing(resArray, initialParameters);
	}

	// Function to check separation constraint of event tasks is satisfied
	public boolean checkSeparationConstraint(ArrayList<ArrayList<testFormat>> partitions) {

		for (int i = 0; i < numberPollingServers; i++) {

			int seperationNumber = 0;
			int tasksPollingServer = partitions.get(i).size();
			boolean firstSeperationNumberFound = false;

			for (int j = 0; j < tasksPollingServer; j++) {

				if (partitions.get(i).get(j).getSeparation() != seperationNumber
						&& partitions.get(i).get(j).getSeparation() != 0) {
					return false;
				}

				if (partitions.get(i).get(j).getSeparation() != 0 && !firstSeperationNumberFound) {
					seperationNumber = partitions.get(i).get(j).getSeparation();
					firstSeperationNumberFound = true;
				}

			}

		}

		return true;

	}

	public int[][] findBestWorkingSolutions(ArrayList<ArrayList<testFormat>> partitions,
			ArrayList<testFormat> eventTasks) {

		int[] acceptableParametersSizes = new int[numberPollingServers];

		ArrayList<ArrayList<int[]>> trueParameters = new ArrayList<ArrayList<int[]>>();

		int max = 0;

		int[][] bestParameters = new int[3][numberPollingServers];
		int[][] currentParameters = new int[3][numberPollingServers];

		for (int i = 0; i < numberPollingServers; i++) {

			// Removed duplicates from collected parameters:
			for (int j = 0; j < acceptableParameters.get(i).size(); j++) {
				int[] temp = acceptableParameters.get(i).get(j);

				for (int k = j + 1; k < acceptableParameters.get(i).size(); k++) {
					if (temp == acceptableParameters.get(i).get(k)) {
						acceptableParameters.get(i).remove(k);
					}
				}
			}

			trueParameters.add(new ArrayList<int[]>());

			acceptableParametersSizes[i] = acceptableParameters.get(i).size();

			if (acceptableParametersSizes[i] > max) {
				max = acceptableParametersSizes[i];
			}
			/*
			
			for (int j = 0; j < acceptableParameters.get(i).size(); j++) {
				System.out.print(Arrays.toString(acceptableParameters.get(i).get(j)) + " \t");
			}
			System.out.println("");
			*/
			int trueParameterArray[] = new int[3];
			// Initialize with best parameters for all polling servers:
			for (int j = 0; j < 3; j++) {
				currentParameters[j][i] = acceptableParameters.get(i).get(acceptableParametersSizes[i] - 1)[j];

				trueParameterArray[j] = currentParameters[j][i];

				trueParameters.get(i).add(trueParameterArray);

			}

		}

		for (int i = 0; i < numberPollingServers; i++) {
			bestParameters[0][i] = currentParameters[0][i];
			bestParameters[1][i] = currentParameters[1][i];
			bestParameters[2][i] = currentParameters[2][i];

		}

		for (int i = 0; i < numberPollingServers; i++) {
			// System.out.println(" Best found parameters for polling server "+i+":
			// "+bestParameters[0][i]+" "+bestParameters[1][i]+" "+bestParameters[2][i]);
			Collections.reverse(acceptableParameters.get(i));
		}

		double bestWCRT = Integer.MAX_VALUE;
		checkUtilizationConstraint(currentParameters);

		if (checkParameterDemand(currentParameters, demands)) {
			int[] WCRTs = optimalPollingServerRun(partitions, currentParameters);
			bestWCRT = finalEventWCRT(WCRTs, partitions, eventTasks);
			// System.out.println("First check ok:"+bestWCRT);
		}

		for (int i = 1; i < max; i++) {

			for (int j = 0; j < numberPollingServers; j++) {

				// Change one set of parameters for j'th polling server

				if (acceptableParametersSizes[j] > i) {
					for (int k = 0; k < 3; k++) {
						currentParameters[k][j] = acceptableParameters.get(j).get(i)[k];
						// System.out.println("Yes: " + acceptableParameters.get(j).get(i)[k]);
					}
				} else {
					continue;
				}

				for (int h = 0; h < numberPollingServers; h++) {
					// System.out.print(" Testing " + i + " " + j + ": " + currentParameters[0][h] +
					// " "+ currentParameters[1][h] + " " + currentParameters[2][h]+"
					// "+(checkParameterDemand(currentParameters, demands))+" \t");
				}

				if (checkParameterDemand(currentParameters, demands) && checkUtilizationConstraint(currentParameters)) {
					int[] WCRTs = optimalPollingServerRun(partitions, currentParameters);
					double currentWCRT = finalEventWCRT(WCRTs, partitions, eventTasks);

					// System.out.println("Current average WCRT: " + currentWCRT+" Best:
					// "+bestWCRT);

					// Add the paramters for pollingserver j that to the trueParmaters arraylist for
					// later use in mutation algorithm (Important we only have correct solutions)
					int trueParameterArray[] = new int[3];
					for (int h = 0; h < 3; h++) {
						trueParameterArray[h] = currentParameters[h][j];
					}
					trueParameters.get(j).add(trueParameterArray);

					if (currentWCRT < bestWCRT) {
						for (int h = 0; h < numberPollingServers; h++) {
							bestParameters[0][h] = currentParameters[0][h];
							bestParameters[1][h] = currentParameters[1][h];
							bestParameters[2][h] = currentParameters[2][h];
							// System.out.print(" Best " + i + " " + j + ": " + bestParameters[0][h] + " "+
							// bestParameters[1][h] + " " + bestParameters[2][h]+"
							// "+(checkParameterDemand(currentParameters, demands))+" \t");
						}
						bestWCRT = currentWCRT;

					}
				}

			}

		}

		for (int i = 0; i < numberPollingServers; i++) {
			System.out.println(" Best found parameters for polling server " + i + ": " + bestParameters[0][i] + " "
					+ bestParameters[1][i] + " " + bestParameters[2][i]);
		}

		checkParameterDemand(bestParameters, demands);
		checkUtilizationConstraint(bestParameters);

		// Do mutation

		int[][] mutatedParameters = mutationAlgorithm(trueParameters, acceptableParametersSizes, bestParameters,
				bestWCRT, partitions, eventTasks);
		int[] mutatedWCRTs = optimalPollingServerRun(partitions, mutatedParameters);
		double mutatedWCRT = finalEventWCRT(mutatedWCRTs, partitions, eventTasks);

		return finetuning(mutatedParameters, (int) mutatedWCRT, partitions, eventTasks);

	}

	public int[][] finetuning(int[][] parameters, int bestWCRT, ArrayList<ArrayList<testFormat>> partitions,
			ArrayList<testFormat> eventTasks) {

		System.out.println("Finetuning started");

		int[][] bestParameters = new int[3][numberPollingServers];

		for (int i = 0; i < numberPollingServers; i++) {
			bestParameters[0][i] = parameters[0][i];
			bestParameters[1][i] = parameters[1][i];
			bestParameters[2][i] = parameters[2][i];
		}

		int currentWCRT = 0;

		for (int i = 0; i < 10000; i++) {

			for (int j = 0; j < numberPollingServers; j++) {
				System.out.print(parameters[0][j] + " " + parameters[1][j] + " " + parameters[2][j] + " \t");
			}
			System.out.println();

			Random rand = new Random();
			int pollingServer = (int) (rand.nextInt(parameters[0].length));
			int choice = (int) (rand.nextInt(3));
			int operation = (int) (rand.nextInt(2));
			EDPAlgorithm runEDP = new EDPAlgorithm();

			if (operation == 0) {
				parameters[choice][pollingServer] = parameters[choice][pollingServer] - 1;
				int[] temp = { parameters[0][pollingServer], parameters[1][pollingServer],
						parameters[2][pollingServer] };
				if (!checkSoftParameterConstraints(temp) || !runEDP.algorithm(temp[0], temp[1], temp[2], partitions.get(pollingServer)).isResult()) {
					parameters[choice][pollingServer] = parameters[choice][pollingServer] + 1;
					continue;
				}
			} else {
				parameters[choice][pollingServer] = parameters[choice][pollingServer] + 1;
				int[] temp = { parameters[0][pollingServer], parameters[1][pollingServer],
						parameters[2][pollingServer] };
				if (!checkSoftParameterConstraints(temp) || !runEDP.algorithm(temp[0], temp[1], temp[2], partitions.get(pollingServer)).isResult()) {
					parameters[choice][pollingServer] = parameters[choice][pollingServer] - 1;
					continue;
				}
			}

			checkUtilizationConstraint(parameters);

			if (checkParameterDemand(parameters, demands)) {
				int[] WCRTs = optimalPollingServerRun(partitions, parameters);
				currentWCRT = (int) finalEventWCRT(WCRTs, partitions, eventTasks);
				// System.out.println("First check ok:"+bestWCRT);
				

				if (currentWCRT < bestWCRT) {
					System.out.println(" Best found parameters: " + currentWCRT + "");
					for (int j = 0; j < numberPollingServers; j++) {
						bestParameters[0][j] = parameters[0][j];
						bestParameters[1][j] = parameters[1][j];
						bestParameters[2][j] = parameters[2][j];
						System.out.print(
								bestParameters[0][j] + " " + bestParameters[1][j] + " " + bestParameters[2][j] + " \t");
					}
					System.out.println("");
					bestWCRT = currentWCRT;

				}

			}

		}

		for (int j = 0; j < numberPollingServers; j++) {
			System.out.println("Final parameters: ");
			System.out.print(bestParameters[0][j] + " " + bestParameters[1][j] + " " + bestParameters[2][j] + " \t");
		}

		return bestParameters;

	}

	// Main function for running the polling servers with their optimal parameters
	// and partitions to return their individual WCRTs
	public int[] optimalPollingServerRun(ArrayList<ArrayList<testFormat>> partitions, int[][] parameters) {

		int[] resultWCRTs = new int[numberPollingServers];

		EDPAlgorithm runEDP = new EDPAlgorithm();

		for (int i = 0; i < numberPollingServers; i++) {
			EDPTuple result = runEDP.algorithm(parameters[0][i], parameters[1][i], parameters[2][i], partitions.get(i));
			resultWCRTs[i] = result.getResponseTime();
			// System.out.println(resultWCRTs[i]);
		}

		return resultWCRTs;

	}

	// Main function to get the average WCRT of all polling servers
	public double finalEventWCRT(int[] eventTasksWCRTs, ArrayList<ArrayList<testFormat>> partitions,
			ArrayList<testFormat> eventTasks) { // We are taking the average

		double result = 0;

		// Number of event tasks
		double n = (double) eventTasks.size();

		for (int i = 0; i < eventTasksWCRTs.length; i++) {
			// System.out.println(eventTasksWCRTs[i]);
			double temp = ((partitions.get(i).size() / n));
			// System.out.println(temp);
			// System.out.println(eventTasksWCRTs[i]*temp);
			result = (result + eventTasksWCRTs[i] * temp);
		}
		return result;
	}

	// Main function to convert polling servers to time tasks for last EDF run
	public ArrayList<testFormat> createPollingServerTasks(ArrayList<testFormat> timeTasks, int[][] parameters) {

		int n = parameters[0].length;

		for (int i = 0; i < n; i++) {
			testFormat pollingServerTask = new testFormat("pollingServer " + i, parameters[0][i], parameters[1][i],
					"TT", 7, parameters[2][i], 0);
			timeTasks.add(pollingServerTask);
			System.out.println(" Polling server created with aprameters: " + parameters[0][i] + " " + parameters[1][i]
					+ " " + parameters[2][i]);
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

	public int[] findInitialSolution(ArrayList<testFormat> eventTasks, int counter) {

		int workingWCRT = Integer.MAX_VALUE;
		EDPAlgorithm runEDP = new EDPAlgorithm();
		int[] bestParameters = new int[3];

		for (int staticParameters = 50; staticParameters < 1001; staticParameters = staticParameters + 10) {

			int prevWCRT = 0;

			for (int i = 0; i < staticParameters; i++) {
				EDPTuple testResult = runEDP.algorithm(i, staticParameters, staticParameters, eventTasks);

				if (workingWCRT > testResult.getResponseTime() && testResult.isResult()) {

					if (counter == 0) {
						workingWCRT = testResult.getResponseTime();
						bestParameters[0] = i;
						bestParameters[1] = staticParameters;
						bestParameters[2] = staticParameters;
						System.out.println(" Parameters: " + i + " " + staticParameters + " " + staticParameters + " "
								+ testResult.getResponseTime() + " " + testResult.isResult());
						return bestParameters;
					} else {
						counter = counter - 1;
					}

				}

				if (prevWCRT > testResult.getResponseTime() && testResult.isResult()) {
					break;
				} else {
					prevWCRT = testResult.getResponseTime();
				}

			}
		}

		// System.out.println("Best initial solution: "+workingWCRT+" Budget:
		// "+bestParameters[0]+" Period: "+bestParameters[1]+" Deadline:
		// "+bestParameters[2]);

		return bestParameters;

	}

	public int[][] findIntialSolutionUpper(ArrayList<ArrayList<testFormat>> partitions) {

		int[][] testInitialParameters = new int[3][numberPollingServers];
		int counter = 0;

		for (int i = 0; i < numberPollingServers; i++) {
			for (int j = 0; j < 3; j++) {
				testInitialParameters[j][i] = 1;
			}
		}

		while (!checkParameterDemand(testInitialParameters, demands)) {
			for (int i = 0; i < numberPollingServers; i++) {
				int[] result = findInitialSolution(partitions.get(i), counter);
				testInitialParameters[0][i] = result[0];
				testInitialParameters[1][i] = result[1];
				testInitialParameters[2][i] = result[2];
			}
			counter++;
		}
		System.out.println("SUCCESS!");
		return testInitialParameters;

	}

	public int[] restartSimulatedAnnealing(int[] parameters, int[] initialParameters) {
		if (parameters[0] == 1 || parameters[1] == 1 || parameters[0] == 2 || parameters[0] == 12000
				|| parameters[0] == 12000 || parameters[0] == 12000) {
			// Pick random parameter to change
			Random rand = new Random();
			// int period = (int) ((Math.random() * (4000 - 500)) + 500);
			// int deadline = (int) ((Math.random() * (period - 500)) + 500);
			// int budget = (int) ((Math.random() * ((deadline / numberPollingServers) - 1))
			// + 1);

			int budget = initialParameters[0];
			int period = initialParameters[1];
			int deadline = initialParameters[2];

			int[] result = { budget, period, deadline };
			return result;

		} else {
			return parameters;
		}

	}

	public int[][] mutationAlgorithm(ArrayList<ArrayList<int[]>> allParameters, int[] acceptableParametersSizes,
			int[][] bestParameters, double bestWCRT, ArrayList<ArrayList<testFormat>> partitions,
			ArrayList<testFormat> eventTasks) {

		int[][] currentParameters = new int[3][numberPollingServers];
		double currentWCRT = bestWCRT;

		System.out.println("Mutation started");

		for (int i = 0; i < numberPollingServers; i++) {

			// Removed duplicates from collected parameters:
			for (int j = 0; j < allParameters.get(i).size(); j++) {
				int[] temp = allParameters.get(i).get(j);

				for (int k = j + 1; k < allParameters.get(i).size(); k++) {
					if (temp == allParameters.get(i).get(k)) {
						allParameters.get(i).remove(k);
					}
				}
			}
			/*
			for (int j = 0; j < allParameters.get(i).size(); j++) {
				System.out.print(Arrays.toString(allParameters.get(i).get(j)) + " \t");
			}
			System.out.println("");
			*/
		}

		for (int i = 0; i < 100; i++) {

			for (int j = 0; j < numberPollingServers; j++) {

				if (allParameters.get(j).size() == 1) {
					continue;
				}

				// Pick at random two parameters from the solution space from the specific
				// polling server
				Random rand = new Random();
				int firstPick = (int) (rand.nextInt(allParameters.get(j).size()));
				int secondPick = (int) (rand.nextInt(allParameters.get(j).size()));

				// If the same index is chosen, ensure that a different one is chosen
				while (firstPick == secondPick) {
					// System.out.println("Stuck: "+firstPick+" "+secondPick+"
					// "+allParameters.get(j).size());
					secondPick = (int) (rand.nextInt(allParameters.get(j).size()));
					;
				}

				// Create mutations and check the two new solutions
				// System.out.println("Size: " + allParameters.size() + " J: " + j + "
				// FirstPick: " + firstPick+ " SecondPick: " + secondPick);

				ArrayList<int[]> mutatedParameters = mutateParameters(allParameters.get(j).get(firstPick),
						allParameters.get(j).get(secondPick));

				int[][] tempParameters = new int[3][numberPollingServers];
				double tempWCRT = bestWCRT;

				for (int h = 0; h < 2; h++) {

					for (int k = 0; k < numberPollingServers; k++) {
						currentParameters[0][k] = bestParameters[0][k];
						currentParameters[1][k] = bestParameters[1][k];
						currentParameters[2][k] = bestParameters[2][k];
					}

					for (int k = 0; k < 3; k++) {
						currentParameters[0][j] = mutatedParameters.get(h)[0];
						currentParameters[1][j] = mutatedParameters.get(h)[1];
						currentParameters[2][j] = mutatedParameters.get(h)[2];
					}

					EDPAlgorithm runEDP = new EDPAlgorithm();
					EDPTuple EDPresult = runEDP.algorithm(currentParameters[0][j], currentParameters[1][j],
							currentParameters[2][j], partitions.get(j));
					checkUtilizationConstraint(currentParameters);
					if (checkParameterDemand(currentParameters, demands) && EDPresult.isResult()) {
						int[] WCRTs = optimalPollingServerRun(partitions, currentParameters);
						currentWCRT = finalEventWCRT(WCRTs, partitions, eventTasks);

						for (int k = 0; k < numberPollingServers; k++) {
							//System.out.print(currentParameters[0][k] + " " + currentParameters[1][k] + " "	+ currentParameters[2][k] + "\t \t");
						}
						// System.out.println("Current average WCRT: " + currentWCRT+" Best:
						// "+bestWCRT);

						if (currentWCRT < bestWCRT) {
							for (int k = 0; k < numberPollingServers; k++) {
								bestParameters[0][k] = currentParameters[0][k];
								bestParameters[1][k] = currentParameters[1][k];
								bestParameters[2][k] = currentParameters[2][k];
								System.out.print(" Best " + i + " " + j + ": " + bestParameters[0][k] + " "
										+ bestParameters[1][k] + " " + bestParameters[2][k] + " \t");
							}
							System.out.println("");
							bestWCRT = currentWCRT;

						}
					}

				}

			}

		}

		return bestParameters;

	}

	public ArrayList<int[]> mutateParameters(int[] firstPick, int[] secondPick) {

		Random rand = new Random();
		// int parameter = (int) ((Math.random() * (3 - 0)) + 0);
		int parameter = rand.nextInt(3);

		int[] firstResult = new int[3];
		int[] secondResult = new int[3];

		ArrayList<int[]> result = new ArrayList<int[]>();

		switch (parameter) {
		case 0:
			firstResult[0] = secondPick[0];
			firstResult[1] = firstPick[1];
			firstResult[2] = firstPick[2];
			secondResult[0] = firstPick[0];
			secondResult[1] = secondPick[1];
			secondResult[2] = secondPick[2];
			break;
		case 1:
			firstResult[0] = firstPick[0];
			firstResult[1] = secondPick[1];
			firstResult[2] = firstPick[2];
			secondResult[0] = secondPick[0];
			secondResult[1] = firstPick[1];
			secondResult[2] = secondPick[2];
			break;
		case 2:
			firstResult[0] = firstPick[0];
			firstResult[1] = firstPick[1];
			firstResult[2] = secondPick[2];
			secondResult[0] = secondPick[0];
			secondResult[1] = secondPick[1];
			secondResult[2] = firstPick[2];
			break;
		default:
			firstResult[0] = firstPick[0];
			firstResult[1] = firstPick[1];
			firstResult[2] = firstPick[2];
			secondResult[0] = secondPick[0];
			secondResult[1] = secondPick[1];
			secondResult[2] = secondPick[2];
			break;
		}

		result.add(firstResult);
		result.add(secondResult);
		return result;
	}

	public int[][] redo(int[][] parameters, ArrayList<ArrayList<testFormat>> partitions) {
		int highestParameter = -Integer.MAX_VALUE;
		int parameterType = 0;
		int pollingServer = 0;
		boolean[] noTouch = new boolean[numberPollingServers];
		boolean escapeFlag = true;

		EDPAlgorithm runEDP = new EDPAlgorithm();

		while (escapeFlag) {

			// Reduce the highest period/deadline with 1
			for (int i = 0; i < numberPollingServers; i++) {
				for (int j = 1; j < 3; j++) {
					if (parameters[j][i] > highestParameter && noTouch[i] != true) {
						highestParameter = parameters[j][i];
						parameterType = j;
						pollingServer = i;
					}
				}
			}

			// Peform the reduction
			parameters[parameterType][pollingServer] = parameters[parameterType][pollingServer] - 1;

			// Check if the new parameters are correct
			for (int i = 0; i < numberPollingServers; i++) {

				if (checkParameterDemand(parameters, demands)) {

					if (runEDP.algorithm(parameters[0][i], parameters[0][i], parameters[0][i], partitions.get(i))
							.isResult()) {
						continue;
					} else {
						// Reverse
						parameters[parameterType][pollingServer] = parameters[parameterType][pollingServer] + 1;
						noTouch[pollingServer] = true;

						break; // Try again
					}

				}
			}

		}

		return parameters;
	}

	// NOT IN USE

	public int[] generateNeighbourNewNew(int budget, int period, int deadline, double temp, int maxTemp) {

		// Pick random parameter to change
		Random rand = new Random();
		int choice = (int) rand.nextInt(3);

		// Normalize current temp from 0 to 100 based on maxTemp
		int randomJump = 0;
		int limit = (int) ((temp / maxTemp) * 100);
		if (limit == 0) {
			randomJump = (int) rand.nextInt(1);
		} else {
			randomJump = rand.nextInt(100);
		}

		// System.out.println("Temp: "+temp+" Jump: "+randomJump);
		// int randomJump = (int) rand.nextInt(100);

		// Pick to either increment or decrement
		int operation = (int) rand.nextInt(2);
		int res;
		int[] resArray = { budget, period, deadline };

		// System.out.println("Case: "+choice+" operation "+operation+" jump
		// "+randomJump);

		switch (choice) {
		case 0: // Change budget
			if (operation == 0 && (budget - randomJump) > 0) {
				res = budget - randomJump;
			} else if (operation == 1 && (budget + randomJump) < period && (budget + randomJump) < deadline
					&& (budget + randomJump) < 12000) {
				res = budget + randomJump;
			} else {
				res = budget;
			}
			// System.out.println("reduced");

			resArray[0] = res;
			return resArray;
		case 1: // Change period
			if (operation == 0 && (period - randomJump) > 0 && (period - randomJump) > budget
					&& (period - randomJump) > deadline) {
				res = period - randomJump;
			} else if (operation == 1 && (period + randomJump) < 12000) { // Hyperperiod
				res = period + randomJump;
			} else {
				res = period;
			}
			resArray[1] = res;
			return resArray;
		case 2: // Change deadline
			if (operation == 0 && (deadline - randomJump) > 0 && (deadline - randomJump) > budget) {
				res = deadline - randomJump;
			} else if (operation == 1 && (deadline + randomJump) < period && (deadline + randomJump) > budget
					&& (deadline + randomJump) < 12000) {
				res = deadline + randomJump;
			} else {
				res = deadline;
			}
			resArray[2] = res;
			return resArray;
		}
		return resArray;
	}

	public int[] localSearch(int[] initialSolution, ArrayList<testFormat> eventTasks, int prevWCRT) {

		EDPAlgorithm runEDP = new EDPAlgorithm();

		int[] result = { initialSolution[0], initialSolution[1], initialSolution[2], prevWCRT };

		while (true) {
			int[] neighbour = exploreNeighbourhood(initialSolution[0], initialSolution[1], initialSolution[2], prevWCRT,
					eventTasks);

			EDPTuple EDPresult = runEDP.algorithm(neighbour[0], neighbour[1], neighbour[2], eventTasks);

			// System.out.println("prevWCRT: " + prevWCRT);

			/*
			 * System.out.println("Best neighbour WCRT: " + EDPresult.getResponseTime() +
			 * " Budget: " + neighbour[0] + " Period: " + neighbour[1] + " Deadline: " +
			 * neighbour[2]);
			 */
			if (neighbour[3] < prevWCRT && EDPresult.isResult()) {
				initialSolution[0] = neighbour[0];
				initialSolution[1] = neighbour[1];
				initialSolution[2] = neighbour[2];
				prevWCRT = neighbour[3];
				int[] newResult = { initialSolution[0], initialSolution[1], initialSolution[2], prevWCRT };
				result = newResult;

				// System.out.println("New best");

			} else {
				return result;
			}
		}
	}

	public boolean checkParameterConstraints(int[] parameters) {

		int budget1000 = parameters[0] * (1000 / parameters[1]);

		if (parameters[0] < parameters[2] && parameters[2] < parameters[1] && parameters[0] < parameters[2]
				&& parameters[0] * numberPollingServers < parameters[2]) {
			return true;
		} else {
			return false;
		}

	}

	public int[] exploreNeighbourhood(int budget, int period, int deadline, int WCRT,
			ArrayList<testFormat> eventTasks) {

		int bestNewWCRT = WCRT;
		EDPAlgorithm runEDP = new EDPAlgorithm();
		int[] bestParameters = { budget, period, deadline };

		// System.out.println(" HEXA EXPLORE!");

		for (int i = 0; i < 3; i++) {

			for (int j = 0; j < 2; j++) {

				switch (i) {
				case 0:

					if (j == 0) {
						EDPTuple testWCRT = runEDP.algorithm(budget - 1, period, deadline, eventTasks);

						int[] newParameters = { budget - 1, period, deadline };

						// System.out.println("Explored: WCRT: " + testWCRT.getResponseTime() + "
						// Budget: " + (budget - 1)+ " Period: " + period + " Deadline: " + deadline+"
						// "+testWCRT.isResult()+" "+checkSoftParameterConstraints(newParameters));

						if (testWCRT.getResponseTime() < bestNewWCRT && testWCRT.isResult()
								&& checkSoftParameterConstraints(newParameters)) {
							bestNewWCRT = testWCRT.getResponseTime();
							bestParameters = newParameters;
						}
					} else {
						EDPTuple testWCRT = runEDP.algorithm(budget + 1, period, deadline, eventTasks);

						int[] newParameters = { budget + 1, period, deadline };

						// System.out.println("Explored: WCRT: " + testWCRT.getResponseTime() + "
						// Budget: " + (budget + 1)+ " Period: " + period + " Deadline: " + deadline+"
						// "+testWCRT.isResult()+" "+checkSoftParameterConstraints(newParameters));

						if (testWCRT.getResponseTime() < bestNewWCRT && testWCRT.isResult()
								&& checkSoftParameterConstraints(newParameters)) {
							bestNewWCRT = testWCRT.getResponseTime();
							bestParameters = newParameters;
						}
					}

					break;

				case 1:
					if (j == 0) {
						EDPTuple testWCRT = runEDP.algorithm(budget, period - 1, deadline, eventTasks);

						int[] newParameters = { budget, period - 1, deadline };

						// System.out.println("Explored: WCRT: " + testWCRT.getResponseTime() + "
						// Budget: " + (budget)+ " Period: " + (period - 1) + " Deadline: " + deadline+"
						// "+testWCRT.isResult()+" "+checkSoftParameterConstraints(newParameters));

						if (testWCRT.getResponseTime() < bestNewWCRT && testWCRT.isResult()
								&& checkSoftParameterConstraints(newParameters)) {
							bestNewWCRT = testWCRT.getResponseTime();
							bestParameters = newParameters;
						}
					} else {
						EDPTuple testWCRT = runEDP.algorithm(budget, period + 1, deadline, eventTasks);

						int[] newParameters = { budget, period + 1, deadline };

						// System.out.println("Explored: WCRT: " + testWCRT.getResponseTime() + "
						// Budget: " + (budget)+ " Period: " + (period + 1) + " Deadline: " + deadline+"
						// "+testWCRT.isResult()+" "+checkSoftParameterConstraints(newParameters));

						if (testWCRT.getResponseTime() < bestNewWCRT && testWCRT.isResult()
								&& checkSoftParameterConstraints(newParameters)) {
							bestNewWCRT = testWCRT.getResponseTime();
							bestParameters = newParameters;
						}
					}
					break;

				case 2:
					if (j == 0) {
						EDPTuple testWCRT = runEDP.algorithm(budget, period, deadline - 1, eventTasks);

						int[] newParameters = { budget, period, deadline - 1 };

						// System.out.println("Explored: WCRT: " + testWCRT.getResponseTime() + "
						// Budget: " + (budget)+ " Period: " + (period) + " Deadline: " + (deadline -
						// 1)+" "+testWCRT.isResult()+" "+checkSoftParameterConstraints(newParameters));

						if (testWCRT.getResponseTime() < bestNewWCRT && testWCRT.isResult()
								&& checkSoftParameterConstraints(newParameters)) {
							bestNewWCRT = testWCRT.getResponseTime();
							bestParameters = newParameters;
						}
					} else {
						EDPTuple testWCRT = runEDP.algorithm(budget, period, deadline + 1, eventTasks);

						int[] newParameters = { budget, period, deadline + 1 };

						// System.out.println("Explored: WCRT: " + testWCRT.getResponseTime() + "
						// Budget: " + (budget)+ " Period: " + (period) + " Deadline: " + (deadline +
						// 1)+" "+testWCRT.isResult()+" "+checkSoftParameterConstraints(newParameters));

						if (testWCRT.getResponseTime() < bestNewWCRT && testWCRT.isResult()
								&& checkSoftParameterConstraints(newParameters)) {
							bestNewWCRT = testWCRT.getResponseTime();
							bestParameters = newParameters;
						}
					}
					break;
				default:
				}

			}

		}

		int[] result = { bestParameters[0], bestParameters[1], bestParameters[2], bestNewWCRT };

		return result;

	}

	// Main function to get the final, average WCRT of the first EDF run and all the
	// EDP-runs using the polling servers
	public int finalWCRT(int EDFWCRT, int EDPWCRT, int timeTasksSize, int eventTasksSize) {

		double timeTasksWeight = (double) timeTasksSize / (timeTasksSize + eventTasksSize);
		double eventTasksWeight = (double) eventTasksSize / (timeTasksSize + eventTasksSize);

		// Placeholder to print out acceptableParameters: TODO remove when not needed
		for (int i = 0; i < numberPollingServers; i++) {
			System.out.println(
					" Polling server " + i + " has gotten " + acceptableParameters.get(i).size() + " solutions");
		}

		return (int) Math.ceil(timeTasksWeight * EDFWCRT + eventTasksWeight * EDPWCRT);
	}

	public double calculateUtilization(ArrayList<testFormat> timeTasks) {

		int n = timeTasks.size();

		double result = 0;

		for (int i = 0; i < n; i++) {
			// System.out.println(timeTasks.get(i).getName()+"
			// "+timeTasks.get(i).getPeriod());
			result = result + (double) timeTasks.get(i).getDuration() / timeTasks.get(i).getPeriod();
		}

		return result;
	}

	public boolean checkUtilizationConstraint(int[][] parameters) {

		int n = parameters[0].length;

		double result = 0;

		for (int i = 0; i < n; i++) {
			result = parameters[0][i] / parameters[1][i];
		}

		// System.out.println("Utilization: "+(result + utilizationTimeTasks));

		if (result + utilizationTimeTasks < 1) {
			// System.out.println("Utilization: True ");
			return true;
		} else {
			// System.out.println("Utilization: False ");
			return false;
		}

	}

	public int[][] getInitialParameters() {

		return initialParameters;
	}

	// Constraint to ensure that EDP-parameters also lets the EDF-algorithm run the
	// polling tasks without exceeded deadlines
	/*
	 * public boolean checkParameterConstraint(int[] parameters, int[][]
	 * prevOptimalParameters, int index) {
	 * 
	 * int totalNeededBudget = 0; int totalProfit = 0;
	 * 
	 * if(index>0){ for(int i=0; i<index; i++) { totalNeededBudget =
	 * totalNeededBudget +
	 * (12000/prevOptimalParameters[1][i])*prevOptimalParameters[0][i];
	 * 
	 * 
	 * totalProfit =
	 * totalProfit+(1000-(1000/prevOptimalParameters[2][i])*prevOptimalParameters[0]
	 * [i]); } }
	 * 
	 * int thisProfit = (1000-(1000/parameters[2])*parameters[0]); if(index>0) {
	 * 
	 * //System.out.println("Total needed time: "+(12000-totalMaxTimeDuration)
	 * +" total budget: "+(totalNeededBudget+(12000/parameters[1])*parameters[0])
	 * +"Total profit per 1000: "+(thisProfit+totalProfit)+" Needed time per 1000: "
	 * +maxTimeDuration); }
	 * //if(parameters[0]*numberPollingServers+maxTimeDuration/2<parameters[2] &&
	 * parameters[0]*numberPollingServers+maxTimeDuration/2<parameters[1]) {
	 * 
	 * if((thisProfit+totalProfit>maxTimeDuration) && (parameters[0]<=parameters[1])
	 * && (parameters[1]<=parameters[2]) &&
	 * (parameters[0]+(numberPollingServers-1)+maxTimeDuration<=parameters[2]) &&
	 * ((parameters[0]+(numberPollingServers-1)+maxTimeDuration<=parameters[1]))) {
	 * return true; } else { return false; }
	 * 
	 * }
	 */

	/*
	 * // UNUSED
	 * 
	 * 
	 * 
	 * // Main function for finding optimal partitions for polling servers (Uses
	 * Simulated Annealing): public ArrayList<ArrayList<testFormat>>
	 * findOptimalPartitions( ArrayList<ArrayList<testFormat>> eventTasks, int
	 * Tstart, double alpha) throws Exception {
	 * 
	 * double t = Tstart; int delta = 0;
	 * 
	 * // Test initial solution, check how good the runtimes of each polling server
	 * is EDPAlgorithm runEDP = new EDPAlgorithm(); boolean resultBoolean = false;
	 * int bestAvgWCRT = 0; boolean correctSolutionMissing=true; //int
	 * currentAvgWCRT = 0; //int[] initialParameters = generateInitialParameters(n,
	 * maxTimeDuration);
	 * 
	 * for (int i = 0; i < numberPollingServers; i++) {
	 * 
	 * 
	 * allParameters[i] = findInitialSolution(eventTasks.get(i));
	 * 
	 * /*
	 * 
	 * EDPTuple resultInitial = runEDP.algorithm(initialParameters[0],
	 * initialParameters[1], initialParameters[2], eventTasks.get(i)); if
	 * (!resultInitial.isResult()) { resultBoolean = false; break; } else {
	 * System.out.println("Solution works"); correctSolutionMissing = false;
	 * resultBoolean = true; bestAvgWCRT = bestAvgWCRT +
	 * resultInitial.getResponseTime(); }
	 * 
	 * 
	 * }
	 * 
	 * if(checkParameterDemand(allParameters, demands)) {
	 * System.out.println("Works"); } else { System.out.println("nope"); }
	 * 
	 * // Now we know the total response time of our initial solution & if it works
	 * 
	 * // System.out.println("Initial WCRT: "+solutionResponseTime);
	 * 
	 * //currentAvgWCRT = bestAvgWCRT; //ArrayList<ArrayList<testFormat>>
	 * currentPartition = eventTasks; ArrayList<ArrayList<testFormat>> bestPartition
	 * = eventTasks;
	 * 
	 * while (t > 0.01) {
	 * 
	 * ArrayList<ArrayList<testFormat>> neighbourPartition = new
	 * ArrayList<ArrayList<testFormat>>(); if (numberPollingServers > 2) {
	 * neighbourPartition = swapN(eventTasks); } else { neighbourPartition =
	 * swap2(eventTasks); }
	 * 
	 * int newAvgWCRT = 0;
	 * 
	 * resultBoolean = false;
	 * 
	 * // Test newly generated solution; for (int i = 0; i < numberPollingServers;
	 * i++) { EDPTuple resultInitial = runEDP.algorithm(initialParameters[0],
	 * initialParameters[1], initialParameters[2], eventTasks.get(i));
	 * 
	 * //System.out.println("Result: "+resultInitial.isResult());
	 * 
	 * if (!resultInitial.isResult()) { resultBoolean = false;
	 * //System.out.print("\t " + resultInitial.isResult() + " \t"); break; } else {
	 * resultBoolean = true; newAvgWCRT = newAvgWCRT +
	 * resultInitial.getResponseTime(); } } //newAvgWCRT = newAvgWCRT;
	 * 
	 * // Print out the new partition
	 * 
	 * delta = bestAvgWCRT - newAvgWCRT; // If delta is positive, the Neighbour is a
	 * better // solution
	 * 
	 * if (delta > 0 || probabilityFunc(delta, t)) { //currentPartition =
	 * neighbourPartition; //currentAvgWCRT = newAvgWCRT;
	 * 
	 * //System.out.print("\t Current Total Response Time: " + currentAvgWCRT +
	 * "\t");
	 * 
	 * if (resultBoolean && newAvgWCRT > 0 && newAvgWCRT < bestAvgWCRT &&
	 * checkSeparationConstraint(neighbourPartition)) { correctSolutionMissing =
	 * false; bestPartition = neighbourPartition; bestAvgWCRT = newAvgWCRT;
	 * System.out.println("Best, correct partitions:");
	 * printOutPartitions(bestPartition); // System.out.print("Current best, correct
	 * Total Response time: // "+bestTotalResponseTime+" with partition: ?"); }
	 * 
	 * } else { //System.out.print("\t\t\t\t"); } t = t * alpha; // Change of
	 * temperature for each round
	 * 
	 * //System.out.print("\t Temperature: " + t + " \t");
	 * //printOutPartitions(currentPartition); //System.out.println(""); }
	 * 
	 * //System.out.println("Best avg response time after parition optimization: " +
	 * bestAvgWCRT);
	 * 
	 * 
	 * if(correctSolutionMissing) { throw new
	 * Exception("No set of partitions with all partitions satisfying the EDP-algorithm was found"
	 * ); }
	 * 
	 * 
	 * return bestPartition;
	 * 
	 * }
	 * 
	 * 
	 * 
	 * 
	 * // Helper functions // Swap event tasks between two partitions public
	 * ArrayList<ArrayList<testFormat>> swap2(ArrayList<ArrayList<testFormat>>
	 * eventTasks) {
	 * 
	 * // Extract the tasks of the two polling servers: ArrayList<testFormat>
	 * pollTasks1 = eventTasks.get(0); ArrayList<testFormat> pollTasks2 =
	 * eventTasks.get(1);
	 * 
	 * // Generate random indices to swap: Random rand = new Random(); int index1 =
	 * (int) rand.nextInt(pollTasks1.size()); int index2 = (int)
	 * rand.nextInt(pollTasks2.size());
	 * 
	 * // Swaps the elements testFormat temp = pollTasks1.get(index1);
	 * pollTasks1.set(index1, pollTasks2.get(index2)); pollTasks2.set(index2, temp);
	 * 
	 * // Packs up the arraylist ArrayList<ArrayList<testFormat>> result = new
	 * ArrayList<ArrayList<testFormat>>(); result.add(pollTasks1);
	 * result.add(pollTasks2);
	 * 
	 * return result; }
	 * 
	 * // Swap event tasks between N partitions public
	 * ArrayList<ArrayList<testFormat>> swapN(ArrayList<ArrayList<testFormat>>
	 * eventTasks) {
	 * 
	 * // Generate random order of swapping // Generate the list arraylist of
	 * integers from 1 to n ArrayList<Integer> orderList = new ArrayList<Integer>();
	 * for (int i = 0; i < numberPollingServers; i++) { orderList.add(i); }
	 * 
	 * Collections.shuffle(orderList); // Shuffles the list
	 * 
	 * // Convert order of swapping to pairs ArrayList<ArrayList<Integer>> swapPairs
	 * = extractPairsFromList(orderList);
	 * 
	 * // System.out.println("Number of pairs: "+swapPairs.size());
	 * 
	 * // ArrayList<ArrayList<testFormat>> result = eventTasks;
	 * 
	 * // printOutPartitions(result); // System.out.println("\n");
	 * 
	 * for (int i = 0; i < numberPollingServers; i++) {
	 * 
	 * // Extract number for first pair ArrayList<Integer> pair = swapPairs.get(i);
	 * 
	 * ArrayList<ArrayList<testFormat>> inputSwap = new
	 * ArrayList<ArrayList<testFormat>>(); inputSwap.add(result.get(pair.get(0)));
	 * inputSwap.add(result.get(pair.get(1)));
	 * 
	 * // System.out.println("Before: Pair:"+pair.get(0)+" "+pair.get(1)); //
	 * printOutPartitions(result); // System.out.println("\n");
	 * 
	 * // Perform swapping and put result back into result-arraylist
	 * ArrayList<ArrayList<testFormat>> swappedResult = swap2(inputSwap);
	 * result.set(pair.get(0), swappedResult.get(0)); result.set(pair.get(1),
	 * swappedResult.get(1));
	 * 
	 * // System.out.println("After: "); // printOutPartitions(result); //
	 * System.out.println("\n");
	 * 
	 * }
	 * 
	 * // printOutPartitions(result); // System.out.println("\n");
	 * 
	 * return result; }
	 * 
	 * // Given a list of partitions in an order, extract pairs to know which
	 * partitions should swap event tasks with each other public
	 * ArrayList<ArrayList<Integer>> extractPairsFromList(ArrayList<Integer> order)
	 * {
	 * 
	 * // Result ArrayList<ArrayList<Integer>> result = new
	 * ArrayList<ArrayList<Integer>>();
	 * 
	 * for (int i = 0; i < numberPollingServers - 1; i++) { ArrayList<Integer> temp
	 * = new ArrayList<Integer>(); temp.add(order.get(i)); temp.add(order.get(i +
	 * 1)); result.add(temp); }
	 * 
	 * // Last pair between first and last partition in order: ArrayList<Integer>
	 * temp = new ArrayList<Integer>(); temp.add(order.get(numberPollingServers -
	 * 1)); temp.add(order.get(0)); result.add(temp);
	 * 
	 * return result;
	 * 
	 * }
	 * 
	 * // Helper functions // Get minimum supply needed to successfully complete all
	 * event tasks in partition public int getMinSupply(ArrayList<testFormat>
	 * eventTasks) {
	 * 
	 * int result = 0;
	 * 
	 * for (int i = 0; i < eventTasks.size(); i++) { result = result +
	 * eventTasks.get(i).getDuration(); }
	 * 
	 * return result; }
	 * 
	 * // Simulated Annealing to find optimal parameters: public int[]
	 * simulatedAnnealing(int[] initialSolution, int Tstart, double alpha,
	 * ArrayList<testFormat> eventTasks, int min, int[][] prevOptimalParameters, int
	 * index) throws Exception {
	 * 
	 * Instant startTime = Instant.now();
	 * 
	 * double t = Tstart; int delta = 0; boolean correctSolutionMissing = true;
	 * EDPAlgorithm runEDP = new EDPAlgorithm();
	 * 
	 * // Test WCRT of initial solution: EDPTuple resultInitial =
	 * runEDP.algorithm(initialSolution[0], initialSolution[1], initialSolution[2],
	 * eventTasks);
	 * 
	 * int[] bestCorrectSolution = initialSolution; int bestCorrectResponseTime =
	 * resultInitial.getResponseTime();
	 * 
	 * if(resultInitial.isResult()) { correctSolutionMissing = false; }
	 * 
	 * 
	 * System.out.println("Initial result: "+resultInitial.isResult() +
	 * " Constraint: "+checkParameterConstraint(initialSolution,
	 * prevOptimalParameters, index));
	 * 
	 * while (t > 0.01) { int[] neighbour = generateNeighbourNew(initialSolution[0],
	 * initialSolution[1], initialSolution[2], min);
	 * 
	 * // Test WCRT of neighbour solution: EDPTuple resultNeighbour =
	 * runEDP.algorithm(neighbour[0], neighbour[1], neighbour[2], eventTasks);
	 * 
	 * if (resultNeighbour.getResponseTime() == 0) { resultNeighbour =
	 * runEDP.algorithm(neighbour[0], neighbour[1], neighbour[2], eventTasks); }
	 * 
	 * //
	 * System.out.println("Neighbour parameters: "+neighbour[0]+" "+neighbour[1]+"
	 * // "+neighbour[2]);
	 * 
	 * delta = resultInitial.getResponseTime() - resultNeighbour.getResponseTime();
	 * // If delta is positive, the // Neighbour is a better // solutio
	 * 
	 * //System.out.println("Budget: "+neighbour[0]+" Period "+neighbour[1]
	 * +" Deadline: "+neighbour[2]);
	 * 
	 * if ((delta > 0 && resultNeighbour.isResult() ) || probabilityFunc(delta, t))
	 * { initialSolution = neighbour; resultInitial = resultNeighbour;
	 * 
	 * 
	 * //System.out.println("Result: "+resultNeighbour.isResult());
	 * //System.out.println("Budget: "+neighbour[0]+" Period "+neighbour[1]
	 * +" Deadline: "+neighbour[2]);
	 * //System.out.println("Special constraint: "+checkParameterConstraint(
	 * neighbour));
	 * 
	 * if (resultNeighbour.isResult() && resultNeighbour.getResponseTime() > 0 &&
	 * resultNeighbour.getResponseTime() < bestCorrectResponseTime) {
	 * correctSolutionMissing = false; bestCorrectSolution = neighbour;
	 * bestCorrectResponseTime = resultNeighbour.getResponseTime();
	 * System.out.println("Current best, correct Response time: " +
	 * bestCorrectResponseTime + " with parameters: " + neighbour[0] + " " +
	 * neighbour[1] + " " + neighbour[2]); }
	 * 
	 * } t = t * alpha; // Change of temperature for each round
	 * 
	 * //System.out.println("Temperature: "+t); }
	 * 
	 * // System.out.println("Best WCRT with Simulated Annealing: //
	 * "+bestCorrectResponseTime);
	 * 
	 * Instant endTime = Instant.now();
	 * 
	 * // System.out.println("Simulated Annealing duration: //
	 * "+Duration.between(startTime,endTime).toMinutes()+" minutes");
	 * 
	 * if(correctSolutionMissing) { throw new
	 * Exception("No set of parameters was found that satisfy the EDP-algorrithm");
	 * }
	 * 
	 * 
	 * return bestCorrectSolution;
	 * 
	 * }
	 * 
	 * // Function to generate new solution from neighborhood public int[]
	 * generateNeighbourNew(int budget, int period, int deadline, int min) {
	 * 
	 * // Pick random parameter to change Random rand = new Random(); int choice =
	 * (int) rand.nextInt(3);
	 * 
	 * // Pick to either increment or decrement int operation = (int)
	 * rand.nextInt(2); int res; int[] resArray = { budget, period, deadline };
	 * switch (choice) { case 0: // Change budget if (operation == 0 && budget >
	 * min) { res = budget - 1; } else if (operation == 1 && budget < period &&
	 * budget < deadline) { res = budget + 1; } else { res = budget; }
	 * //System.out.println("reduced");
	 * 
	 * resArray[0] = res; return resArray; case 1: // Change period if (operation ==
	 * 0 && period != min && budget < period) { res = period - 1; } else if
	 * (operation == 1 && period < 12000 ) { // Hyperperiod res = period + 1; } else
	 * { res = period; } resArray[1] = res; return resArray; case 2: // Change
	 * deadline if (operation == 0 && deadline != min && budget < deadline - 1) {
	 * res = deadline - 1; } else if (operation == 1) { res = deadline + 1; } else {
	 * res = deadline; } resArray[2] = res; return resArray; } return resArray; }
	 * 
	 * // Generates initial parameters for the EDP-algorithm, based on the minimum
	 * supply for the partition, // total number of polling servers and max time
	 * needed to run all time tasks each 1000 ticks public int[]
	 * generateInitialParameters(int min, int numberPollingServers, int
	 * maxTimeDuration) {
	 * 
	 * 
	 * 
	 * 
	 * //int[] result = {(1000-maxTimeDuration)/numberPollingServers, 1000, 1000 };
	 * 
	 * if(min==0) { int[] result = {(1000-maxTimeDuration)/numberPollingServers,
	 * 1000, 1000 }; //System.out.println("Initial generated budget: "+result[0]);
	 * return result; } else { //int[] result = {(int) Math.ceil(min*1.2),
	 * (min*numberPollingServers+maxTimeDuration),
	 * (min*numberPollingServers+maxTimeDuration)};
	 * //System.out.println("Initial generated budget: "+result[0]); int budget =
	 * (12000-totalMaxTimeDuration)/numberPollingServers; //int[] result = {(int)
	 * (budget*0.5), (int) (budget*0.75), (int) (budget*0.75)}; //int[] result =
	 * {(1000-maxTimeDuration)/numberPollingServers, 1000, 1000 }; int[] result =
	 * {50, 300, 300}; //int[] result = {1000, 1000, 1000}; //int[] result =
	 * {budget, budget, budget};
	 * //System.out.println("Initial generated parameters: Budget: "+result[0]
	 * +" Period: "+result[1]+" Deadline: "+result[2]); return result; } }
	 * 
	 */
}
