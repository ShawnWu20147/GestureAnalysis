package com.nju.wxy.ac.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.StrictMode;

public class SensorNotifier {
	public static final int[] INDEX_SL=new int[]{8,5,2,7,1,3};
	public static final String HOST_IP="114.212.81.51";
	public static final int HOST_PORT=6384;
	public static Socket sk;
	public static ObjectOutputStream oos;
	static{
		if (android.os.Build.VERSION.SDK_INT > 9) {
		    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		    StrictMode.setThreadPolicy(policy);
		}
		//network connection
		try {
			sk=new Socket(HOST_IP, HOST_PORT);
			oos=new ObjectOutputStream(sk.getOutputStream());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void sendMsg(SensorMessage s){
		//for later
		try {
			oos.writeObject(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void register_SL(int sensors,SensorListener sl){
		int as=sensors;
		if (as>63) as=63;	//only consider 1,2,4,8,16,32 sensor
		int current=32;
		int i=0;
		while (as>=1){
			if (as>=current){
				SensorMaintainer.addOne(INDEX_SL[i], sl);
				SensorMessage sm=new SensorMessage(0, INDEX_SL[i]);
				sendMsg(sm);
				as-=current;
			}
			current/=2;
			i++;
		}
	}
	
	public static void register_SLD(int sensors,SensorListener sl,int sensor_delay){
		register_SL(sensors, sl);
	}
	
	public static void register_SELD(Sensor sensor,SensorEventListener sel,int sensor_delay){
		if (sensor==null) return;
		SensorMaintainer.addOne(sensor.getType(), sel);
		SensorMessage sm=new SensorMessage(0, sensor.getType());
		sendMsg(sm);	
	}
	
	public static void register_SELDH(Sensor sensor,SensorEventListener sel,int sensor_delay,Handler hl){
		register_SELD(sensor,sel,sensor_delay);
	}
	
	public static void unregister_SELS(Sensor sensor){
		if (sensor==null) return;
		SensorMaintainer.removeOne(sensor.getType());
		SensorMessage sm=new SensorMessage(1,sensor.getType());
		sendMsg(sm);
	}
	public static void unregister_SEL(Object obj){
		ArrayList<Integer> removed= SensorMaintainer.removeSomeSensorEventListener(obj);
		for (int i:removed){
			SensorMessage sm=new SensorMessage(1,i);
			sendMsg(sm);
		}
	}
	public static void unregister_SLS(int sensors){
		int as=sensors;
		if (as>63) as=63;	//only consider 1,2,4,8,16,32 sensor
		int current=32;
		int i=0;
		while (as>=1){
			if (as>=current){
				SensorMaintainer.removeOne(INDEX_SL[i]);
				SensorMessage sm=new SensorMessage(1, INDEX_SL[i]);
				sendMsg(sm);
				as-=current;
			}
			current/=2;
			i++;
		}
		
	}
	public static void unregister_SL(Object obj){
		ArrayList<Integer> removed= SensorMaintainer.removeSomeSensorListener(obj);
		for (int i:removed){
			SensorMessage sm=new SensorMessage(1,i);
			sendMsg(sm);
		}
	}
	
	
	
}
