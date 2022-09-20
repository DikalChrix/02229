import java.util.ArrayList;

import algos.EDFAlgorithm;
import dataBase.dataHandler;
import objectClasses.testFormat;

public class main {
	public static void main(String[] args) throws Exception {
		dataHandler dataHandler = new dataHandler();
		EDFAlgorithm test = new EDFAlgorithm();
		
		ArrayList<testFormat> data = new ArrayList<testFormat>();
		data = dataHandler.readTestData();
		
		dataHandler.removeET(data);
		
		test.testAlgorithm(data);
		System.out.println("Test");
		
	}  
		
		
	
		
	}




