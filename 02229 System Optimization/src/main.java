import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import algos.EDFAlgorithm;
import algos.EDPAlgorithm;
import algos.OptimizationAlgorithm;
import dataBase.dataHandler;
import objectClasses.EDPTuple;
import objectClasses.testFormat;

public class main {

	public static boolean disablePrints = true;

	public static void main(String[] args) throws Exception {
		testReliability(
				"test_separation\\inf_20_40\\taskset__1643188193-a_0.2-b_0.4-n_30-m_20-d_unif-p_2000-q_4000-g_1000-t_5__0__tsk.csv",
				10);
	}

	public static double[] runAlgorithm(String filepath) throws Exception {
		// Initializes other classes:
		dataHandler dataHandler = new dataHandler();
		EDFAlgorithm runEDF = new EDFAlgorithm();

		// Reads the data from the csv files, creates the task objects and puts them in
		// an arraylist
		ArrayList<testFormat> mixedTasks = new ArrayList<testFormat>();
		mixedTasks = dataHandler.readTestData(filepath);

		// Separates event and time tasks from each other
		ArrayList<ArrayList<testFormat>> seperatedTasks = new ArrayList<ArrayList<testFormat>>();
		ArrayList<testFormat> timeTasks = new ArrayList<testFormat>();
		ArrayList<testFormat> timeTasksCopy = new ArrayList<testFormat>();
		ArrayList<testFormat> eventTasks = new ArrayList<testFormat>();
		seperatedTasks = dataHandler.seperateTasks(mixedTasks);
		timeTasks = seperatedTasks.get(0);
		eventTasks = seperatedTasks.get(1);

		for (int i = 0; i < timeTasks.size(); i++) {
			timeTasksCopy.add(timeTasks.get(i).clone());
		}

		// Runs the EDF algorithm to schedule time tasks. Returns the minimum idle
		// period
		// per 1000 ticks (Which is min. time we can run polling servers each 1000 tick)
		int[] EDFoutput = new int[3];
		EDFoutput = runEDF.algorithm(timeTasks, disablePrints);
		int minIdlePeriod = EDFoutput[0];
		int EDFWCRT = EDFoutput[1];
		int totalIdlePeriod = EDFoutput[2];
		System.out.println("Minimum idle period: " + minIdlePeriod);
		System.out.println("Total idle period: " + totalIdlePeriod);
		// System.exit(0);

		// Initialize the optimizatino algorithm with max required time for time tasks.
		OptimizationAlgorithm optimizeAlgo = new OptimizationAlgorithm(0, calculateDemand(timeTasks), timeTasks);

		// Finds optimal number of polling servers, returns initial partitions of the
		// event tasks between the polling servers
		ArrayList<ArrayList<testFormat>> initialPollingServerPartitions = optimizeAlgo
				.findNumberPollingServers(eventTasks);

		// Sets the number of polling servers to use in the optimization algorithm
		// System.out.println("Number of polling servers:
		// "+initialPollingServerPartitions.size());
		optimizeAlgo.setNumberPollingServers(initialPollingServerPartitions.size());
		// calculateDemand(timeTasks);

		ArrayList<ArrayList<testFormat>> optimalPartitions = initialPollingServerPartitions;

		// Finds optimal partitions based on the number of polling servers and the
		// initial partitions. Uses simulated annealing, with start temperature of 10000
		// and rate of 0.99
		// ArrayList<ArrayList<testFormat>> optimalPartitions =
		// optimizeAlgo.findOptimalPartitions(initialPollingServerPartitions, 1000,
		// 0.99);

		// Finds optimal parameters for each polling server, given the optimal
		// partitions
		int[][] optimalParameters = optimizeAlgo.findOptimalParameters(optimalPartitions, eventTasks);

		// Runs the EDP algorithm for each polling task using their optimal partitions
		// and parameters and returns list of the individual WCRTs.
		int[] eventWCRTs = optimizeAlgo.optimalPollingServerRun(optimalPartitions, optimalParameters);

		// Calculates the average WCRT of all the polling servers
		int EDPWCRT = (int) optimizeAlgo.finalEventWCRT(eventWCRTs, optimalPartitions, eventTasks);

		// Calculates the final, average WCRT of the whole dataset
		int finalWCRT = optimizeAlgo.finalWCRT(EDFWCRT, EDPWCRT, timeTasks.size(), eventTasks.size());

		// Converts polling servers to time tasks for a final run of the EDF-algorithm
		ArrayList<testFormat> finalTimeTasks = optimizeAlgo.createPollingServerTasks(timeTasks, optimalParameters);

		// Calculates utilization, false if >1 (Processor is then overloaded)
		System.out.println("Utilization:" + optimizeAlgo.calculateUtilization(finalTimeTasks));

		double currentUtilization = optimizeAlgo.calculateUtilization(finalTimeTasks);

		// Runs the EDF algorithm for a final time, using the initial time tasks as well
		// as the added polling servers with their optimal parameters.
		EDFoutput = runEDF.algorithm(timeTasks, disablePrints);

		// If the optimized parameters result in an unscheduable set of tasks, go back
		// and use the initial paramters, which we know works

		/*
		 * System.out.println("Global parameters: "); for(int i=0;
		 * i<optimalPartitions.size(); i++) { for(int j=0;
		 * j<optimizeAlgo.getGlobalTrueParameters().get(i).size(); j++) {
		 * System.out.print(optimizeAlgo.getGlobalTrueParameters().get(i).get(j)[0]+" "
		 * +optimizeAlgo.getGlobalTrueParameters().get(i).get(j)[1]+" "+optimizeAlgo.
		 * getGlobalTrueParameters().get(i).get(j)[2]); } System.out.println(""); }
		 */

		/*
		
		int counter = 100;
		while (counter > 0) {
			// Try a small change to the parameters to allow them to be scheduled

			optimalParameters = optimizeAlgo.redo2(optimalParameters, optimalPartitions, eventTasks, timeTasks, currentUtilization);
			ArrayList<testFormat> safeTimeTasks = optimizeAlgo.createPollingServerTasks(timeTasks, optimalParameters);

			System.out.println("Re-trying with parameters: ");
			for (int j = 0; j < optimalPartitions.size(); j++) {
					System.out.print(optimalParameters[j][0] + " "
							+ optimalParameters[j][1] + " "
							+ optimalParameters[j][2]+"\t");
			}

			System.out.println("Utilization:" + optimizeAlgo.calculateUtilization(finalTimeTasks));

			if (optimizeAlgo.calculateUtilization(finalTimeTasks) < currentUtilization) {
				currentUtilization = optimizeAlgo.calculateUtilization(finalTimeTasks);
				EDFoutput = runEDF.algorithm(safeTimeTasks, disablePrints);

				if (EDFoutput != null) {
					break;
				} else {
					continue;
				}
			}

			counter = counter - 1;
			if (counter == 0) {
				throw new Exception("A feasible solution could not be found");
			}

			/*
			 * 
			 * System.out.println(" Re-trying with new parameters: "); for (int i = 0; i <
			 * optimalPartitions.size(); i++) { int size =
			 * optimizeAlgo.getGlobalTrueParameters().get(i).size();
			 * System.out.println("Size: "+size);
			 * 
			 * int counter = 0;
			 * 
			 * if (staticCounter >= size) { counter = size - 1; } else { counter =
			 * staticCounter; }
			 * 
			 * System.out.println("Counter: "+counter);
			 * 
			 * optimalParameters[0][i] =
			 * optimizeAlgo.getGlobalTrueParameters().get(i).get(counter)[0];
			 * optimalParameters[1][i] =
			 * optimizeAlgo.getGlobalTrueParameters().get(i).get(counter)[1];
			 * optimalParameters[2][i] =
			 * optimizeAlgo.getGlobalTrueParameters().get(i).get(counter)[2];
			 * System.out.print("New parameters for polling server "+i+" : "
			 * +optimalParameters[0][i]+" "+optimalParameters[1][i]+" "+optimalParameters[2]
			 * [i]+"\t"); } System.out.println(""); ArrayList<testFormat> safeTimeTasks =
			 * optimizeAlgo.createPollingServerTasks(timeTasks, optimalParameters);
			 * EDFoutput = runEDF.algorithm(safeTimeTasks, disablePrints);
			 * System.out.println("Utilization:" +
			 * optimizeAlgo.calculateUtilization(finalTimeTasks));
			 * 
			 */
		
		int count = 1;
		if(EDFoutput == null) {
			// Try again
			
			System.out.println(" Failed, re-trying");
			
			optimalParameters = optimizeAlgo.findOptimalParameters(optimalPartitions, eventTasks);
			finalTimeTasks = optimizeAlgo.createPollingServerTasks(timeTasks, optimalParameters);

			// Calculates utilization, false if >1 (Processor is then overloaded)
			System.out.println("Utilization:" + optimizeAlgo.calculateUtilization(finalTimeTasks));

			// Runs the EDF algorithm for a final time, using the initial time tasks as well
			// as the added polling servers with their optimal parameters.
			EDFoutput = runEDF.algorithm(timeTasks, disablePrints);
			
			
			count ++;
			
			if(count== 3) {
				throw new Exception("Failed 3 times in a row, can't handle this test case ");
			}
			
		}
		
		
		System.out.println("Utilization:" + optimizeAlgo.calculateUtilization(finalTimeTasks));
		EDFWCRT = EDFoutput[1];
		System.out.println("Final WCRT: " + EDFWCRT);

		double utilization = optimizeAlgo.calculateUtilization(finalTimeTasks);
		double[] result = { EDFWCRT, utilization };
		// Outputs the final WCRT, based on the last run of the EDF algorithm with the
		// time tasks and the polling servers
		return result;
	}

