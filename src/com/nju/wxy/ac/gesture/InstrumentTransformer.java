package com.nju.wxy.ac.gesture;

import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.Scene;

public class InstrumentTransformer extends BodyTransformer {
	public InstrumentTransformer() {
		// TODO Auto-generated constructor stub
		Scene.v().addBasicClass("com.example.asynctask.EmmaInstrumentation", 3);
		Scene.v().addBasicClass("com.example.asynctask.FinishListener", 3);
		Scene.v().addBasicClass("com.example.asynctask.InstrumentedActivity", 3);
		Scene.v().addBasicClass("com.example.asynctask.SMSInstrumentedReceiver", 3);
		Scene.v().addBasicClass("com.example.asynctask.InstrumentedActivity$CoverageCollector",3);
	}
	@Override
	protected void internalTransform(Body arg0, String arg1,
			Map<String, String> arg2) {
		// TODO Auto-generated method stub
		Scene.v().loadClassAndSupport("com.example.asynctask.EmmaInstrumentation").setApplicationClass();
		Scene.v().loadClassAndSupport("com.example.asynctask.FinishListener").setApplicationClass();
		Scene.v().loadClassAndSupport("com.example.asynctask.InstrumentedActivity").setApplicationClass();
		Scene.v().loadClassAndSupport("com.example.asynctask.SMSInstrumentedReceiver").setApplicationClass();
		
		Scene.v().loadClassAndSupport("com.example.asynctask.InstrumentedActivity$CoverageCollector").setApplicationClass();
		
		
		
	}

}
