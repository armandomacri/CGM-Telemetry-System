package iot.unipi.it;

import java.util.logging.LogManager;

import iot.unipi.it.services.CollectorMQTT;
import iot.unipi.it.services.RegistrationService;
import iot.unipi.it.services.TelemetryDBService;

public class Collector {
	private static RegistrationService rs = new RegistrationService();
	private static CollectorMQTT mc = new CollectorMQTT();
	private static TelemetryDBService th = TelemetryDBService.getInstance();
	
	public static void main(String[] args) throws InterruptedException {
		// Remove log messages (Californium)
        LogManager.getLogManager().reset();
        
        th.cleanDB();
        rs.start();

	}

}
