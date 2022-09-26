import java.util.ArrayList;

import algos.EDFAlgorithm;
import algos.EDPAlgorithm;
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
		
		
		//runEDF.algorithm(timeTasks);
		//TODO: Other parameters for EDP has been manually inserted according to runs of EDF, should be done dynamically.
		//TODO: Maybe calculate smallest idle time when running EDP, so we know the max amount of time we have for polling servers each period. 
		EDPTuple result =runEDP.algorithm(602, 600, 300, eventTasks);
		System.out.println("EDP result: "+result.isResult()+" ResponseTime: "+result.getResponseTime());
		
	}  
		
		
	
		
	}




