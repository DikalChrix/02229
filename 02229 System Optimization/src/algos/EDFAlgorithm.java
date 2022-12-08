						package algos;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import objectClasses.testFormat;

public class EDFAlgorithm {
	
	
	boolean disablePrints;
	int taskIndex = -1; //Remember index in of current job in readyList
	
	public int[] algorithm(ArrayList<testFormat> tasks, boolean disablePrints) {
		
		this.disablePrints = disablePrints;
		
		int n = tasks.size();
		int t=0;
		ArrayList<testFormat> readyList = new ArrayList<testFormat>();
		int[] periodArray = new int[tasks.size()];
		for(int i=0; i<n; i++) {
			// Check that input data is valid
			if (!(tasks.get(i).getType().equals("TT"))) {
				System.out.println("EDF Algorithm was called with one ET Task");
				int[] output ={-1,-1}; 
				return output;	
			}
			// Setup
			periodArray[i] = tasks.get(i).getPeriod();
		}
		
		for(testFormat x : tasks) {
			readyList.add(x.clone());
		}

		t = 12000;
		String[] schedule = new String[t];
		
		int wcrt = 0;
		
		// Simulation
		testFormat currJob = null;
		int minIdlePeriod = Integer.MAX_VALUE;
		int currIdlePeriod = 0;
		boolean idlePeriod = false;
		int totalIdlePeriod = 0;
		for(int tick = 1;tick<t; tick++) {
			// Check if new job has been released:
			readyList = getReady(tasks, readyList, tick);
			
			if(readyList == null) {
				return null;
			}
						
			if(readyList.size()==0) {
				currIdlePeriod++;
				idlePeriod = true;
				continue;
			}
			
			else if (idlePeriod)  {
				totalIdlePeriod = totalIdlePeriod + currIdlePeriod;
				if (currIdlePeriod<minIdlePeriod) {
					minIdlePeriod = currIdlePeriod;
				}
				currIdlePeriod = 0;
				idlePeriod = false;
			}

			// Pick task with earliest deadline
			currJob = pickEarliestTask(readyList);
			// If this is the first time the task is worked on, set the start tick
			if(currJob.getStartTick() == -1) {
				currJob.setStartTick(tick);
			}
			schedule[tick] = currJob.getName();
			// Decrement execution time of current task
			currJob.setDuration(currJob.getDuration()-1);
						
			if(currJob.getDuration() == 0 && currJob.getDeadline()>=(tick-currJob.getStartTick())) {
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
		
		int[] output = {minIdlePeriod, wcrt, totalIdlePeriod};
 		
		if(!disablePrints) { 
		
			schedulePrinter(schedule);
		
		}
		
		return output;
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
	
	public int lcmPair(int a, int b) {
		return (a*b)/gcdPair(a,b);
	}
	
	public int lcmMultiple(int[] numbers) {
		
		int temp = numbers[0];
		for(int i=1; i<numbers.length-1; i++) {
			temp = lcmPair(numbers[i],temp);
		}
		return temp; 
	}
	
	public ArrayList<testFormat> getReady(ArrayList<testFormat> tasks, ArrayList<testFormat> currReadyList, int tick) {
		
		for(int i=0; i<currReadyList.size(); i++) {
			if(currReadyList.get(i).getDuration() > 0 && currReadyList.get(i).getDeadline() <= tick) {		
				
				System.out.println(" Scheduling impossible with current parameters, instead using initial ones");
				
				return null;
			}			
		}
				
		for(int i=0; i<tasks.size(); i++) {
			if(tick % tasks.get(i).getPeriod() == 0) {
				
				testFormat newTask = tasks.get(i).clone();
				newTask.setDeadline(tick+newTask.getDeadline());
				currReadyList.add(newTask);
			}
		}
		
		return currReadyList;
		
		
	}

	public testFormat pickEarliestTask(ArrayList<testFormat> currReadyList) {
		
		int indexDeadline = Integer.MAX_VALUE;
		int earliestIndex = currReadyList.size()-1;
		for(int i=0; i<currReadyList.size();i++) {
			if(currReadyList.get(i).getDeadline()<indexDeadline) {
				earliestIndex = i;
				indexDeadline = currReadyList.get(i).getDeadline();
			}
		}
		
		taskIndex = earliestIndex;
		return currReadyList.get(earliestIndex);
		
	}
	
	public void printJobsListName(ArrayList<testFormat> list) {
		
		for(int i=0; i<list.size();i++) {
			System.out.print(list.get(i).getName()+"; ");
		}
		
	}

	@SuppressWarnings("unchecked")
	public ArrayList<testFormat> sortTaskDeadline(ArrayList<testFormat> currReadyList) {
		
		Collections.sort(currReadyList); 
		
		return currReadyList;
		
	}

	public testFormat pickEarliestTaskNew(ArrayList<testFormat> tasks) {
		
		int earliestDeadline = Integer.MAX_VALUE;
		int earliestIndex = -1;
		
		for(int i=0; i<tasks.size();i++) {
			if(tasks.get(i).getDeadline()<earliestDeadline) {
				earliestIndex = i;
			}
		}
		
		return tasks.get(earliestIndex);
	}
	
	
	public void schedulePrinter(String[] schedule) {
		
		int n = schedule.length;
		//boolean different = false;
		int startTick = 1;
		
		for(int i = 2; i<n-2; i++) {
			if(schedule[i] != schedule[i-1]) {
				
				if(schedule[i-1]==null) {
					System.out.println("IDLE"+" ["+startTick+","+(i-1)+"]");
				} else {
					System.out.println(schedule[i-1]+" ["+startTick+","+(i-1)+"]");
				}
				startTick=i;
			}
		}
		
		
		
	}
	
}
