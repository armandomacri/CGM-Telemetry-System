package iot.unipi.it.services;

import java.sql.SQLException;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import iot.unipi.it.device.SmartDevice;
import iot.unipi.it.services.TelemetryDBService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CollectorMQTT implements MqttCallback{
	
	private static String broker = "tcp://127.0.0.1:1883";
	private static String clientId = "JavaCollector";
	private static String subTopic = "glucose";
	private static String pubTopic = "alarm";
	private static String pubTopic2 = "sampling_rate";
	private static MqttClient mqttClient = null;
	private short state = 0;
	private static final Logger logger = LogManager.getLogger(CollectorMQTT.class);
	private static final TelemetryDBService th = TelemetryDBService.getInstance();
	private static int samplingRate;
	
	public CollectorMQTT(int samplingRate) {
		do {
			this.samplingRate = samplingRate;
			int timeWindow = 50000;
			try {
				this.mqttClient = new MqttClient(this.broker,this.clientId);
				this.mqttClient.setCallback( this );
				this.mqttClient.connect();
				this.mqttClient.subscribe(this.subTopic);
			}catch(MqttException me) {
				logger.error("CollectorMQTT unable to connect, Retrying ...", me);
				try {
					Thread.sleep(timeWindow);
				} catch (InterruptedException ie) {
					logger.error("Something wrong with thread sleep!", ie);
				}
			}
		}while(!this.mqttClient.isConnected());
		logger.info("MQTT Connected!");
	}
	
	public void publish(String content, String node) throws MqttException{
		try {
			MqttMessage message = new MqttMessage(content.getBytes());
			this.mqttClient.publish(this.pubTopic+node, message);
			logger.info("MQTT alarm published!");
		} catch(MqttException me) {
			logger.error("Impossible to publish!", me);
		}
	}
	

	public void changeInterval(String interval) throws MqttException{
		try {
			MqttMessage message = new MqttMessage(interval.getBytes());
			this.mqttClient.publish(this.pubTopic2, message);
			logger.info("MQTT sampling_rate published!");
		} catch(MqttException me) {
			logger.error("Impossible to publish!", me);
		}
	}
	
	public void connectionLost(Throwable cause) {
		// TODO Auto-generated method stub
		logger.error("Connection is broken!");
		int timeWindow = 3000;
		while (!this.mqttClient.isConnected()) {
			try {
				logger.warn("Trying to reconnect in " + timeWindow/1000 + " seconds.");
				Thread.sleep(timeWindow);
				logger.warn("Reconnecting ...");
				timeWindow *= 2;
				this.mqttClient.connect();
				this.mqttClient.subscribe(this.subTopic);
				logger.warn("Connection is restored");
			}catch(MqttException me) {
				logger.error("CollectorMQTT unable to connect", me);
			} catch (InterruptedException e) {
				logger.error("Something wrong with thread sleep!", e);
			}
		}
		
	}

	public void messageArrived(String topic, MqttMessage message) throws Exception {
		byte[] payload = message.getPayload();
		logger.info("Message arrived: " + new String(payload));
		try {
			JSONObject sensorMessage = (JSONObject) JSONValue.parseWithException(new String(payload));
			if (sensorMessage.containsKey("glucose")) {
				int timestamp = Integer.parseInt(sensorMessage.get("timestamp").toString());
				Integer value = Integer.parseInt(sensorMessage.get("glucose").toString());
				String nodeId = sensorMessage.get("node").toString();
				if(!th.checkSensorExistence("mqtt://"+nodeId)) {
					changeInterval(String.valueOf(this.samplingRate));
					th.addSensor("mqtt://"+nodeId);
				}
				th.addObservation("mqtt://"+nodeId, value, timestamp);
				int lower = 99;
				int upper = 125;
				boolean on = false;
				String reply;
				
				if (value > lower && value <= upper) {
					if(state != 1) {
						state = 1;
						reply = "y";
						publish(reply, nodeId);
                    	logger.info("[WARNING] - "+nodeId+" - the level of glucose is higher than normal!");
                    	th.updateSensorState("mqtt://"+nodeId, state);
					}
				} else if(value > upper)
				{
					if(state != 2) {
						state = 2;
						reply = "r";
						publish(reply, nodeId);
                    	logger.info("[CRITICAL] - "+nodeId+" - the level of glucose is too high!");
                    	th.updateSensorState("mqtt://"+nodeId, state);
					}
				} else {
					if(state != 0) {
						state = 0;
						reply = "g";
						publish(reply, nodeId);
                    	logger.info("[NORMAL] - "+nodeId+" - the level of glucose is normal!");
                    	th.updateSensorState("mqtt://"+nodeId, state);
					}
				}
				 
			}	
		} catch (ParseException e) {
			logger.error("Parse exception", e);
		}
	}

	public void deliveryComplete(IMqttDeliveryToken token) {
		logger.info("Delivery Completed");
		
	}

}
