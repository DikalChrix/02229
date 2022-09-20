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
			sc = new Scanner(new File("src\\dataBase\\inf_10_10\\taskset__1643188013-a_0.1-b_0.1-n_30-m_20-d_unif-p_2000-q_4000-g_1000-t_5__0__tsk.csv"));
			sc.useDelimiter(",");   //sets the delimiter pattern  

			String split = sc.nextLine();		
			
			while (sc.hasNextLine())  //returns a boolean value  
			{  
				split = sc.nextLine();
				
				String[] splitString = split.split(";");
				
				testFormat test1 = new testFormat(splitString[1], Integer.parseInt(splitString[2]), Integer.parseInt(splitString[3]), splitString[4], Integer.parseInt(splitString[5]), Integer.parseInt(splitString[6]));
				
				data.add(test1);
			}
			sc.close();  //closes the scanner 
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		return data;
	}
	
	public void removeET(ArrayList<testFormat> tasks) {
		int counter = 0; //Each time we remove an element, the array becomes shorter
		int n = tasks.size();
		for(int i=0; i<n; i++) {
			if(tasks.get(i-counter).getType().equals("ET")) {
				tasks.remove(i-counter);
				counter++;
			}
		}
		
	}
}
