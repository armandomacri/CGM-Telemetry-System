package iot.unipi.it.services.resources;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;

import iot.unipi.it.device.SmartDevice;

public class ResRegistration extends CoapResource{
	
	protected List<SmartDevice> smartDevices = new ArrayList<SmartDevice>();
	
	public ResRegistration() {
		super("registration");
	}
	
	@Override
	public void handlePOST(CoapExchange exchange) {
		String deviceType = exchange.getRequestText();
        String ipAddress = exchange.getSourceAddress().getHostAddress();
        
		boolean success = true;
		this.smartDevices.add(new SmartDevice(ipAddress));
		
		System.out.println(smartDevices.size());
		if (success)
            exchange.respond(CoAP.ResponseCode.CREATED, "Registration, Success!".getBytes(StandardCharsets.UTF_8));
        else
            exchange.respond(CoAP.ResponseCode.NOT_ACCEPTABLE, "Registratio, Unsuccessful".getBytes(StandardCharsets.UTF_8));
		
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
	

}
