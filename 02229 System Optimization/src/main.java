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
		//int avgWCRT = testReliability("inf_10_10\\taskset__1643188013-a_0.1-b_0.1-n_30-m_20-d_unif-p_2000-q_4000-g_1000-t_5__1__tsk.csv", 1);
		int avgWCRT = testReliability("test_separation\\test_separation.csv", 1);	
		//testFolderSets("inf_20_20\\taskset__1643188157-a_0.2-b_0.2-n_30-m_20-d_unif-p_2000-q_4000-g_1000-t_5__");
	}
	
	
	
	public static int runAlgorithm(String filepath) throws Exception {
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
		ArrayList<testFormat> eventTasks = new ArrayList<testFormat>();
		seperatedTasks = dataHandler.seperateTasks(mixedTasks);
		timeTasks = seperatedTasks.get(0);
		eventTasks = seperatedTasks.get(1);

		
		
		// Runs the EDF algorithm to schedule time tasks. Returns the minimum idle period
		// per 1000 ticks (Which is min. time we can run polling servers each 1000 tick)
		int[] EDFoutput = new int[2];
		EDFoutput = runEDF.algorithm(timeTasks, disablePrints);
		int minIdlePeriod = EDFoutput[0];
		int EDFWCRT = EDFoutput[1];
		//System.out.println("Minimum idle period: " + minIdlePeriod);
		
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
				.findOptimalPartitions(initialPollingServerPartitions, 10000, 0.999);
		
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
		EDFoutput = runEDF.algorithm(timeTasks, disablePrints);
		EDFWCRT = EDFoutput[1];
		//System.out.println("Final WCRT: " + EDFWCRT);	
		
		// Outputs the final WCRT, based on the last run of the EDF algorithm with the time tasks and the polling servers
		return EDFWCRT; 
	}

	public static int testReliability(String filepath, int iterations ) throws Exception {
		
		int totalWCRT = 0;
		int[] WCRTs = new int[iterations];
		Instant startTime = Instant.now();
		
		
		for(int i=0; i<iterations; i++) {
			WCRTs[i] = runAlgorithm(filepath);
			totalWCRT = totalWCRT + WCRTs[i];
		}
		
		int result = totalWCRT/iterations;
		int median = 0;
		if(iterations % 2 == 1) {
			median = WCRTs[(iterations)/2];
		} else {
			median = ((WCRTs[iterations/2]+WCRTs[(iterations/2)+1])/2);
		}
		
		int minWCRT = 0;
		int maxWCRT = 0;
		int stdDeviation = 0; 
		int stdSum = 0;
		// Calculate standard deviation. Using formula for sample:
		for(int i=0; i<iterations; i++) {
			stdSum = (WCRTs[i]-result)^2;
		}
		
		//stdDeviation = (int) Math.floor(Math.sqrt(stdSum/(iterations-1)));
		
		System.out.println("Average WCRT over "+iterations+" iterations: "+result);
		System.out.println("Minimum WCRT over "+iterations+" iterations: "+minWCRT);
		System.out.println("Median WCRT over "+iterations+" iterations: "+median);
		System.out.println("Maximum WCRT over "+iterations+" iterations: "+maxWCRT);
		
		
		Instant endTime = Instant.now();
		
		 System.out.println("Test duration:"+Duration.between(startTime,endTime).toMinutes()+" minutes");
		
		
		
		return result;
	}

	public static void testFolderSets(String filepath) throws Exception {
		
		for(int i=0; i<100;i++) {
			testReliability(filepath+i+"__tsk.csv", 10);
		}
	}
}
