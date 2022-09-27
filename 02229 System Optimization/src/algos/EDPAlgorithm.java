package algos;

import java.util.ArrayList;

import objectClasses.EDPTuple;
import objectClasses.testFormat;

public class EDPAlgorithm {

	
	//TODO: Find optimal polling budget and period, for now period=deadline and budget=½*period)
	//Budget is the ticks allowed to be run each period.
	public EDPTuple algorithm(int pollingBudget, int pollingPeriod, int pollingDeadline, ArrayList<testFormat> tasks) {
		
		
		//Setup
		EDFAlgorithm edfAlgorithm = new EDFAlgorithm();
		int delta = pollingPeriod + pollingDeadline - 2* pollingBudget;
		double alpha = (double) pollingBudget/ (double) pollingPeriod; //Long for fractions?
		System.out.println("Aplha: "+alpha);
		int n = tasks.size();
		EDPTuple result = new EDPTuple(false, 0);
			
		int[] periodArray = new int[tasks.size()];
		for(int i=0; i<n; i++) {
			// Check that input data is valid
			if (!(tasks.get(i).getType().equals("ET"))) {
				System.out.println("EDP Algorithm was called with one TT Task");				
				return result;
				
			}
			periodArray[i] = tasks.get(i).getPeriod();
	
		}
		
		int hyperPeriod = edfAlgorithm.lcmMultiple(periodArray);
		System.out.println("EDP hyperPeriod: "+hyperPeriod);
		int responseTime = 0;
		
		
		for(testFormat x : tasks) {
			int tick = 0;
			responseTime = x.getDeadline()+1;
			
			while(tick<=hyperPeriod) {
				double supply = alpha*(tick-delta);
				double demand = 0;
				
				//Get list of tasks with larger period than current x
				for(testFormat j: getEventsLargerPriority(tasks, x )) {
					demand = (demand + Math.ceil(tick/j.getPeriod())+j.getDuration());
				}
				
				if(supply >= demand) {
					responseTime = tick;
					break;
				}				

				tick++;				
			}
			
			if (responseTime>x.getDeadline()) {
				result.setResponseTime(responseTime);
				return result;
			}		
		}
	
		result.setResult(true);
		result.setResponseTime(responseTime);
		return result;		
	}
	
	
	
	public ArrayList<testFormat> getEventsLargerPriority(ArrayList<testFormat> data, testFormat currentTask) {
		
		ArrayList<testFormat> result = new ArrayList<testFormat>();
		
		for(testFormat x: data) {
			if (x.getPriority()>=currentTask.getPriority()) {
				result.add(x);
			}
		}
		
		return result;
		
	}
	
}
