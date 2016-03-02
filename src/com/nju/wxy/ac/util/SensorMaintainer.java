package com.nju.wxy.ac.util;

import java.util.ArrayList;
import java.util.HashMap;

import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;

public class SensorMaintainer {
	static HashMap<Integer,SensorEventListener> hsel;
	static HashMap<Integer,SensorListener> hsl;
	static{
		hsel=new HashMap<Integer,SensorEventListener>();
		hsl=new HashMap<Integer,SensorListener>();
		
	}
	
	public static void addOne(int tp,SensorEventListener sel){
		hsel.put(tp, sel);
	}
	public static void addOne(int tp,SensorListener sl){
		hsl.put(tp, sl);
	}	
	public static void removeOne(int tp){
		if (hsel.containsKey(tp))
			hsel.remove(tp);
		else
			hsl.remove(tp);
	}
	
	public static ArrayList<Integer> removeSomeSensorEventListener(Object obj){
		ArrayList<Integer> rs=new ArrayList<Integer>();
		for (Integer i:hsel.keySet()){
			SensorEventListener sel=hsel.get(i);
			if (sel==obj){
				rs.add(i);				
			}
		}
		for (int i:rs){
			hsel.remove(i);	//delete it from hsel
		}
		return rs;
	}
	
	public static ArrayList<Integer> removeSomeSensorListener(Object obj){
		ArrayList<Integer> rs=new ArrayList<Integer>();
		for (Integer i:hsl.keySet()){
			SensorListener sel=hsl.get(i);
			if (sel==obj){
				rs.add(i);				
			}
		}
		for (int i:rs){
			hsl.remove(i);	//delete it from hsl
		}
		return rs;			
	}
	
	
	
}
