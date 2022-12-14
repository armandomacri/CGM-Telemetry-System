package iot.unipi.it.device;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import iot.unipi.it.services.RegistrationService;
import iot.unipi.it.services.TelemetryDBService;
import iot.unipi.it.services.resources.ResRegistration;

public class SmartDevice{
	private static int LOWER_BOUND_GLU = 100;
	private static int UPPER_BOUND_GLU = 125;
	private static final Logger logger = LogManager.getLogger(SmartDevice.class);
	private static final TelemetryDBService th = TelemetryDBService.getInstance();

	private final String ip;
	private CoapClient resGlucosio;
	private CoapClient resAlarm;
	private boolean stopObserve = false;
	private short state = 0;
	private int samplingRate;
	
		
	public SmartDevice(String ipAddress, int samplingRate) {
		this.ip = ipAddress;
		this.samplingRate = samplingRate;
		
		this.resGlucosio = new CoapClient("coap://[" + ipAddress + "]/glucose");
		this.resAlarm = new CoapClient("coap://[" + ipAddress + "]/alarm");
		
		//set sampling rate
		String payload = Integer.toString(samplingRate);
    	Request req = new Request(Code.PUT);
		req.setPayload(payload);
    	req.setURI("coap://[" + ip + "]/glucose");
    	req.send();
		
		CoapObserveRelation newObserveGlucose = this.resGlucosio.observe(
				new CoapHandler() {
					public void onLoad(CoapResponse response) {
						boolean success = true;
						
						long timestamp = 0;
						int value = 0;
						int nodeId = 0;
						if(response.getResponseText() == null || response.getResponseText() == "")
							return;
						
						try {							
							JSONObject sensorMessage = (JSONObject) JSONValue.parseWithException(response.getResponseText());
							
							timestamp = Integer.parseInt(sensorMessage.get("timestamp").toString());
							value = Integer.parseInt(sensorMessage.get("glucose").toString());
							nodeId = Integer.parseInt(sensorMessage.get("node_id").toString());
							
						} catch (ParseException pe) {
							System.out.println(response.getResponseText());
							logger.error("Impossible to parse the response!", pe);
							success = false;
						}
						
						if(!success)
							return;
						
						if(ip.endsWith(Integer.toString(nodeId)) && success) {
							if(!th.addObservation(ip, value, timestamp)) {
								logger.warn("Impossible to add new observation!");
								success = false;
							}
						} else {
							logger.warn("Message destination incorrect!");
							
						}
						
						if(!success)
							return;
						
						if (value > LOWER_BOUND_GLU && value <= UPPER_BOUND_GLU) {
							if(state != 1) {
								state = 1;
								String payload = "mode=on";
	                        	Request req = new Request(Code.POST);
								req.setPayload(payload);
	                        	req.setURI("coap://[" + ip + "]/alarm?color=y");
	                        	req.send();
	                        	logger.info("[WARNING] - "+ip+" - the level of glucose ("+value+" mg/dL) is higher than normal!");
	                        	th.updateSensorState(ip, state);
							}
						} else if(value > UPPER_BOUND_GLU)
						{
							if(state != 2) {
								state = 2;
								String payload = "mode=on";
	                        	Request req = new Request(Code.POST);
								req.setPayload(payload);
	                        	req.setURI("coap://[" + ip + "]/alarm?color=r");
	                        	req.send();
	                        	logger.info("[CRITICAL] - "+ip+" - the level of glucose ("+value+" mg/dL) is too high!");
	                        	th.updateSensorState(ip, state);
							}
						} else {
							if(state != 0) {
								state = 0;
								String color = (state == 1 ? "y" : "r");
								String payload = "mode=off";
	                        	Request req = new Request(Code.POST);
								req.setPayload(payload);
	                        	req.setURI("coap://[" + ip + "]/alarm?color=g");
	                        	req.send();
	                        	logger.info("[NORMAL] - "+ip+" - the level of glucose ("+value+" mg/dL) is normal!");
	                        	th.updateSensorState(ip, state);
							}
						}					
					}
					
					public void onError() {
						stopObserve = true;
						logger.error("OBSERVING FAILED with sensor " + ip);
						
						if(ResRegistration.removeDevice(ip)) {
							th.deleteSensor(ip);
							logger.error("OBSERVING FAILED with sensor " + ip);
						} else {
							logger.error("Sensor not in the registered list " + ip);
						}
                    
					}
				}, MediaTypeRegistry.APPLICATION_JSON);
		
		if (stopObserve) {
			newObserveGlucose.proactiveCancel();
		}
	}

	public String getIP() {
		return ip;
	}

}
