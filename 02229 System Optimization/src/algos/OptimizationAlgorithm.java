package algos;

import java.util.ArrayList;

import objectClasses.EDPTuple;
import objectClasses.testFormat;

public class OptimizationAlgorithm {

	public int[] optimizePollingPeriod(int fixedPollingBudget, int fixedPollingDeadline, int hyperPeriod, ArrayList<testFormat> eventTasks) {
			
		int optimalPeriod = 0;
		int optimalResponseTime = Integer.MAX_VALUE;
		EDPTuple result = null;
		EDPAlgorithm runEDP = new EDPAlgorithm();
		
		
		
		
		for(int i = fixedPollingDeadline; i<hyperPeriod; i++ ) {
			result = runEDP.algorithm(fixedPollingBudget, i, fixedPollingDeadline, eventTasks);
			if(optimalResponseTime>result.getResponseTime() && result.isResult()) {
				optimalPeriod = i;
				optimalResponseTime = result.getResponseTime();
			}
			
			//System.out.println("Testing period: "+i+" Response Time: "+result.getResponseTime()+" Works: "+result.isResult());
		}
		
		int[] resultArray = new int[2];
		resultArray[0] = optimalResponseTime;
		resultArray[1] = optimalPeriod;
		
		return resultArray;
		
	}
	
	public int[] optimizeBoth(int fixedPollingBudget, int hyperPeriod, ArrayList<testFormat> eventTasks, int incremental, int max) {
		
		int optimalResponseTime = Integer.MAX_VALUE;
		int optimalPeriod = 0;
		int optimalDeadline = 0;
		int[] resultArray = new int[2]; 
		
		for(int j=fixedPollingBudget; j<max; j=j+incremental) {
			resultArray = optimizePollingPeriod(fixedPollingBudget, j, hyperPeriod, eventTasks);
			if(resultArray[0]<optimalResponseTime) {
				optimalResponseTime = resultArray[0];
				optimalPeriod = resultArray[1];
				optimalDeadline = j;		
			}
			
			System.out.println("Testing period: "+j+" Response Time: "+resultArray[0]+" Period: "+resultArray[1]);
		}
		
		int returnArray[] = {optimalResponseTime, optimalPeriod, optimalDeadline};
		return returnArray;
	}
	
	
}
