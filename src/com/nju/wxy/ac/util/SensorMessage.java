package com.nju.wxy.ac.util;

import java.io.Serializable;

public class SensorMessage implements Serializable{
	private int ot,st;
	public SensorMessage(int ot,int st){
		this.ot=ot;
		this.st=st;
	}
	public int getOperationType(){
		return ot;
	}
	public int getSensorType(){
		return st;
	}
}
