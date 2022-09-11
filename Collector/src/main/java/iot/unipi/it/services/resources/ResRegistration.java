package iot.unipi.it.services.resources;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.server.resources.CoapExchange;

import iot.unipi.it.device.SmartDevice;
import iot.unipi.it.services.TelemetryDBService;

public class ResRegistration extends CoapResource{
	private static final Logger logger = LogManager.getLogger(SmartDevice.class);
	private static final TelemetryDBService th = TelemetryDBService.getInstance();
	private static Collection<SmartDevice> smartDevices = Collections.synchronizedList(new ArrayList<SmartDevice>());
	private static int samplingRate;
	
	public ResRegistration(int samplingRate) {
		super("registration");
		this.samplingRate = samplingRate;
	}
	
	@Override
	public void handlePOST(CoapExchange exchange) {
		exchange.accept();
		String deviceType = exchange.getRequestText();
        String ipAddress = exchange.getSourceAddress().getHostAddress();
        
		if (contains(ipAddress)<0) {
			if(th.addSensor(ipAddress)) {
        		synchronized(smartDevices) {
        			ResRegistration.smartDevices.add(new SmartDevice(ipAddress, this.samplingRate));
        		}
				logger.info("A new smart device: [" + ipAddress + "] is now registered!");
				exchange.respond(CoAP.ResponseCode.CREATED, "Registration, Success!".getBytes(StandardCharsets.UTF_8));
			}
			else {
				logger.error("Impossible to add new device!");
				exchange.respond(CoAP.ResponseCode.NOT_ACCEPTABLE, "Registration, Unsuccessful".getBytes(StandardCharsets.UTF_8));	
			}
		} else
			logger.warn("Device " + ipAddress + " already registered!");		
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
	
	private static int contains(final String ipAddress) {
		int idx = -1;
		
		for(SmartDevice device : smartDevices) {
			idx++;
			if(device.getIP().contentEquals(ipAddress))
				return idx;
		}
		return -1;
	}
	
	public static boolean removeDevice(final String ipAddress) {
		boolean success = true;
		int idx = contains(ipAddress);
		if (idx > -1) {
			synchronized(smartDevices) {
        		ResRegistration.smartDevices.remove(idx);
        	}
			
		} else {
			success = false;
		}
		
		return success;
	}
}
