import java.util.ArrayList;

import algos.EDFAlgorithm;
import algos.EDPAlgorithm;
import algos.OptimizationAlgorithm;
import dataBase.dataHandler;
import objectClasses.EDPTuple;
import objectClasses.testFormat;

public class main {
	public static void main(String[] args) throws Exception {
		dataHandler dataHandler = new dataHandler();
		EDFAlgorithm runEDF = new EDFAlgorithm();
		EDPAlgorithm runEDP = new EDPAlgorithm();
		
		ArrayList<testFormat> mixedTasks = new ArrayList<testFormat>();
		mixedTasks = dataHandler.readTestData();
		
		ArrayList<ArrayList<testFormat>> seperatedTasks = new ArrayList<ArrayList<testFormat>>();
		ArrayList<testFormat> timeTasks = new ArrayList<testFormat>();
		ArrayList<testFormat> eventTasks = new ArrayList<testFormat>();
		
		seperatedTasks = dataHandler.seperateTasks(mixedTasks);
		timeTasks = seperatedTasks.get(0);
		eventTasks = seperatedTasks.get(1);
		
		
		int minIdlePeriod = runEDF.algorithm(timeTasks);
		//TODO: Other parameters for EDP has been manually inserted according to runs of EDF, should be done dynamically.
		//TODO: Maybe calculate smallest idle time when running EDP, so we know the max amount of time we have for polling servers each period.
		//Delta is period where supply is negative/null, so time is reserved for TT tasks.
		System.out.println("Minimum idle period: "+minIdlePeriod);
		EDPTuple result =runEDP.algorithm(minIdlePeriod, 1000, 1000, eventTasks);
		System.out.println("EDP result: "+result.isResult()+" ResponseTime: "+result.getResponseTime());
		
		
		//Testing Optimization Algorithm:
		OptimizationAlgorithm optimizeAlgo = new OptimizationAlgorithm();
		
		//int[] resultArray = new int[2];
		//resultArray = optimizeAlgo.optimizePollingPeriod(minIdlePeriod, minIdlePeriod, 12000, eventTasks);
		//System.out.println("OptimalPollingPeriod: "+resultArray[1]+" Best ReponseTime"+resultArray[0]);
		int[] resultArray = new int[3];
		resultArray = optimizeAlgo.optimizeBoth(minIdlePeriod, 12000, eventTasks, 25, 1000);
		System.out.println("Optimal Period: "+resultArray[0]+" Optimal Deadline: "+resultArray[1]+" Best ResponseTime: "+resultArray[2]);
	
	
	
	}  
		
		
	
		
	}




