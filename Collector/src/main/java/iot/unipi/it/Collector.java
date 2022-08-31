package iot.unipi.it;

import java.util.logging.LogManager;

import iot.unipi.it.services.RegistrationService;
import iot.unipi.it.services.TelemetryDBService;

public class Collector {
	private static RegistrationService rs = new RegistrationService();
	
	
	public static void main(String[] args) {
		// Remove log messages (Californium)
        LogManager.getLogManager().reset();
        
        TelemetryDBService th = TelemetryDBService.getInstance();
        th.cleanDB();
        rs.start();

	}

}
