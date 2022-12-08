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

	private int[][] initialParameters;
	private int numberPollingServers;
	private int[] demands;
	private int[][] allParameters;
	ArrayList<ArrayList<int[]>> foundParameters = new ArrayList<ArrayList<int[]>>();
	private double utilizationTimeTasks;

	// Creates an instance of the optimization class and sets some of the global parameters
	public OptimizationAlgorithm(int numberPollingServers, int[] demands, ArrayList<testFormat> timeTasks) {
		this.numberPollingServers = numberPollingServers;
		this.demands = demands;
		this.utilizationTimeTasks = calculateUtilization(timeTasks);
	}

	// Sets the number of polling servers, when that number is known
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

			// Gets the min number of polling servers needed, based on separation constraint
			public int getSeperationNum(ArrayList<testFormat> eventTasks) {
				ArrayList<Integer> result = new ArrayList<Integer>();
				for (int i = 0; i < eventTasks.size(); i++) {
					if (!result.contains(eventTasks.get(i).getSeparation())) {
						result.add(eventTasks.get(i).getSeparation());
					}
				}
				return result.size();
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

		if (checkDemandConstraint(allParameters, demands)) {
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

		// Outer function for generating initial solutions for the polling servers
		public int[][] findIntialSolutionUpper(ArrayList<ArrayList<testFormat>> partitions) {
	
			int[][] testInitialParameters = new int[3][numberPollingServers];
			int counter = 0;
	
			for (int i = 0; i < numberPollingServers; i++) {
				for (int j = 0; j < 3; j++) {
					testInitialParameters[j][i] = 1;
				}
			}
	
			while (!checkDemandConstraint(testInitialParameters, demands)) {
				for (int i = 0; i < numberPollingServers; i++) {
					int[] result = findInitialSolution(partitions.get(i), counter);
					testInitialParameters[0][i] = result[0];
					testInitialParameters[1][i] = result[1];
					testInitialParameters[2][i] = result[2];
				}
				counter++;
			}
	
			for (int i = 0; i < numberPollingServers; i++) {
				System.out.println("Initial parameters for polling server " + i + " : " + testInitialParameters[0][i] + " "
						+ testInitialParameters[1][i] + " " + testInitialParameters[2][i]);
			}
	
			return testInitialParameters;
	
		}
	
			// Inner function; generates solution for a single polling server
			public int[] findInitialSolution(ArrayList<testFormat> eventTasks, int counter) {
	
				EDPAlgorithm runEDP = new EDPAlgorithm();
				int[] bestParameters = new int[3];
	
				for (int staticParameters = 50; staticParameters < 1001; staticParameters = staticParameters + 10) {
	
					int prevWCRT = 0;
	
					for (int i = 0; i < staticParameters; i++) {
						EDPTuple testResult = runEDP.algorithm(i, staticParameters, staticParameters, eventTasks);
	
						if ( testResult.isResult()) {
	
							if (counter == 0) {
								bestParameters[0] = i;
								bestParameters[1] = staticParameters;
								bestParameters[2] = staticParameters;
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

				return bestParameters;
	
			}
	
			// Simulated Annealing
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

				System.out.println("Simulated Annealing Started");
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

						for (int i = 0; i < 3; i++) {
							testAllParameters[i][index] = currentParameters[i];
						}

						if (neighbourResult.isResult() && currentWCRT > 0 && currentWCRT < bestWCRT
								&& checkParameterConstraints(currentParameters)) {
							acceptableParametersThis.add(neighbourParameters);
							bestParameters = currentParameters;
							bestWCRT = currentWCRT;
							System.out.println("Current best, correct Response time: " + bestWCRT + " with parameters: "
									+ bestParameters[0] + " " + bestParameters[1] + " " + bestParameters[2]);
						}

					}
					t = t * alpha; // Change of temperature for each round
;

					Instant endTime = Instant.now();
					if (Duration.between(startTime, endTime).toSeconds() > 180) {
						break;
					}

				}

				Instant endTime = Instant.now();

				System.out.println(
						"Simulated Annealing duration:" + Duration.between(startTime, endTime).toSeconds() + " seconds");

				System.out.println("Current best, correct Response time: " + bestWCRT + " with parameters: " + bestParameters[0]
						+ " " + bestParameters[1] + " " + bestParameters[2]);

				foundParameters.add(acceptableParametersThis);

				return bestParameters;

			}
			
				// Function for generating a solution from the neighborhood
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
			
				// Function for restarting SA
				public int[] restartSimulatedAnnealing(int[] parameters, int[] initialParameters) {
					
					// Only restart if following is true, else just pass along current parameters
					if (parameters[0] == 1 || parameters[1] == 1 || parameters[0] == 2 || parameters[0] == 12000
							|| parameters[0] == 12000 || parameters[0] == 12000) {
						// Pick random parameter to change
						Random rand = new Random();
						int budget = initialParameters[0];
						int period = initialParameters[1];
						int deadline = initialParameters[2];

						int[] result = { budget, period, deadline };
						return result;

					} else {
						return parameters;
					}

				}
			
			// Main function for post-SA optimization (Filtering, Mutation & Finetuning)
			public int[][] findBestWorkingSolutions(ArrayList<ArrayList<testFormat>> partitions,
					ArrayList<testFormat> eventTasks) {

				int[] foundParametersSizes = new int[numberPollingServers];

				ArrayList<ArrayList<int[]>> filteredParameters = new ArrayList<ArrayList<int[]>>();

				int max = 0;

				int[][] bestParameters = new int[3][numberPollingServers];
				int[][] currentParameters = new int[3][numberPollingServers];

				for (int i = 0; i < numberPollingServers; i++) {
					
					// Remove duplicates
					for (int j = 0; j < foundParameters.get(i).size(); j++) {
						int[] temp = foundParameters.get(i).get(j);
						for (int k = j + 1; k < foundParameters.get(i).size(); k++) {
							if (temp == foundParameters.get(i).get(k)) {
								foundParameters.get(i).remove(k);
							}
						}
					}

					// Create array for parameters to pass onto mutation algorithm
					filteredParameters.add(new ArrayList<int[]>());
					foundParametersSizes[i] = foundParameters.get(i).size();
					if (foundParametersSizes[i] > max) {
						max = foundParametersSizes[i];
					}

					int trueParameterArray[] = new int[3];
					for (int j = 0; j < 3; j++) {
						currentParameters[j][i] = foundParameters.get(i).get(foundParametersSizes[i] - 1)[j];

						trueParameterArray[j] = currentParameters[j][i];
					}
					filteredParameters.get(i).add(trueParameterArray);
				}

				  // Set best parameters
				  for (int i = 0; i < numberPollingServers; i++) { 
					  bestParameters[0][i] = currentParameters[0][i]; 
					  bestParameters[1][i] = currentParameters[1][i];
					  bestParameters[2][i] = currentParameters[2][i];
				  }
				 
				// Reverse the lists of found parameters
				for (int i = 0; i < numberPollingServers; i++) {
					Collections.reverse(foundParameters.get(i));
				}


				double bestWCRT = Integer.MAX_VALUE;
				
				// Perform the constraint filtering
				for (int i = 1; i < max; i++) {

					for (int j = 0; j < numberPollingServers; j++) {

						// Change one set of parameters for j'th polling server
						if (foundParametersSizes[j] > i) {
							for (int k = 0; k < 3; k++) {
								currentParameters[k][j] = foundParameters.get(j).get(i)[k];
							}
						} else {
							continue;
						}

						if (checkDemandConstraint(currentParameters, demands) && checkUtilizationConstraint(currentParameters)) {
							int[] WCRTs = optimalPollingServerRun(partitions, currentParameters);
							double currentWCRT = calculateWeightedWCRT(WCRTs, partitions, eventTasks);

							// Add the parameters for pollingserver j that to the trueParmaters arraylist for
							// later use in mutation algorithm (Important we only have correct solutions)
							int trueParameterArray[] = new int[3];
							for (int h = 0; h < 3; h++) {
								trueParameterArray[h] = currentParameters[h][j];
							}
							filteredParameters.get(j).add(trueParameterArray);

							if (currentWCRT < bestWCRT) {
								for (int h = 0; h < numberPollingServers; h++) {
									bestParameters[0][h] = currentParameters[0][h];
									bestParameters[1][h] = currentParameters[1][h];
									bestParameters[2][h] = currentParameters[2][h];
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

				// Do mutation
				int[][] mutatedParameters = mutationAlgorithm(filteredParameters, bestParameters,
						bestWCRT, partitions, eventTasks);
				int[] mutatedWCRTs = optimalPollingServerRun(partitions, mutatedParameters);
				double mutatedWCRT = calculateWeightedWCRT(mutatedWCRTs, partitions, eventTasks);

				// Do finetuning using mutated parameters and WCRT 
				return finetuning(mutatedParameters, (int) mutatedWCRT, partitions, eventTasks);

			}
				
				// Function to perform mutation
				public int[][] mutationAlgorithm(ArrayList<ArrayList<int[]>> filteredParameters,
						int[][] bestParameters, double bestWCRT, ArrayList<ArrayList<testFormat>> partitions,
						ArrayList<testFormat> eventTasks) {
	
					int[][] currentParameters = new int[3][numberPollingServers];
					double currentWCRT = bestWCRT;
	
					System.out.println("Mutation started");
	
					
					for (int i = 0; i < 100; i++) {
	
						for (int j = 0; j < numberPollingServers; j++) {
	
							if (filteredParameters.get(j).size() == 1) {
								continue;
							}
	
							// Pick at random two parameters from the solution space from the specific
							// polling server
							Random rand = new Random();
							int firstPick = (int) (rand.nextInt(filteredParameters.get(j).size()));
							int secondPick = (int) (rand.nextInt(filteredParameters.get(j).size()));
	
							// If the same index is chosen, ensure that a different one is chosen
							while (firstPick == secondPick) {

								secondPick = (int) (rand.nextInt(filteredParameters.get(j).size()));
								;
							}
	
							// Create mutations and check the two new solutions
							ArrayList<int[]> mutatedParameters = mutateParameters(filteredParameters.get(j).get(firstPick),
									filteredParameters.get(j).get(secondPick));

							// Test both childs
							for (int h = 0; h < 2; h++) {
	
								for (int k = 0; k < numberPollingServers; k++) {
									currentParameters[0][k] = bestParameters[0][k];
									currentParameters[1][k] = bestParameters[1][k];
									currentParameters[2][k] = bestParameters[2][k];
								}
	
								currentParameters[0][j] = mutatedParameters.get(h)[0];
								currentParameters[1][j] = mutatedParameters.get(h)[1];
								currentParameters[2][j] = mutatedParameters.get(h)[2];
	
								EDPAlgorithm runEDP = new EDPAlgorithm();
								EDPTuple EDPresult = runEDP.algorithm(currentParameters[0][j], currentParameters[1][j],
										currentParameters[2][j], partitions.get(j));
								
								if (checkDemandConstraint(currentParameters, demands) && EDPresult.isResult()) {
									int[] WCRTs = optimalPollingServerRun(partitions, currentParameters);
									currentWCRT = calculateWeightedWCRT(WCRTs, partitions, eventTasks);

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
			
					// Function for mutating two solutions and returing their childs
					public ArrayList<int[]> mutateParameters(int[] firstPick, int[] secondPick) {
	
						Random rand = new Random();

						int parameter = rand.nextInt(3);	
						int[] firstResult = new int[3];
						int[] secondResult = new int[3];
	
						ArrayList<int[]> result = new ArrayList<int[]>();
						
						// 3 cases for each possible swap of parameters (Either swapping budget, period or deadline)
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
				
				// Function for performing last-step finetuning of the parameters
				public int[][] finetuning(int[][] currentParameters, int bestWCRT, ArrayList<ArrayList<testFormat>> partitions,
						ArrayList<testFormat> eventTasks) {
	
					System.out.println("Finetuning started");
	
					int[][] bestParameters = new int[3][numberPollingServers];
	
					for (int i = 0; i < numberPollingServers; i++) {
						bestParameters[0][i] = currentParameters[0][i];
						bestParameters[1][i] = currentParameters[1][i];
						bestParameters[2][i] = currentParameters[2][i];
					}
	
					int currentWCRT = 0;
	
					for (int i = 0; i < 10000; i++) {
	
						Random rand = new Random();
						int pollingServer = (int) (rand.nextInt(currentParameters[0].length));
						int choice = (int) (rand.nextInt(3));
						int operation = (int) (rand.nextInt(2));
						EDPAlgorithm runEDP = new EDPAlgorithm();
	
						// Test if result still satisfies constraint after incremention/decremention 
						if (operation == 0) {
							currentParameters[choice][pollingServer] = currentParameters[choice][pollingServer] - 1;
							int[] temp = { currentParameters[0][pollingServer], currentParameters[1][pollingServer],
									currentParameters[2][pollingServer] };
							if (!checkParameterConstraints(temp)
									|| !runEDP.algorithm(temp[0], temp[1], temp[2], partitions.get(pollingServer)).isResult()) {
								currentParameters[choice][pollingServer] = currentParameters[choice][pollingServer] + 1;
								continue;
							}
						} else {
							currentParameters[choice][pollingServer] = currentParameters[choice][pollingServer] + 1;
							int[] temp = { currentParameters[0][pollingServer], currentParameters[1][pollingServer],
									currentParameters[2][pollingServer] };
							if (!checkParameterConstraints(temp)
									|| !runEDP.algorithm(temp[0], temp[1], temp[2], partitions.get(pollingServer)).isResult()) {
								currentParameters[choice][pollingServer] = currentParameters[choice][pollingServer] - 1;
								continue;
							}
						}
	
						if (checkDemandConstraint(currentParameters, demands)) {
							int[] WCRTs = optimalPollingServerRun(partitions, currentParameters);
							currentWCRT = (int) calculateWeightedWCRT(WCRTs, partitions, eventTasks);
	
							if (currentWCRT < bestWCRT) {
								System.out.println(" Best found parameters: " + currentWCRT + "");
								for (int j = 0; j < numberPollingServers; j++) {
									bestParameters[0][j] = currentParameters[0][j];
									bestParameters[1][j] = currentParameters[1][j];
									bestParameters[2][j] = currentParameters[2][j];
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
			
			
	// Functions for checking constraints
				
	public boolean checkDemandConstraint(int[][] parameters, int[] demands) {

		int totalBudget1000 = 0;

		// Figure out the 1000-tick demand by the total budget of the polling tasks
		for (int i = 0; i < numberPollingServers; i++) {
			double multiple = 1000 / parameters[1][i];
			int budget1000 = (int) Math.ceil(parameters[0][i] * multiple);
			totalBudget1000 = totalBudget1000 + budget1000;
		}

		// Check if adding polling demand exceeds demand in any run of the EDF-algorithm
		for (int i = 1; i < 13; i++) {

			if (demands[i - 1] + totalBudget1000 * i > i * 1000) {
				return false;
			}
		}
		return true;

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

	public boolean checkParameterConstraints(int[] parameters) {

		if (parameters[0] < parameters[2] && parameters[2] < parameters[1] && parameters[0] < parameters[2] && 
				parameters[0] > 0 && parameters[1] > 0 && parameters[2] > 0 && parameters[0] < 12000
				&& parameters[1] < 12000 && parameters[2] < 12000
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

		if (result + utilizationTimeTasks < 1) {
			return true;
		} else {
			return false;
		}

	}

	// Main function for running the polling servers with their optimal parameters
	// and partitions to return their individual WCRTs
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
	public double calculateWeightedWCRT(int[] eventTasksWCRTs, ArrayList<ArrayList<testFormat>> partitions,
			ArrayList<testFormat> eventTasks) { // We are taking the average

		double result = 0;

		// Number of event tasks
		double n = (double) eventTasks.size();

		for (int i = 0; i < eventTasksWCRTs.length; i++) {
			double temp = ((partitions.get(i).size() / n));
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

}
