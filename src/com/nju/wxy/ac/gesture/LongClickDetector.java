package com.nju.wxy.ac.gesture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import android.widget.Button;

import com.nju.wxy.ac.ViewResolver;

import soot.Body;
import soot.BodyTransformer;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.util.Chain;


//detect context menu -- long click
public class LongClickDetector extends BodyTransformer {
	int setup;
	
	SootClass act;
	
	HashMap<String,String> view_activity_long;
	
	HashMap<String,String> view_activity_short;
	
	HashSet<String> activity_cm;
	
	
	HashSet<SootClass> uview_click;
	HashSet<SootClass> uview_lclick;
	
	String debugInfo;
	
	public LongClickDetector(){
		setup=0;
		
		Scene.v().addBasicClass("android.app.Activity", 3);
		Scene.v().addBasicClass("android.view.View$OnCreateContextMenuListener",3);
		
		view_activity_long=new HashMap<String,String>();
		view_activity_short=new HashMap<String,String>();
		activity_cm=new HashSet<String>();
		
		
		uview_click=new HashSet<SootClass>();
		uview_lclick=new HashSet<SootClass>();
		
		debugInfo="";
	}
	@Override
	protected void internalTransform(Body arg0, String arg1,
			Map<String, String> arg2) {
		// TODO Auto-generated method stub
		if (setup==0){
			setup=1;
			
			checker();
		}
		return;
	}
	
	private void checker(){
		act=Scene.v().loadClassAndSupport("android.app.Activity");
		Chain<SootClass> c=Scene.v().getApplicationClasses();
		
		HashSet<SootClass> all_act=new HashSet<SootClass>();
		for (SootClass s:c){
			if (s.toString().startsWith("android"))
				continue;
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
			all_act.add(s);
		}
		
		for (SootClass i:all_act){
			ArrayList<SootMethod> explorer=new ArrayList<SootMethod>();		
			HashSet<SootMethod> already=new HashSet<SootMethod>();
			if (i.declaresMethodByName("onCreate")){
				SootMethod sm_oncreate=i.getMethodByName("onCreate");
				explorer.add(sm_oncreate);
				already.add(sm_oncreate);
			}
			for (SootMethod smm:i.getMethods()){
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
			    		if (sc_invoke==i){
			    			SootMethod sm_invoke=st.getInvokeExpr().getMethod();
			    			if (!already.contains(sm_invoke)){
			    				already.add(sm_invoke);
			    				explorer.add(sm_invoke);
			    			}
			    		}
			    		
			    		if (st.getInvokeExpr().getMethod().getName().equals("setOnLongClickListener")){
			    			//important
			    			InvokeExpr ie=st.getInvokeExpr();
			    			debugInfo+="[LongClickDetector.Debug]: find one view that registers LongClick in"+csm.toString()+"\n";
			    			Value base=((VirtualInvokeExpr)ie).getBase();
			    			Value v=ie.getArg(0);
			    			
			    			
			    			ViewResolver.resolve(i, csm, st, base);
			    			String rs_view=ViewResolver.getViewId();
				    		String rs_vact=ViewResolver.getViewActivity();
				    		if (!rs_view.equals("")){
				    			view_activity_long.put(rs_view, rs_vact);
				    		}
				    		if (rs_view.equals("")){
					    		debugInfo+="[LongClickDetector.Debug]: we can not resolve the view but the **TYPE** is "+ ((VirtualInvokeExpr)ie).getBase().getType().toString()+  "\n";  				
				    			continue;
				    		}			    				
			    			
			    		}
			    		
			    		
			    		else if (st.getInvokeExpr().getMethod().getName().equals("setOnClickListener")){
			    			//important
			    			InvokeExpr ie=st.getInvokeExpr();
			    			debugInfo+="[LongClickDetector.Debug]: find one view that registers Click in"+csm.toString()+"\n";
			    			Value base=((VirtualInvokeExpr)ie).getBase();
			    			Value v=ie.getArg(0);
			    			
			    			
			    			ViewResolver.resolve(i, csm, st, base);
			    			String rs_view=ViewResolver.getViewId();
				    		String rs_vact=ViewResolver.getViewActivity();
				    		if (!rs_view.equals("")){
				    			view_activity_short.put(rs_view, rs_vact);
				    		}
				    		if (rs_view.equals("")){
					    		debugInfo+="[LongClickDetector.Debug]: we can not resolve the view but the **TYPE** is "+ ((VirtualInvokeExpr)ie).getBase().getType().toString()+  "\n";  				 				
				    			continue;
				    		}			    				
			    			
			    		}
			    		
			    	}
			    }
			}			
		}	
		
		check_user_view();
	}
	
	private void check_user_view(){
		Chain<SootClass> c=Scene.v().getApplicationClasses();
		for (SootClass s:c){
			boolean isview=false;
			SootClass cur=s;
			while (true){
				if (cur.toString().equals("android.view.View")){
					isview=true;
					break;
				}
				if (cur.hasSuperclass())
					cur=cur.getSuperclass();
				else
					break;
			}
			if (!isview)
				continue;
			
			//now check whether this view has sth to do with click/longclick
			
			for (SootMethod sm:s.getMethods()){
				if (!sm.hasActiveBody())
					continue;
				if (sm.isStatic())
					continue;	//we only consider this.setOnLongClick this.setOnClick
				Body bd=sm.getActiveBody();
				Iterator<Unit> it=bd.getUnits().snapshotIterator();
				while (it.hasNext()){
					Stmt st=(Stmt) it.next();
					if (st.containsInvokeExpr()){
						InvokeExpr ie=st.getInvokeExpr();
						if (!(ie instanceof VirtualInvokeExpr))
							continue;	//not this
						VirtualInvokeExpr vie=(VirtualInvokeExpr)ie;
						Value base=vie.getBase();
						if (!base.toString().equals("$r0"))		//must this pointer
							continue;
						
						if (ie.getMethod().getName().equals("setOnLongClickListener")){
							uview_lclick.add(s);
						}
						else if (ie.getMethod().getName().equals("setOnClickListener")){
							uview_click.add(s);
						}
						else{
							//do nothing
						}
					}
				}
				if (uview_click.contains(s) && uview_lclick.contains(s))
					break;
				
			}
			
			
		}
		
		//now we get uview_click and uview_lclick
		//we can just send the result to the User
		
	}
	
	public String getDebug(){
		return debugInfo;
	}
	
	
	
	
	public String outputResult(){
		String result="";
		for (String i:view_activity_long.keySet()){
			result+="[LongClickDetector]: "+i+" in "+view_activity_long.get(i)+" registers LONGClick\n";
		}
		for (String i:view_activity_short.keySet()){
			result+="[LongClickDetector]: "+i+" in "+view_activity_short.get(i)+" registers Click\n";
		}	
		
		for (SootClass i:uview_lclick){
			result+="[LongClickDetector]: USER-DEFINIED View ["+i.toString()+"] registers LONGClick\n";
		}
		
		for (SootClass i:uview_click){
			result+="[LongClickDetector]: USER-DEFINIED View ["+i.toString()+"] registers Click\n";
		}	
		

		return result;
	}

}
