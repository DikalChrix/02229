import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class EDFAlgorithm {
	
	int taskIndex = -1; //Remember index in of current job in readyList
	
	@SuppressWarnings("unchecked")
	public void testAlgorithm(ArrayList<testFormat> tasks) {
		
		int n = tasks.size();
		int t=0;
		ArrayList<testFormat> readyList = null;
		int[] periodArray = new int[tasks.size()];
		for(int i=0; i<n; i++) {
			// Check that input data is valid
			if (!(tasks.get(i).getType().equals("TT"))) {
				System.out.println("EDF Algorithm was called with one ET Task");
				return;
				
			}
			// Setup
			periodArray[i] = tasks.get(i).getPeriod();
			//Shallow copy to readylist here, have to check if deep copy is needed
			readyList = (ArrayList<testFormat>) tasks.clone();
			// Schedule is initially just a integer list with numbers, where each number represents what job ran in that specific tick(index):
		}

		//System.out.println(Arrays.toString(periodArray));
		t = lcmMultiple(periodArray);
		String[] schedule = new String[t];
		
		int wcrt = 0;
		
		// Simulation
		testFormat currJob = null;
		for(int tick = 1;tick<t; tick++) {
			// Check if new job has been released:
			readyList = getReady(tasks, readyList, tick);
			// Pick task with earliest deadline
			currJob = pickEarliestTask(readyList);
			// If this is the first time the task is worked on, set the start tick
			if(currJob.getStartTick() == -1) {
				currJob.setStartTick(tick);
			}
			schedule[tick] = currJob.getName();
			// Decrement execution time of current task
			currJob.setDuration(currJob.getDuration()-1);
			
			if(currJob.getDuration() == 0) {
				// Calculate response time
				currJob.setResponseTime(tick - currJob.getStartTick());
				// Update worst-case response time
				if(currJob.getResponseTime()>wcrt) {
					wcrt = currJob.getResponseTime();
				}
				//Remove finished job from readylist
				readyList.remove(taskIndex);
			}
		}
			
	}
	
	
	public int gcdPair(int a, int b) {
		int temp;
		while(a!=0) {
			temp = b;
			b=a;
			a=temp % a;
		}
		return b;
	}

	public int lcmMultiple(int[] numbers) {
		System.out.println(Arrays.toString(numbers));
		int res = numbers[0];
		for(int i=1; i<numbers.length-1; i++) {
			res = gcdPair(res, numbers[i]);
		}
		
		int mul = numbers[0];
		for(int i=1; i<numbers.length-2;i++) {
			mul = mul * numbers[i];
			System.out.println("Mul in loop: "+mul);
		}
		
		//TODO: Can't multiply that many numbers together, find another solution to calculate lcm of multiple integers
		/*
		if(res==0) {
			res = 1;
		}
		
		if(mul==0) {
			mul = 1;
		}
		
		*/
		
		System.out.println("Mul: "+mul);
		System.out.println("Res: "+res);
		
		
		return (numbers[numbers.length-1]/mul*res);
	}
	
	public ArrayList<testFormat> getReady(ArrayList<testFormat> tasks, ArrayList<testFormat> currReadyList, int tick) {
		
		for(int i=0; i<tasks.size(); i++) {
			if(tick % tasks.get(i).getPeriod() == 0) {
				currReadyList.add(tasks.get(i));
			}
		}
		
		return currReadyList;
		
		
	}

	public testFormat pickEarliestTask(ArrayList<testFormat> currReadyList) {
		
		int earliestIndex = Integer.MAX_VALUE;
		for(int i=0; i<currReadyList.size();i++) {
			if(currReadyList.get(i).getDeadline()<earliestIndex) {
				earliestIndex = i;
			}
		}
		
		taskIndex = earliestIndex;
		return currReadyList.get(earliestIndex);
		
	}

	@SuppressWarnings("unchecked")
	public ArrayList<testFormat> sortTaskDeadline(ArrayList<testFormat> currReadyList) {
		
		Collections.sort(currReadyList); 
		
		return currReadyList;
		
	}
}
