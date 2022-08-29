package iot.unipi.it.device;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;

import iot.unipi.it.services.RegistrationService;

public class SmartDevice{
	private static final Logger logger = LogManager.getLogger(SmartDevice.class);
	
	private String ip;
	private CoapClient resGlucosio;
	//private CoapClient alarm;
	
		
	public SmartDevice(String ipAddress) {
		this.ip = ipAddress;
		this.resGlucosio = new CoapClient("coap://[" + ipAddress + "]/temperature");
		
		logger.info("A new smart device: [" + ipAddress + "] is now registered");
		
		CoapObserveRelation newObserveTemperature = this.resGlucosio.observe(
				new CoapHandler() {
					public void onLoad(CoapResponse response) {
						System.out.println(response.getResponseText());
					}
					
					public void onError() { //provare a creare un exception
						logger.error("----OBSERVING FAILED----");
                        //Logger.error("coap://[" + ip + "]/temperature_sensor: "+ " OBSERVING FAILED");
                    }
				});
                
                
	}

	public String getIP() {
		return ip;
	}

}