	public static int testReliability(String filepath, int iterations) throws Exception {

		int totalWCRT = 0;
		int[] WCRTs = new int[iterations];
		double[] utilization = new double[iterations];
		double totalUtilization = 0;
		double minUtilization = Integer.MAX_VALUE;
		double maxUtilization = -Integer.MAX_VALUE;
		double medianUtilization = 0;
		double stdDeviationUtilization = 0;

		// Speed analysis variables
		long[] secondsArray = new long[iterations];
		long maxDuration = -Integer.MAX_VALUE;
		long minDuration = Integer.MAX_VALUE;
		long avgDuration = 0;
		long medianDuration = 0;
		double stdDeviationDuration = 0;
		long totalDuration = 0;

		for (int i = 0; i < iterations; i++) {
			Instant startTime = Instant.now();
			double[] result = runAlgorithm(filepath);
			Instant endTime = Instant.now();
			secondsArray[i] = Duration.between(startTime, endTime).toSeconds();
			WCRTs[i] = (int) result[0];
			utilization[i] = result[1];
			totalWCRT = totalWCRT + WCRTs[i];
			totalUtilization = totalUtilization + utilization[i];
			totalDuration = totalDuration + secondsArray[i];

		}

		int avgWCRT = totalWCRT / iterations;
		int median = 0;
		if (iterations % 2 == 1) {
			median = WCRTs[(iterations) / 2];
			medianDuration = secondsArray[(iterations) / 2];
			medianUtilization = utilization[(iterations) / 2];
		} else {
			median = ((WCRTs[(iterations / 2) - 1] + WCRTs[(iterations / 2)]) / 2);
			medianDuration = ((secondsArray[(iterations / 2) - 1] + secondsArray[(iterations / 2)]) / 2);
			medianUtilization = ((utilization[(iterations / 2) - 1] + utilization[(iterations / 2)]) / 2);
		}

		int minWCRT = Integer.MAX_VALUE;
		int maxWCRT = -Integer.MAX_VALUE;
		double stdDeviation = 0;
		int stdSum = 0;

		avgDuration = totalDuration / iterations;

		long stdSumDuration = 0;

		// Calculate standard deviation. Using formula for sample:
		for (int i = 0; i < iterations; i++) {
			stdSum = (WCRTs[i] - avgWCRT) ^ 2;
			stdSumDuration = (secondsArray[i] - avgDuration) ^ 2;

			if (WCRTs[i] < minWCRT) {
				minWCRT = WCRTs[i];
			}

			if (WCRTs[i] > maxWCRT) {
				maxWCRT = WCRTs[i];
			}

			if (secondsArray[i] < minDuration) {
				minDuration = secondsArray[i];
			}

			if (secondsArray[i] > maxDuration) {
				maxDuration = secondsArray[i];
			}

			if (utilization[i] < minUtilization) {
				minUtilization = utilization[i];
			}

			if (utilization[i] > maxUtilization) {
				maxUtilization = utilization[i];
			}

		}

		double avgUtilization = totalUtilization / iterations;

		if (iterations > 1) {
			stdDeviation = Math.sqrt(stdSum / (iterations - 1));
			stdDeviationDuration = Math.sqrt(stdSumDuration / (iterations - 1));
		}

		System.out.println("********* TEST RESULTS *********");

		System.out.println("*** Reliability/Robustness analysis results: ");
		System.out.println("Average WCRT over " + iterations + " iterations: " + avgWCRT);
		System.out.println("Minimum WCRT over " + iterations + " iterations: " + minWCRT);
		System.out.println("Standard deviation over " + iterations + " iterations: " + stdDeviation);
		System.out.println("Median WCRT over " + iterations + " iterations: " + median);
		System.out.println("Maximum WCRT over " + iterations + " iterations: " + maxWCRT);

		System.out.println("*** Speed analysis results: ");
		System.out.println("Average duration in seconds over " + iterations + " iterations: " + avgDuration);
		System.out.println("Minimum duraiton in seconds over " + iterations + " iterations: " + minDuration);
		System.out.println("Standard deviation in seconds over " + iterations + " iterations: " + stdDeviationDuration);
		System.out.println("Median duration in seconds over " + iterations + " iterations: " + medianDuration);
		System.out.println("Maximum duration in seconds over " + iterations + " iterations: " + maxDuration);

		System.out.println("*** Utilization analysis results: ");
		System.out.println("Average utilization over " + iterations + " iterations: " + avgUtilization);
		System.out.println("Minimum utilization over " + iterations + " iterations: " + minUtilization);
		System.out.println("Standard utilization over " + iterations + " iterations: " + stdDeviationUtilization);
		System.out.println("Median utilization over " + iterations + " iterations: " + medianUtilization);
		System.out.println("Maximum utilization over " + iterations + " iterations: " + maxUtilization);

		return avgWCRT;
	}

	public static void testFolderSets(String filepath) throws Exception {

		for (int i = 0; i < 100; i++) {
			testReliability(filepath + i + "__tsk.csv", 10);
		}
	}

	// Calculate 1000-ticks demands from timetasks
	public static int[] calculateDemand(ArrayList<testFormat> timeTasks) {

		// Calculate demands for 2000, 3000 and 4000-ticks periods
		int[] periodicDemands = new int[3];

		for (int period = 2000; period < 4001; period = period + 1000) {
			for (int j = 0; j < timeTasks.size(); j++) {
				if (timeTasks.get(j).getPeriod() == period) {
					periodicDemands[((period - 1000) / 1000) - 1] = periodicDemands[((period - 1000) / 1000) - 1]
							+ timeTasks.get(j).getDuration();
				}
			}
			// System.out.println("Demand at "+period+":
			// "+periodicDemands[((period-1000)/1000)-1]);
		}

		int[] result = new int[12];

		for (int i = 1; i < 13; i++) {
			result[i - 1] = periodicDemands[0] * (i / 2) + periodicDemands[1] * (i / 3) + periodicDemands[2] * (i / 4);
		}

		return result;
	}

}
