package com.nju.wxy.ac.sensor;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.util.Chain;

public class ApkTransformer extends BodyTransformer {
	public static final String SENSORMAINTAINER="com.nju.wxy.ac.util.SensorMaintainer";
	public static final String SENSORMESSAGE="com.nju.wxy.ac.util.SensorMessage";
	public static final String SENSORNOTIFIER="com.nju.wxy.ac.util.SensorNotifier";
	
	SootClass maintainer,message,notifier;
	int setup;
	
	String debugInfo;
	
	public ApkTransformer(){
		Scene.v().addBasicClass(SENSORMAINTAINER, 3);
		Scene.v().addBasicClass(SENSORMESSAGE, 3);
		Scene.v().addBasicClass(SENSORNOTIFIER, 3);		
		setup=0;
		
		debugInfo="";
	}
	@Override
	protected void internalTransform(Body arg0, String arg1,
			Map<String, String> arg2) {
		if (setup==0){
			maintainer=Scene.v().loadClassAndSupport(SENSORMAINTAINER);
			message=Scene.v().loadClassAndSupport(SENSORMESSAGE);
			notifier=Scene.v().loadClassAndSupport(SENSORNOTIFIER);
			maintainer.setApplicationClass();
			message.setApplicationClass();
			notifier.setApplicationClass();
			
			instrumentation();
			setup=1;
		}
		
		//need do nothing
		return;
		

	}
	
	private void instrumentation(){
		
		
		System.out.println(notifier.getMethods());
		SootMethod reg_sl =notifier.getMethodByName("register_SL");
		SootMethod reg_sld =notifier.getMethodByName("register_SLD");
		SootMethod reg_seld =notifier.getMethodByName("register_SELD");
		SootMethod reg_seldh =notifier.getMethodByName("register_SELDH");
		
		SootMethod unreg_sels =notifier.getMethodByName("unregister_SELS");
		SootMethod unreg_sel =notifier.getMethodByName("unregister_SEL");
		SootMethod unreg_sls =notifier.getMethodByName("unregister_SLS");
		SootMethod unreg_sl =notifier.getMethodByName("unregister_SL");
		
		
		Chain<SootClass> cs=Scene.v().getApplicationClasses();
		for (SootClass sc:cs){
			if (sc.toString().equals(SENSORMAINTAINER)
				|| sc.toString().equals(SENSORMESSAGE)
				|| sc.toString().equals(SENSORNOTIFIER)
			)
				continue;
			List<SootMethod> ls=sc.getMethods();
			for (SootMethod sm:ls){
				if (!sm.hasActiveBody())
					continue;
				Body bd=sm.getActiveBody();
				if (bd.getMethod().getDeclaringClass().getName().startsWith("android")){
					continue;
				}
				
				Chain<Unit> units = bd.getUnits();  
			    Iterator<Unit> stmtIt = units.snapshotIterator();
			    while (stmtIt.hasNext()){
			    	Stmt stmt=(Stmt)stmtIt.next(); 
			    	
			    	
			    	//SensorManager sm;
			    	//sm.un
			    	if (stmt.containsInvokeExpr()){	
			    		InvokeExpr ik=stmt.getInvokeExpr();
			    		if (ik.getMethod().getName().equals("registerListener") && ik.getMethod().getDeclaringClass().toString().equals("android.hardware.SensorManager")){
			    			if (ik.getArgCount()==4){
			    				//sel,s,int,handler
			    				Value s0=ik.getArg(0);Value s1=ik.getArg(1);
			    				Value s2=ik.getArg(2);Value s3=ik.getArg(3);
			    				StaticInvokeExpr sie=Jimple.v().newStaticInvokeExpr(reg_seldh.makeRef(), s1,s0,s2,s3);
			    				InvokeStmt is=Jimple.v().newInvokeStmt(sie);
			    				units.insertAfter(is, stmt);
			    				
			    			}
			    			else if (ik.getArgCount()==2){
			    				//sl,int
			    				Value s0=ik.getArg(0);Value s1=ik.getArg(1);
			    				StaticInvokeExpr sie=Jimple.v().newStaticInvokeExpr(reg_sl.makeRef(), s1,s0);
			    				InvokeStmt is=Jimple.v().newInvokeStmt(sie);
			    				units.insertAfter(is, stmt);
			    			}
			    			else if (ik.getMethod().getParameterType(0).toString().equals("android.hardware.SensorEventListener")){
			    				//sel,s,int
			    				Value s0=ik.getArg(0);Value s1=ik.getArg(1);
			    				Value s2=ik.getArg(2);
			    				StaticInvokeExpr sie=Jimple.v().newStaticInvokeExpr(reg_seld.makeRef(), s1,s0,s2);
			    				InvokeStmt is=Jimple.v().newInvokeStmt(sie);
			    				units.insertAfter(is, stmt);
			    			}
			    			else{
			    				//must be  sl,int,int
			    				Value s0=ik.getArg(0);Value s1=ik.getArg(1);
			    				Value s2=ik.getArg(2);
			    				StaticInvokeExpr sie=Jimple.v().newStaticInvokeExpr(reg_sld.makeRef(), s1,s0,s2);
			    				InvokeStmt is=Jimple.v().newInvokeStmt(sie);
			    				units.insertAfter(is, stmt);			    				
			    			}
			    		}
			    		
			    		if (ik.getMethod().getName().equals("unregisterListener") && ik.getMethod().getDeclaringClass().toString().equals("android.hardware.SensorManager")){
			    			if (ik.getArgCount()==1){
			    				if (ik.getMethod().getParameterType(0).toString().equals("android.hardware.SensorEventListener")){
			    					//sel
			    					Value s0=ik.getArg(0);
				    				StaticInvokeExpr sie=Jimple.v().newStaticInvokeExpr(unreg_sel.makeRef(), s0);
				    				InvokeStmt is=Jimple.v().newInvokeStmt(sie);
				    				units.insertAfter(is, stmt);	
			    				}
			    				else{
			    					//sl
			    					Value s0=ik.getArg(0);
				    				StaticInvokeExpr sie=Jimple.v().newStaticInvokeExpr(unreg_sl.makeRef(), s0);
				    				InvokeStmt is=Jimple.v().newInvokeStmt(sie);
				    				units.insertAfter(is, stmt);
			    				}
			    			}
			    			
			    			else{
			    				if (ik.getMethod().getParameterType(0).toString().equals("android.hardware.SensorEventListener")){
			    					//sel,s
			    					Value s1=ik.getArg(1);
				    				StaticInvokeExpr sie=Jimple.v().newStaticInvokeExpr(unreg_sels.makeRef(), s1);
				    				InvokeStmt is=Jimple.v().newInvokeStmt(sie);
				    				units.insertAfter(is, stmt);
			    				}
			    				else{
			    					//sl,i
			    					Value s1=ik.getArg(1);
				    				StaticInvokeExpr sie=Jimple.v().newStaticInvokeExpr(unreg_sls.makeRef(), s1);
				    				InvokeStmt is=Jimple.v().newInvokeStmt(sie);
				    				units.insertAfter(is, stmt);
			    				}
			    			}
			    		}
				    		
			    	}
			    			    	
			    }
				
			}
		}
	}
	
	public String getDebug(){
		return debugInfo;
	}

}
