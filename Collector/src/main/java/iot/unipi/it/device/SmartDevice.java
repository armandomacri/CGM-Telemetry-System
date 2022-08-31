package iot.unipi.it.device;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import iot.unipi.it.services.RegistrationService;
import iot.unipi.it.services.TelemetryDBService;

public class SmartDevice{
	private static final Logger logger = LogManager.getLogger(SmartDevice.class);
	private static final TelemetryDBService th = TelemetryDBService.getInstance();
	
	private final String ip;
	private CoapClient resGlucosio;
	//private CoapClient alarm;
	
		
	public SmartDevice(String ipAddress) {
		this.ip = ipAddress;
		this.resGlucosio = new CoapClient("coap://[" + ipAddress + "]/glucose");
		
		
		CoapObserveRelation newObserveTemperature = this.resGlucosio.observe(
				new CoapHandler() {
					public void onLoad(CoapResponse response) {
						long timestamp = 0;
						int value = 0;
						int nodeId = 0;
						System.out.println(response.getResponseText());
						JSONParser parser = new JSONParser();
						
						try {							
							JSONObject sensorMessage = (JSONObject) JSONValue.parseWithException(response.getResponseText());
							
							timestamp = Integer.parseInt(sensorMessage.get("timestamp").toString());
							value = Integer.parseInt(sensorMessage.get("glucose").toString());
							nodeId = Integer.parseInt(sensorMessage.get("node_id").toString());
						} catch (ParseException pe) {
							logger.error("Impossible to parse the response!", pe);
						}
						
						if(ip.endsWith(Integer.toString(nodeId))) {
							if(!th.addObservation(ip, value, timestamp))
								logger.warn("Impossible to add new observation!");
						} else {
							logger.warn("Message destination incorrect!");
						}
					}
					
					public void onError() {
						th.deleteSensor(ip);
						logger.error("OBSERVING FAILED with node " + ip);
                    }
				}, MediaTypeRegistry.APPLICATION_JSON);
                
                
	}

	public String getIP() {
		return ip;
	}

}
