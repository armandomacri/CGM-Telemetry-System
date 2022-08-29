package iot.unipi.it;

import java.util.logging.LogManager;

import iot.unipi.it.services.RegistrationService;

public class Collector {
	private static RegistrationService rs = new RegistrationService();
	
	public static void main(String[] args) {
		// Remove log messages (Californium)
        LogManager.getLogManager().reset();
        
        rs.start();

	}

}
