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
		dataHandler dataHandler = new dataHandler();
		EDFAlgorithm runEDF = new EDFAlgorithm();
		EDPAlgorithm runEDP = new EDPAlgorithm();
		
		// Reads the data from the csv files, creates the task objects and puts them in an arraylist
		ArrayList<testFormat> mixedTasks = new ArrayList<testFormat>();
		mixedTasks = dataHandler.readTestData();
		
		// Seperates event and time taksks from each other
		ArrayList<ArrayList<testFormat>> seperatedTasks = new ArrayList<ArrayList<testFormat>>();
		ArrayList<testFormat> timeTasks = new ArrayList<testFormat>();
		ArrayList<testFormat> eventTasks = new ArrayList<testFormat>();
		seperatedTasks = dataHandler.seperateTasks(mixedTasks);
		timeTasks = seperatedTasks.get(0);
		eventTasks = seperatedTasks.get(1);
		
		// Runs the EDF algorithm to schedule time tasks. Returns the minmum idle period per 1000 ticks (Which is min. time we can run polling servers each 1000 tick)
		int[] EDFoutput = new int[2];
		EDFoutput = runEDF.algorithm(timeTasks);
		int minIdlePeriod = EDFoutput[0];	
		int EDFWCRT = EDFoutput[1];
		//TODO: Other parameters for EDP has been manually inserted according to runs of EDF, should be done dynamically.
		//TODO: Maybe calculate smallest idle time when running EDP, so we know the max amount of time we have for polling servers each period.
		//Delta is period where supply is negative/null, so time is reserved for TT tasks.
		System.out.println("Minimum idle period: "+minIdlePeriod);
		
		//System.exit(0);
		
		// Runs EPD algorithm once
		EDPTuple result =runEDP.algorithm(minIdlePeriod, 1000, 1000, eventTasks);
		System.out.println("EDP result: "+result.isResult()+" ResponseTime: "+result.getResponseTime());
		
		//Testing Optimization Algorithm:
		OptimizationAlgorithm optimizeAlgo = new OptimizationAlgorithm();
		
		//int[] resultArray = new int[2];
		//resultArray = optimizeAlgo.optimizePollingPeriod(minIdlePeriod, minIdlePeriod, 12000, eventTasks);
		//System.out.println("OptimalPollingPeriod: "+resultArray[1]+" Best ReponseTime"+resultArray[0]);
		
		//int[] resultArray = new int[3];
		//resultArray = optimizeAlgo.optimizeBoth(minIdlePeriod, 12000, eventTasks, 25, 1000);
		//System.out.println("Optimal Period: "+resultArray[0]+" Optimal Deadline: "+resultArray[1]+" Best ResponseTime: "+resultArray[2]);
	
		//Testing simulated Annealing:
		//int midSearchSpace = (12000 - minIdlePeriod)/2; // Find the middle of the search space and use as starting parameters
		
		//int[] initialSolution = {minIdlePeriod, minIdlePeriod, minIdlePeriod}; //
		//int[] initialSolution = {minIdlePeriod, 1000, 1000}; //Chris test
		//int[] testRes = optimizeAlgo.simulatedAnnealing(initialSolution, 100000, 0.999, eventTasks, minIdlePeriod);
		//System.out.println("Best parameters with Simulated Annealing: Budget "+testRes[0]+" Period: "+testRes[1]+" Deadline "+testRes[2]);
	
		
		// FIND OPTIMAL NUMBER OF POLLING SERVERS		
		ArrayList<ArrayList<testFormat>> initialPollingServerPartitions = optimizeAlgo.findNumberPollingServers(eventTasks, minIdlePeriod);
		
		System.out.println(" STARTING OPTIMIZATION");
				
		// FIND BEST PARTITIONS FOR POLLING SERVERS
		ArrayList<ArrayList<testFormat>> optimalPartitions = optimizeAlgo.simulatedAnnealingPollingServers(initialPollingServerPartitions, 10000, 0.99, minIdlePeriod);
		
		//Print out result	
		System.out.println("OPTIMAL PARTITIONS: \t");
		optimizeAlgo.printOutPartitions(optimalPartitions);
		System.out.println("\nWCRT using single polling server: "+result.getResponseTime());
		
		// FIND BEST PARAMETERS FOR POLLING SERVERS
		int[][] optimalParameters = optimizeAlgo.findOptimalPollingServerParameters(optimalPartitions, minIdlePeriod);
		
		// GET WCRTs OF ALL POLLING SERVERS USING THEIR BEST PARAMETERS
		int[] eventWCRTs = optimizeAlgo.optimalPollingServerRun(optimalPartitions, optimalParameters);
		
		// CALCULATE WCRT OF ALL POLLING SERVERS
		int EDPWCRT = optimizeAlgo.finalEventWCRT(eventWCRTs);
		
		System.out.println("test "+EDPWCRT+" "+EDFWCRT);
		
		// CALCULATE FINAL WCRT OF EVENT AND TIME TASKS
		int finalWCRT = optimizeAlgo.finalWCRT(EDFWCRT, EDPWCRT, timeTasks.size(), eventTasks.size());
		
		// OUTPUT WCRT OF DATASET
		System.out.println("FINAL WCRT: "+finalWCRT);
		
		// CREATE POLLING TASKS AS TIME TASKS
		ArrayList<testFormat> finalTimeTasks = optimizeAlgo.createPollingServerTasks(timeTasks, optimalParameters);
		
		// RUN EDF AGAIN WITH NORMAL TIME TASKS AND POLLING TASKS. PRINT OUT SCHEDULE
		EDFoutput = runEDF.algorithm(timeTasks);
		EDFWCRT = EDFoutput[1];
		//TODO: Other parameters for EDP has been manually inserted according to runs of EDF, should be done dynamically.
		//TODO: Maybe calculate smallest idle time when running EDP, so we know the max amount of time we have for polling servers each period.
		//Delta is period where supply is negative/null, so time is reserved for TT tasks.
		System.out.println("Final WCRT: "+EDFWCRT);
		System.out.println("optimized Deadline: "+optimalParameters[2][0]);
		
		
		
		
		
		// TODO: Weighted Sum Method for calculating final WRCT
		// Idea for WCRT of polling servers: Let weight be equal to percentage of event tasks handled, then take average of sum.
		// For EDF and EDP combination, base weight on handled percentage of combined set of tasks
		
		
		
		
		
		
	}  
		
		
	
		
	}




