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
	private ArrayList<ArrayList<int[]>> globalTrueParameters;
	ArrayList<ArrayList<int[]>> acceptableParameters = new ArrayList<ArrayList<int[]>>();
	private double utilizationTimeTasks;

	public OptimizationAlgorithm(int numberPollingServers, int[] demands, ArrayList<testFormat> timeTasks) {
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

			int currentWCRT = testPartitions(currentPartitions);

			if (currentWCRT < bestTotalWCRT) {
				bestTotalWCRT = currentWCRT;
				bestPartitions = currentPartitions;
			}

		}

		printOutPartitions(bestPartitions);
		System.out.print("\n");

		if (!checkSeparationConstraint(bestPartitions)) {
			throw new Exception("Paritions did not satisfy separation constraint");
		}

		return bestPartitions;

	}

	// Helper functions:
	// Returns partitions, based on the given event tasks and the number of polling
	// servers requested
	public ArrayList<ArrayList<testFormat>> getPartitionsPollingServers(ArrayList<testFormat> eventTasks,
			int numPolling) throws Exception {

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

		int[][] initialGeneratedParameters = findIntialSolutionUpper(partitions);

		for (int i = 0; i < numberPollingServers; i++) {
			for (int j = 0; j < 3; j++) {
				allParameters[j][i] = initialGeneratedParameters[j][i];
				initialParameters[j][i] = initialGeneratedParameters[j][i];
			}

		}
		checkUtilizationConstraint(allParameters);
		if (checkParameterDemand(allParameters, demands)) {
			System.out.println("Initial solution should work");
		}

		// Output
		int[][] result = new int[3][numberPollingServers];

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

		int pollingBudget1000 = 0;

		// Figure out the 1000-tick demand by the total budget of the polling tasks
		for (int i = 0; i < parameters[0].length; i++) {
			double multiple = 1000 / parameters[1][i];
			int budget1000 = (int) Math.ceil(parameters[0][i] * multiple);
			pollingBudget1000 = pollingBudget1000 + budget1000;
		}

		// Check if adding polling demand exceeds demand in any run of the EDF-algorithm
		for (int i = 1; i < 13; i++) {

			if (demands[i - 1] + pollingBudget1000 * i > i * 1000) {
				return false;
			}
		}
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
			if (Duration.between(startTime, endTime).toSeconds() > 1) {
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

		//
		// System.out.println("Acceptable Parameters at finetuning: ");
		for (int i = 0; i < numberPollingServers; i++) {
			for (int j = 0; j < acceptableParameters.get(i).size(); j++) {
				// System.out.print(acceptableParameters.get(i).get(j)[0]+"
				// "+acceptableParameters.get(i).get(j)[1]+"
				// "+acceptableParameters.get(i).get(j)[2]+"\t");
			}
			// System.out.println("");
		}

		for (int i = 0; i < numberPollingServers; i++) {

			// Removed duplicates from collected parameters:

			// acceptableParameters.get(i).remove(0);

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
			 * 
			 * for (int j = 0; j < acceptableParameters.get(i).size(); j++) {
			 * System.out.print(Arrays.toString(acceptableParameters.get(i).get(j)) +
			 * " \t"); } System.out.println("");
			 */
			int trueParameterArray[] = new int[3];
			// Initialize with best parameters for all polling servers:
			for (int j = 0; j < 3; j++) {
				currentParameters[j][i] = acceptableParameters.get(i).get(acceptableParametersSizes[i] - 1)[j];

				trueParameterArray[j] = currentParameters[j][i];
			}
			trueParameters.get(i).add(trueParameterArray);

		}

		
		  
		  for (int i = 0; i < numberPollingServers; i++) { 
			  bestParameters[0][i] = currentParameters[0][i]; 
			  bestParameters[1][i] = currentParameters[1][i];
			  bestParameters[2][i] = currentParameters[2][i];
		  
		  }
		 

		for (int i = 0; i < numberPollingServers; i++) {
			//System.out.println(" Best found parameters for polling server "+i+": "+bestParameters[0][i]+" "+bestParameters[1][i]+" "+bestParameters[2][i]);
			Collections.reverse(acceptableParameters.get(i));
		}

		/*
		 * double bestWCRT = Integer.MAX_VALUE;
		 * checkUtilizationConstraint(currentParameters);
		 * 
		 * if (checkParameterDemand(currentParameters, demands) &&
		 * checkUtilizationConstraint(currentParameters)) { int[] WCRTs =
		 * optimalPollingServerRun(partitions, currentParameters); bestWCRT =
		 * finalEventWCRT(WCRTs, partitions, eventTasks); //
		 * System.out.println("First check ok:"+bestWCRT); } else { bestWCRT =
		 * Integer.MAX_VALUE; }
		 * 
		 */
		double bestWCRT = Integer.MAX_VALUE;
		for (int i = 1; i < max; i++) {

			for (int j = 0; j < numberPollingServers; j++) {

				// Change one set of parameters for j'th polling server

				if (acceptableParametersSizes[j] > i) {
					for (int k = 0; k < 3; k++) {
						currentParameters[k][j] = acceptableParameters.get(j).get(i)[k];
						//System.out.print("Yes: " + acceptableParameters.get(j).get(i)[k]+"\t");
					}
					//System.out.println("");
				} else {
					continue;
				}

				for (int h = 0; h < numberPollingServers; h++) {
					// System.out.print(" Testing " + i + " " + j + ": " + currentParameters[0][h]
					// +" "+ currentParameters[1][h] + " " + currentParameters[2][h]);
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

					// System.out.println("Added to polling server "+j+" : ");
					// System.out.println(trueParameterArray[0]+" "+trueParameterArray[1]+"
					// "+trueParameterArray[2]);

					if (currentWCRT < bestWCRT) {
						for (int h = 0; h < numberPollingServers; h++) {
							bestParameters[0][h] = currentParameters[0][h];
							bestParameters[1][h] = currentParameters[1][h];
							bestParameters[2][h] = currentParameters[2][h];
							//System.out.print(" Best " + i + " " + j + ": " + bestParameters[0][h] + " "+bestParameters[1][h] + " " + bestParameters[2][h]+" "+(checkParameterDemand(currentParameters, demands))+" \t");
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
		// True parameters [

		/*
		 * 
		 * System.out.println("True parameters sent to mutation: "); for(int i=0;
		 * i<numberPollingServers; i++) {
		 * 
		 * for(int j = 0; j<trueParameters.get(i).size(); j++) {
		 * System.out.println("True parameters for polling server "+i+" :"
		 * +trueParameters.get(i).get(j)[0]+" "+trueParameters.get(i).get(j)[1]+" "
		 * +trueParameters.get(i).get(j)[2]+"\t"); } }
		 * 
		 */

		int[][] mutatedParameters = mutationAlgorithm(trueParameters, acceptableParametersSizes, bestParameters,
				bestWCRT, partitions, eventTasks);
		setGlobalTrueParameters(trueParameters);
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
				// System.out.print(parameters[0][j] + " " + parameters[1][j] + " " +
				// parameters[2][j] + " \t");
			}
			// System.out.println();

			Random rand = new Random();
			int pollingServer = (int) (rand.nextInt(parameters[0].length));
			int choice = (int) (rand.nextInt(3));
			int operation = (int) (rand.nextInt(2));
			EDPAlgorithm runEDP = new EDPAlgorithm();

			if (operation == 0) {
				parameters[choice][pollingServer] = parameters[choice][pollingServer] - 1;
				int[] temp = { parameters[0][pollingServer], parameters[1][pollingServer],
						parameters[2][pollingServer] };
				if (!checkParameterConstraints(temp)
						|| !runEDP.algorithm(temp[0], temp[1], temp[2], partitions.get(pollingServer)).isResult()) {
					parameters[choice][pollingServer] = parameters[choice][pollingServer] + 1;
					continue;
				}
			} else {
				parameters[choice][pollingServer] = parameters[choice][pollingServer] + 1;
				int[] temp = { parameters[0][pollingServer], parameters[1][pollingServer],
						parameters[2][pollingServer] };
				if (!checkParameterConstraints(temp)
						|| !runEDP.algorithm(temp[0], temp[1], temp[2], partitions.get(pollingServer)).isResult()) {
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
						// System.out.println(" Parameters: " + i + " " + staticParameters + " " +
						// staticParameters + " "+ testResult.getResponseTime() + " " +
						// testResult.isResult());
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
		// System.out.println("SUCCESS!");

		for (int i = 0; i < numberPollingServers; i++) {
			System.out.println("Initial parameters for polling server " + i + " : " + testInitialParameters[0][i] + " "
					+ testInitialParameters[1][i] + " " + testInitialParameters[2][i]);
		}

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
			/*
			 * 
			 * for (int j = 0; j < allParameters.get(i).size(); j++) { int[] temp =
			 * allParameters.get(i).get(j);
			 * 
			 * for (int k = j + 1; k < allParameters.get(i).size(); k++) { if (temp ==
			 * allParameters.get(i).get(k)) { allParameters.get(i).remove(k); } } }
			 */
			for (int j = 0; j < allParameters.get(i).size(); j++) {
				System.out.print(Arrays.toString(allParameters.get(i).get(j)) + " \t");
			}
			System.out.println("");
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
							// System.out.print(currentParameters[0][k] + " " + currentParameters[1][k] + "
							// " + currentParameters[2][k] + "\t \t");
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
		int lowestParameter = Integer.MAX_VALUE;
		int parameterType = 0;
		int pollingServer = 0;
		boolean[] noTouch = new boolean[numberPollingServers];
		boolean escapeFlag = true;

		EDPAlgorithm runEDP = new EDPAlgorithm();

		while (escapeFlag) {

			// Reduce the highest period/deadline with 1
			for (int i = 0; i < numberPollingServers; i++) {
				for (int j = 1; j < 3; j++) {
					if (parameters[j][i] < lowestParameter && noTouch[i] != true) {
						lowestParameter = parameters[j][i];
						parameterType = j;
						pollingServer = i;
					}
				}
			}

			// Peform the reduction
			parameters[parameterType][pollingServer] = parameters[parameterType][pollingServer] + 1;

			// Check if the new parameters are correct
			for (int i = 0; i < numberPollingServers; i++) {

				if (checkParameterDemand(parameters, demands)) {

					if (runEDP.algorithm(parameters[0][i], parameters[0][i], parameters[0][i], partitions.get(i))
							.isResult()) {
						return parameters;
					} else {
						// Reverse
						parameters[parameterType][pollingServer] = parameters[parameterType][pollingServer] - 1;
						noTouch[pollingServer] = true;

						break; // Try again
					}

				}
			}

		}

		return parameters;
	}

	public int[][] redo2(int[][] parameters, ArrayList<ArrayList<testFormat>> partitions,
			ArrayList<testFormat> eventTasks, ArrayList<testFormat> timeTasks, double initialUtilization) {

		double currentUtilization = initialUtilization;
		double prevUtilization = initialUtilization;
		int[][] currentParameters = parameters;

		Random rand = new Random();
		int pollingServer = (int) (rand.nextInt(parameters[0].length));
		int choice = (int) (rand.nextInt(3));
		int operation = (int) (rand.nextInt(2));
		EDPAlgorithm runEDP = new EDPAlgorithm();

		while (true) {

			if (operation == 0) {
				parameters[choice][pollingServer] = parameters[choice][pollingServer] - 1;
				int[] temp = { parameters[0][pollingServer], parameters[1][pollingServer],
						parameters[2][pollingServer] };
				if (!checkParameterConstraints(temp)
						|| !runEDP.algorithm(temp[0], temp[1], temp[2], partitions.get(pollingServer)).isResult()) {
					parameters[choice][pollingServer] = parameters[choice][pollingServer] + 1;
					continue;
				}
			} else {
				parameters[choice][pollingServer] = parameters[choice][pollingServer] + 1;
				int[] temp = { parameters[0][pollingServer], parameters[1][pollingServer],
						parameters[2][pollingServer] };
				if (!checkParameterConstraints(temp)
						|| !runEDP.algorithm(temp[0], temp[1], temp[2], partitions.get(pollingServer)).isResult()) {
					parameters[choice][pollingServer] = parameters[choice][pollingServer] - 1;
					continue;
				}
			}

			ArrayList<testFormat> finalTimeTasks = createPollingServerTasks(timeTasks, parameters);

			System.out.println("Utilization: " + calculateUtilization(finalTimeTasks));

			if (checkParameterDemand(parameters, demands)
					&& calculateUtilization(finalTimeTasks) < currentUtilization) {
				return parameters;

			}

		}
	}

	public boolean checkParameterConstraints(int[] parameters) {

		if (parameters[0] < parameters[2] && parameters[2] < parameters[1] && parameters[0] < parameters[2]
				&& parameters[0] * (numberPollingServers) < parameters[2]) {
			return true;
		} else {
			return false;
		}

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

	public ArrayList<ArrayList<int[]>> getGlobalTrueParameters() {
		return globalTrueParameters;
	}

	public void setGlobalTrueParameters(ArrayList<ArrayList<int[]>> globalTrueParameters) {
		this.globalTrueParameters = globalTrueParameters;
	}

	
}
