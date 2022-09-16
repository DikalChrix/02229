import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.time.*;

public class main {
	public static void main(String[] args) throws Exception {
		
		readTestCases();
		//System.out.println("Test");
		
	}
	
	public static void readTestCases() throws Exception {
	
		ArrayList<testFormat> data = new ArrayList<testFormat>();
		Scanner sc = new Scanner(new File("C:\\Users\\kicc7\\Downloads\\test cases\\test cases\\inf_10_10\\taskset__1643188013-a_0.1-b_0.1-n_30-m_20-d_unif-p_2000-q_4000-g_1000-t_5__0__tsk.csv"));  
		sc.useDelimiter(",");   //sets the delimiter pattern  
		
		String split2 = sc.nextLine();		
				
		while (sc.hasNext())  //returns a boolean value  
		{  
			String split = sc.nextLine();
			
			String[] splitString = split.split(";");
			
			testFormat test1 = new testFormat(splitString[1], Integer.parseInt(splitString[2]), Integer.parseInt(splitString[3]), splitString[4], Integer.parseInt(splitString[5]), Integer.parseInt(splitString[6]));
			
			data.add(test1);
			
		}   
		sc.close();  //closes the scanner  
		
		System.out.println("Data size:"+data.size());
		
		for(int i=0; i<data.size(); i++) {
			System.out.print(data.get(i).getName());
			System.out.print(data.get(i).getDuration());
			System.out.print(data.get(i).getPeriod());
			System.out.print(data.get(i).getType());
			System.out.print(data.get(i).getPriority());
			System.out.println(data.get(i).getDeadline());
		}
		
		EDFAlgorithm test = new EDFAlgorithm();
		test.testAlgorithm(data);
		
		}  
		
		
		
	}




