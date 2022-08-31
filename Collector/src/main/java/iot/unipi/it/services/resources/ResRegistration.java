package iot.unipi.it.services.resources;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;

import iot.unipi.it.device.SmartDevice;
import iot.unipi.it.services.TelemetryDBService;

public class ResRegistration extends CoapResource{
	private static final Logger logger = LogManager.getLogger(SmartDevice.class);
	private static final TelemetryDBService th = TelemetryDBService.getInstance();
	
	private static List<SmartDevice> smartDevices = new ArrayList<SmartDevice>();
	
	public ResRegistration() {
		super("registration");
	}
	
	@Override
	public void handlePOST(CoapExchange exchange) {
		String deviceType = exchange.getRequestText();
        String ipAddress = exchange.getSourceAddress().getHostAddress();
        
		boolean success = checkRegistration(ipAddress);
		
		if (success)
			if(th.addSensor(ipAddress)) {
				this.smartDevices.add(new SmartDevice(ipAddress));
				logger.info("A new smart device: [" + ipAddress + "] is now registered");
				exchange.respond(CoAP.ResponseCode.CREATED, "Registration, Success!".getBytes(StandardCharsets.UTF_8));
			}
			else {
				logger.error("Impossible to add new device!");
				exchange.respond(CoAP.ResponseCode.NOT_ACCEPTABLE, "Registratio, Unsuccessful".getBytes(StandardCharsets.UTF_8));	
			}			
	}
	
	@Override
	public void handleDELETE(CoapExchange exchange) {
		String[] request = exchange.getRequestText().split("-");
		String ipAddress = request[0];
		String deviceType = request[1];
		boolean success = true;
		
		if (success)
			exchange.respond(CoAP.ResponseCode.DELETED, "Cancellation Completed!".getBytes(StandardCharsets.UTF_8));
		else
			exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Cancellation not allowed!".getBytes(StandardCharsets.UTF_8));
	}
	
	private boolean checkRegistration(final String ipAddress) {

		for(SmartDevice device : this.smartDevices) {
			if(device.getIP().contentEquals(ipAddress))
				return false;
		}
		return true;
	}
	

}
