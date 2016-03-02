package com.nju.wxy.ac.gesture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.util.Chain;

public class ViewPagerDetector extends BodyTransformer {
	int setup=0;
	HashMap<String,String> hm; //view to activity
	
	HashMap<String,String> unew;
	
	String debugInfo;
	
	public ViewPagerDetector(){
		hm=new HashMap<String,String>();
		unew=new HashMap<String,String>();
		
		Scene.v().addBasicClass("android.support.v4.view.ViewPager", 3);
		Scene.v().addBasicClass("android.support.v4.view.PagerAdapter", 3);
		
		debugInfo="";
	}
	@Override
	protected void internalTransform(Body arg0, String arg1,
			Map<String, String> arg2) {
		if (setup==0){
			//System.out.println("[ViewPagerDetector]: hehe");
			
			/*
			Chain<SootClass> c=Scene.v().getApplicationClasses();
			int total=0;
			for (SootClass sc:c){
				for (SootMethod sm:sc.getMethods()){
					if (sm.isDeclared()){
						System.out.println(sm);
						total++;
					}
				}
				
			}
			System.out.println(total);
			System.exit(-1);
			
			*/
			
			
			setup=1;
			checker();
		}
		return;

	}
	

	
	
	
	private void checker(){
		SootClass vp=Scene.v().loadClassAndSupport("android.support.v4.view.ViewPager");
		SootClass pa=Scene.v().loadClassAndSupport("android.support.v4.view.PagerAdapter");
		
		SootClass act=Scene.v().getSootClass("android.app.Activity");
		
		SootClass rid=Scene.v().getSootClass(Main_Detector.PACKAGE_NAME+".R$id");
		//HashMap<String,String> id_name=resolveId(rid);
		//System.exit(-1);
		
		Chain<SootClass> c=Scene.v().getApplicationClasses();
		for (SootClass s:c){
			assert(s.isApplicationClass());
			assert(!s.toString().startsWith("android"));

			
			boolean findact=false;
			SootClass fa=s;
			while (fa.hasSuperclass()){
				fa=fa.getSuperclass();
				if (fa==act){
					findact=true;
					break;
				}
			}
			if (!findact) continue;
			
			
			
			SootClass cur=s;
			
			
			
			ArrayList<SootMethod> explorer=new ArrayList<SootMethod>();		
			HashSet<SootMethod> already=new HashSet<SootMethod>();
			if (cur.declaresMethodByName("onCreate")){
				SootMethod sm_oncreate=cur.getMethodByName("onCreate");
				explorer.add(sm_oncreate);
				already.add(sm_oncreate);
			}
			for (SootMethod smm:cur.getMethods()){
				if (!smm.getName().equals("onCreate")){
					explorer.add(smm);
					already.add(smm);
				}
			}
			

			
			while (explorer.size()>0){
				SootMethod csm=explorer.get(0);
				explorer.remove(0);	
				if (!csm.hasActiveBody()) continue;
				Body bd=csm.getActiveBody();
				Chain<Unit> units = bd.getUnits(); 
				Iterator<Unit> stmtIt = units.snapshotIterator();
			    while (stmtIt.hasNext()){
			    	Stmt st=(Stmt) stmtIt.next();
			    	if (st.containsInvokeExpr()){
			    		SootClass sc_invoke=st.getInvokeExpr().getMethod().getDeclaringClass();
			    		if (sc_invoke==s){
			    			SootMethod sm_invoke=st.getInvokeExpr().getMethod();
			    			if (!already.contains(sm_invoke)){
			    				already.add(sm_invoke);
			    				explorer.add(sm_invoke);
			    			}
			    		}
			    	}
			    	if (st instanceof AssignStmt){
			    		AssignStmt as=(AssignStmt)st;
			    		Value right=as.getRightOp();
			    		
			    		if (right instanceof NewExpr){
			    			NewExpr ne=(NewExpr)right;
			    			if (ne.getType()==vp.getType()){
			    				String acname=s.toString();
			    				String vpge="a *UNKNOWN* ViewPager";
			    				unew.put(vpge, acname);
			    			}
			    		}
			    		
			    		
			    		if (right instanceof CastExpr){
			    			CastExpr ce=(CastExpr)right;
			    			if (ce.getType()==vp.getType()){
			    				// find one here
			    				Value op=ce.getOp();
			    				Stmt before=(Stmt) units.getPredOf(st);
			    				while (true){
			    					if (! (before instanceof AssignStmt)){
			    						if (before==units.getFirst()) break;
			    						before=(Stmt) units.getPredOf(before);
			    						continue;
			    					}
			    					//sure it is AssignStmt
			    					AssignStmt nas=(AssignStmt)before;
			    					Value lft=nas.getLeftOp();
			    					Value rft=nas.getRightOp();
			    					if (lft.toString()!=op.toString()){
			    						if (before==units.getFirst()) break;
			    						before=(Stmt) units.getPredOf(before);
			    						continue;
			    					}
			    					//sure  op = ...
			    					if (! (rft instanceof InvokeExpr)){
			    						System.out.println("[ViewPagerDetector.Debug]: error in getting ViewPager(not obtained by findViewById)");
			    						System.exit(-1);
			    					}
			    					SootMethod tsm=( ((InvokeExpr)rft).getMethod());
			    					
			    					
			    					//String eq="<android.view.View: android.view.View findViewById(int)>";
			    					String eq="findViewById";
			    					//above are due to  android.app.Activity and android.view.View
			    					
			    					if (tsm.getName().equals(eq)){
			    						 String r_id=(((InvokeExpr)rft).getArg(0)).toString();
			    						 hm.put(r_id, s.toString());
			    						 
			    					}
			    					else{
			    						System.out.println("[ViewPagerDetector.Debug]: error in getting ViewPager(not obtained by findViewById)");
			    						System.exit(-1);
			    					}
			    					break;
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
	
	public String outputResult(){
		String result="";
		for (String s:hm.keySet()){
			result+="[ViewPagerDetector]:"+s+" in ";
			if (hm.get(s)==null){
				result+="unknown";
			}
			else{
				result+=hm.get(s);
			}
			result+="\n";
		}
		
		for (String s:unew.keySet()){
			result+="[ViewPagerDetector]: NEW OPERATION "+s+" in ";
			if (unew.get(s)==null){
				result+="unknown";
			}
			else{
				result+=unew.get(s);
			}
			result+="\n";
		}
		
		
		return result;
	}

}
