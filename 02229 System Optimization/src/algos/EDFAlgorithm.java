						package algos;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import objectClasses.testFormat;

public class EDFAlgorithm {
	
	int taskIndex = -1; //Remember index in of current job in readyList
	
	public int[] algorithm(ArrayList<testFormat> tasks, boolean disablePrints) {
		
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
		//readyList.addAll(tasks);
		
		
		//System.out.print("\n ReadyList before loop: ");
		//printJobsListName(readyList);
		
		for(testFormat x : tasks) {
			readyList.add(x.clone());
		}
		
		//System.out.println(Arrays.toString(periodArray));
		//t = lcmMultiple(periodArray);
		t = 12000;
		String[] schedule = new String[t];
		
		int wcrt = 0;
		
		
		//System.out.print("\n ReadyList before loop: ");
		//printJobsListName(readyList);
		
		// Simulation
		testFormat currJob = null;
		int minIdlePeriod = Integer.MAX_VALUE;
		int currIdlePeriod = 0;
		boolean idlePeriod = false;
		for(int tick = 1;tick<t; tick++) {
			// Check if new job has been released:
			readyList = getReady(tasks, readyList, tick);
			
			//System.out.print("\n ReadyList at start of cycle "+tick+": ");
			//printJobsListName(readyList);
			/*
			if (tick==4100) {
				System.exit(0);
			}
			*/
			
			
			if(readyList.size()==0) {
				currIdlePeriod++;
				idlePeriod = true;
				continue;
			}
			
			else if (idlePeriod)  {
				if (currIdlePeriod<minIdlePeriod) {
					minIdlePeriod = currIdlePeriod;
				}
				currIdlePeriod = 0;
				idlePeriod = false;
			}
			
			//System.out.print("\n ReadyList when initial tasks have been added: ");
			//printJobsListName(readyList);
			
			
			// Pick task with earliest deadline
			currJob = pickEarliestTask(readyList);
			// If this is the first time the task is worked on, set the start tick
			if(currJob.getStartTick() == -1) {
				currJob.setStartTick(tick);
			}
			schedule[tick] = currJob.getName();
			// Decrement execution time of current task
			currJob.setDuration(currJob.getDuration()-1);
			//System.out.println("Current duration: "+currJob.getDuration()+"\t Name: "+currJob.getName());
			//System.out.println(" Working on: "+currJob.getName()+", ticks remaining: "+currJob.getDuration());
						
			if(currJob.getDuration() == 0 && currJob.getDeadline()>=(tick-currJob.getStartTick())) {
				// Calculate response time
				currJob.setResponseTime(tick - currJob.getStartTick());
				// Update worst-case response time
				if(currJob.getResponseTime()>wcrt) {
					wcrt = currJob.getResponseTime();
				}
				//Remove finished job from readylist
				//System.out.println("Check");
				//System.out.println("Removed");
				readyList.remove(taskIndex);
			}
		}
		
		//System.out.println("\nEDF WRCT: "+wcrt);
		
		// TODO: Should also return the WCRT, not just print it 
		int[] output = {minIdlePeriod, wcrt};
 		
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
		
		/*
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
		
		
		
		System.out.println("Mul: "+mul);
		System.out.println("Res: "+res);
		
		
		return (numbers[numbers.length-1]/mul*res);
		*/
	}
	
	public ArrayList<testFormat> getReady(ArrayList<testFormat> tasks, ArrayList<testFormat> currReadyList, int tick) {
		
		for(int i=0; i<currReadyList.size(); i++) {
			if(currReadyList.get(i).getDuration() > 0 && currReadyList.get(i).getDeadline() <= tick) {		
				System.out.println("\nShit: "+currReadyList.get(i).getName()+" Duration: "+currReadyList.get(i).getDuration()+" Deadline: "+currReadyList.get(i).getDeadline()+" Tick: "+tick);
				return null;
			}			
		}
				
		for(int i=0; i<tasks.size(); i++) {
			if(tick % tasks.get(i).getPeriod() == 0) {
				
				//tasks.get(i).setDeadline(tick+tasks.get(i).getDeadline());
				testFormat newTask = tasks.get(i).clone();
				newTask.setDeadline(tick+newTask.getDeadline());
				currReadyList.add(newTask);
				
				//currReadyList.add(tasks.get(i).clone()); // TODO: Need deep copy
			}
		}
		
		return currReadyList;
		
		
	}

	public testFormat pickEarliestTask(ArrayList<testFormat> currReadyList) {
		
		int indexDeadline = Integer.MAX_VALUE;
		int earliestIndex = currReadyList.size()-1;
		for(int i=0; i<currReadyList.size();i++) {
			//System.out.println("Duration: "+currReadyList.get(i).getDeadline()+" Index: "+i);
			if(currReadyList.get(i).getDeadline()<indexDeadline) {
				earliestIndex = i;
				indexDeadline = currReadyList.get(i).getDeadline();
				//System.out.println("Index: "+i+" chosen");
			}
		}
		
		taskIndex = earliestIndex;
		//System.out.println("Index: "+earliestIndex);
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
		//int endTick = 0;
		//String previousTask = schedule[0];
		
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
