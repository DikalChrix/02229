import java.util.ArrayList;

import algos.EDFAlgorithm;
import algos.EDPAlgorithm;
import algos.OptimizationAlgorithm;
import dataBase.dataHandler;
import objectClasses.EDPTuple;
import objectClasses.testFormat;

//Test

public class main {
	public static void main(String[] args) throws Exception {
		
		// Initializes other classes:
		dataHandler dataHandler = new dataHandler();
		EDFAlgorithm runEDF = new EDFAlgorithm();

		// Reads the data from the csv files, creates the task objects and puts them in
		// an arraylist
		ArrayList<testFormat> mixedTasks = new ArrayList<testFormat>();
		mixedTasks = dataHandler.readTestData();

		// Separates event and time tasks from each other
		ArrayList<ArrayList<testFormat>> seperatedTasks = new ArrayList<ArrayList<testFormat>>();
		ArrayList<testFormat> timeTasks = new ArrayList<testFormat>();
		ArrayList<testFormat> eventTasks = new ArrayList<testFormat>();
		seperatedTasks = dataHandler.seperateTasks(mixedTasks);
		timeTasks = seperatedTasks.get(0);
		eventTasks = seperatedTasks.get(1);

		// Runs the EDF algorithm to schedule time tasks. Returns the minimum idle period
		// per 1000 ticks (Which is min. time we can run polling servers each 1000 tick)
		int[] EDFoutput = new int[2];
		EDFoutput = runEDF.algorithm(timeTasks);
		int minIdlePeriod = EDFoutput[0];
		int EDFWCRT = EDFoutput[1];
		System.out.println("Minimum idle period: " + minIdlePeriod);
		
		// Initialize the optimizatino algorithm with max required time for time tasks.
		OptimizationAlgorithm optimizeAlgo = new OptimizationAlgorithm(0, 1000-minIdlePeriod);

		// Finds optimal number of polling servers, returns initial partitions of the
		// event tasks between the polling servers
		ArrayList<ArrayList<testFormat>> initialPollingServerPartitions = optimizeAlgo
				.findNumberPollingServers(eventTasks);
		
		// Sets the number of polling servers to use in the optimization algorithm
		optimizeAlgo.setNumberPollingServers(initialPollingServerPartitions.size());

		// Finds optimal partitions based on the number of polling servers and the
		// initial partitions. Uses simulated annealing, with start temperature of 10000
		// and rate of 0.99
		ArrayList<ArrayList<testFormat>> optimalPartitions = optimizeAlgo
				.findOptimalPartitions(initialPollingServerPartitions, 10000, 0.99);
		
		// Finds optimal parameters for each polling server, given the optimal
		// partitions
		int[][] optimalParameters = optimizeAlgo.findOptimalParameters(optimalPartitions);

		// Runs the EDP algorithm for each polling task using their optimal partitions
		// and parameters and returns list of the individual WCRTs.
		int[] eventWCRTs = optimizeAlgo.optimalPollingServerRun(optimalPartitions, optimalParameters);

		// Calculates the average WCRT of all the polling servers
		int EDPWCRT = optimizeAlgo.finalEventWCRT(eventWCRTs, optimalPartitions, eventTasks);

		// Calculates the final, average WCRT of the whole dataset
		int finalWCRT = optimizeAlgo.finalWCRT(EDFWCRT, EDPWCRT, timeTasks.size(), eventTasks.size());

		// Converts polling servers to time tasks for a final run of the EDF-algorithm
		ArrayList<testFormat> finalTimeTasks = optimizeAlgo.createPollingServerTasks(timeTasks, optimalParameters);

		// Runs the EDF algorithm for a final time, using the initial time tasks as well
		// as the added polling servers with their optimal parameters.
		EDFoutput = runEDF.algorithm(timeTasks);
		EDFWCRT = EDFoutput[1];
		System.out.println("Final WCRT: " + EDFWCRT);
	}

}
