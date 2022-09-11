package iot.unipi.it.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.CoapServer;

import iot.unipi.it.services.resources.ResRegistration;

public class RegistrationService extends CoapServer {
	private static final Logger logger = LogManager.getLogger(RegistrationService.class);
	
	public RegistrationService(int samplingRate) {
		this.add(new ResRegistration(samplingRate));
		logger.info("Coap server is ready!");
    }

}
