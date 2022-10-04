package dataBase;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import objectClasses.testFormat;

public class dataHandler {
	
	public ArrayList<testFormat> readTestData () {
		ArrayList<testFormat> data = new ArrayList<testFormat>();
		Scanner sc;
		try {
			sc = new Scanner(new File("src\\dataBase\\test_separation\\test_separation.csv"));
			sc.useDelimiter(",");   //sets the delimiter pattern
			testFormat test1 = new testFormat();
			int separationFlag = 0;
			String split = sc.nextLine();
			if (split.contains("separation")) {
				separationFlag = 1;
			}
			
			while (sc.hasNextLine())  //returns a boolean value  
			{  
				split = sc.nextLine();
				split = split.replace(",",";");
				if (split.length() < 10) {
					break;
				}
				System.out.println(split);
				String[] splitString = split.split(";");
				if (separationFlag == 1) {
					test1 = new testFormat(splitString[1], Integer.parseInt(splitString[2]), Integer.parseInt(splitString[3]), splitString[4], Integer.parseInt(splitString[5]), Integer.parseInt(splitString[6]), Integer.parseInt(splitString[7]));
					
				} else  {
					test1 = new testFormat(splitString[1], Integer.parseInt(splitString[2]), Integer.parseInt(splitString[3]), splitString[4], Integer.parseInt(splitString[5]), Integer.parseInt(splitString[6]), 0);
					
				}
				
				data.add(test1);
			}
			sc.close();  //closes the scanner
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		return data;
	}
	
	public ArrayList<ArrayList<testFormat>> seperateTasks(ArrayList<testFormat> tasks) {
		
		ArrayList<testFormat> timeTasks = new ArrayList<testFormat>();
		ArrayList<testFormat> eventTasks = new ArrayList<testFormat>();
		
		int n = tasks.size();
		for(int i=0; i<n; i++) {
			if(tasks.get(i).getType().equals("TT")) {
				timeTasks.add(tasks.get(i).clone());
			}
			else {
				eventTasks.add(tasks.get(i).clone());
			}
			//tasks.remove(i-counter);
			//counter++;
		}
		
		ArrayList<ArrayList<testFormat>> result = new ArrayList<ArrayList<testFormat>>();
		result.add(timeTasks);
		result.add(eventTasks);
		return result;	
	}
}
