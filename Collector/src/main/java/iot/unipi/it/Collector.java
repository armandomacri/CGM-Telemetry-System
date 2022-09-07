package iot.unipi.it;

import java.util.Scanner;
import java.util.logging.LogManager;

import iot.unipi.it.services.CollectorMQTT;
import iot.unipi.it.services.RegistrationService;
import iot.unipi.it.services.TelemetryDBService;
import org.apache.logging.log4j.Logger;

public class Collector {
	private static TelemetryDBService th = TelemetryDBService.getInstance();
	private static int samplingRate = 8;
	
	
	public static void main(String[] args) throws InterruptedException {
		// Remove log messages (Californium)
        LogManager.getLogManager().reset();
        System.out.println("Insert sampling rate");
        int tmp;
        Scanner scanner = new Scanner(System.in);
        while (true) {
        	try {       		
        	    samplingRate=Integer.parseInt(scanner.nextLine());
        	    break;
        	  } catch (NumberFormatException e) {
        	    System.err.println("Only integers are allowed");
        	  }
        }
        CollectorMQTT mc = new CollectorMQTT(samplingRate);
        RegistrationService rs = new RegistrationService();
        th.cleanDB();
        rs.start();

	}

}
