package com.nju.wxy.ac.gesture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import com.nju.wxy.ac.ViewResolver;





import soot.Body;
import soot.BodyTransformer;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.util.Chain;

//handle multi-touch
public class MultiPointDetector extends BodyTransformer {
	int setup;
	String debugInfo;
	
	HashMap<String,String>   view_activity;		// id -> Activity
	
	
	HashMap<String,String> uview_activity; 		// id ->Activity (id is of UView)
	
	
	HashMap<String,String> activity_ontouchevent;	//for Activity that overrides onTouchEvent that contains getPointerCount()
	
	
	HashMap<String,String> unewview_activity;	//in an Activity,  A=new UView()  here A contains multiTouch
	
	SootClass otl;
	SootClass act;
	SootClass me;
	
	
	HashSet<SootClass> all_activities;	//all the Activity
	

	HashSet<SootClass> hotl;	//for classes that implement onTouchListener
	
	HashSet<SootClass> view_otl;	// for a UView that overrides onTouchEvent
	HashSet<SootClass> activity_otl;	//for a UActivity that overrides onTouchEvent( here useless)
	
	
	HashSet<SootClass> consider_multi; //of all classes in hotl, whom are associated with multitouch
	
	HashSet<SootClass> uview_multi;	//for a UView that overrides onTouchEvent that contains getPointerCount()
	
	
	public MultiPointDetector(){
		setup=0;
		
		Scene.v().addBasicClass("android.view.View$OnTouchListener", 3);
		Scene.v().addBasicClass("android.app.Activity", 3);
		Scene.v().addBasicClass("android.view.MotionEvent", 3);
		
		view_activity=new HashMap<String,String>();
		uview_activity=new HashMap<String,String>();
		activity_ontouchevent=new HashMap<String,String>();
		
		unewview_activity=new HashMap<String,String>();
		
		
		all_activities=new HashSet<SootClass>();
		
		
		debugInfo="";
		
	}
	@Override
	protected void internalTransform(Body arg0, String arg1,
			Map<String, String> arg2) {
		// TODO Auto-generated method stub
		if (setup==0){
			setup=1;
			
			otl=Scene.v().loadClassAndSupport("android.view.View$OnTouchListener");
			act=Scene.v().loadClassAndSupport("android.app.Activity");
			me=Scene.v().loadClassAndSupport("android.view.MotionEvent");
			
			checker();
			
		}
		return;		
	}
	
	
	private void checker(){
		Chain<SootClass> c=Scene.v().getApplicationClasses();
		

		
		hotl=new HashSet<SootClass>(); //for  val.onTouchEvent(...)
		
		view_otl=new HashSet<SootClass>();	//for a super view that overrides onTouchEvent
		activity_otl=new HashSet<SootClass>(); //for a Activity that overrides onTouchEvent ,but in facr no need because we consider it before the summer
		
		
		boolean findover=false;
		while (!findover){
			findover=true;
			for (SootClass s:c){
				if (!s.isApplicationClass())
					continue;
				if (all_activities.contains(s))
					continue;
				
				if (s.hasSuperclass()){
					SootClass sp=s.getSuperclass();
					if (sp==act || all_activities.contains(sp)){
						all_activities.add(s);
						findover=false;
					}
				}
			}
		}
		//now we get all USER-DEFINED Activity
		
		
		for (SootClass s:c){
			if (s.toString().startsWith("android")) continue;
			if (s.getInterfaces().contains(otl) || ( s.hasSuperclass() && hotl.contains(s.getSuperclass()) )       ){
				hotl.add(s);		//what classes are associated with the touchevent
			}
			if(s.declaresMethodByName("onTouchEvent")){
				//consider whether it is a view or activity
				if (!s.hasSuperclass()) continue;	//must be a view
				SootClass fas=s.getSuperclass();
				int itis=0;
				while (true){
					if (fas.toString().equals("android.view.View")){
						itis=1;
						break;
					}
					if (fas.toString().equals("android.app.Activity")){
						itis=2;
						break;
					}					
					if (!fas.hasSuperclass()) break;
					fas=fas.getSuperclass();
				}
				if (itis==1)
					view_otl.add(s);
				else if (itis==2)
					activity_otl.add(s);
			}
		}
		
		//first check all "hotl" to see whether they are associated with multi-touch
		
		consider_multi=new HashSet<SootClass>();
		
		for (SootClass sa:hotl){
			SootClass cursa=sa;
			while (!cursa.declaresMethodByName("onTouch")){
				if (!cursa.hasSuperclass()) break;

				cursa=cursa.getSuperclass();
			}
			if (!cursa.declaresMethodByName("onTouch")) continue;

			//it seems the while loop will never be executed

			SootMethod sm_otl=cursa.getMethodByName("onTouch");	//care cursa VS sa
			if (sm_otl.getParameterCount()!=2){
				System.out.println("[MultiPointDetector]: ouTouch has not 2 paras in "+sm_otl.toString());
				System.exit(-1);
			}
			if (!sm_otl.hasActiveBody()){
				debugInfo+="[MultiPointDetector.Debug]: ouTouch has no activity body in "+sm_otl.toString()+"\n";
				continue;
			}
			
			Body ab=sm_otl.getActiveBody();
			Chain<Unit> cu=ab.getUnits();
			Iterator itu=cu.snapshotIterator();
			
			
			ArrayList<SootMethod> more=new ArrayList<SootMethod>();
			HashSet<SootMethod> already_s=new HashSet<SootMethod>();
			
			already_s.add(sm_otl);
			
			while (itu.hasNext()){
				Stmt st=(Stmt)itu.next();
				if (st.containsInvokeExpr()){
					InvokeExpr ie=st.getInvokeExpr();
					if (ie.getMethod().getSignature().equals("<android.view.MotionEvent: int getPointerCount()>")){
						debugInfo+="[MultiPointDetector.Debug]: find a class that may receive multi-point events ["+sa.toString()+"]\n";
						consider_multi.add(sa);
						break;
					}
					
					//we do not consider call other methods, but it would be better to consider
					//let's do it on 7.22
					
					if (ie.getMethod().getDeclaringClass().isApplicationClass() && !ie.getMethod().getDeclaringClass().toString().startsWith("android")){
						if (!already_s.contains(ie.getMethod())){
							more.add(ie.getMethod());
							already_s.add(ie.getMethod());
						}
					}
					
					
					
				}
			}
			
			//following while are added on 7.22
			while (more.size()>0){
				SootMethod cm=more.get(0);
				more.remove(0);	
				if (!cm.hasActiveBody()) continue;
				Body ocd=cm.getActiveBody();
				Chain<Unit> ocu=ocd.getUnits();
				Iterator it=ocu.snapshotIterator();
				while (it.hasNext()){
					Stmt stmt=(Stmt) it.next();
					if (stmt.containsInvokeExpr()){
						InvokeExpr ie=stmt.getInvokeExpr();
						if (ie.getMethod().getSignature().equals("<android.view.MotionEvent: int getPointerCount()>")){
							consider_multi.add(sa);
							break;
						}
						if (ie.getMethod().getDeclaringClass().isApplicationClass() && !ie.getMethod().getDeclaringClass().toString().startsWith("android")){
							if (!already_s.contains(ie.getMethod())){
								more.add(ie.getMethod());
								already_s.add(ie.getMethod());
							}
						}
					}
				}
				
			}
			
			
			
			
		}
		
		c=Scene.v().getApplicationClasses();
		for (SootClass s:c){
			assert(s.isApplicationClass());
			assert(!s.toString().startsWith("android"));


			
			
			/*
			boolean findact=false;
			SootClass fa=s;
			while (fa.hasSuperclass()){
				fa=fa.getSuperclass();
				if (fa==act){
					findact=true;
					break;
				}
			}
			if (!findact) continue;*/
			
			if (!all_activities.contains(s))
				continue;
			
			
			if (s.declaresMethodByName("onTouchEvent")){
				// we should save it to the result
				// activity override onTouchEvent
				if (!activity_ontouchevent.containsKey(s.toString())){
					activity_ontouchevent.put(s.toString(), "");
				}
				
				SootMethod sm_ote=s.getMethodByName("onTouchEvent");
				
				ArrayList<SootMethod> explorer=new ArrayList<SootMethod>();
				explorer.add(sm_ote);
				HashSet<SootMethod> already=new HashSet<SootMethod>();
				already.add(sm_ote);
				
				boolean related_to_m=false;
				
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
				    		InvokeExpr ie=st.getInvokeExpr();
				    		SootMethod sm=ie.getMethod();
				    		if (sm.getSignature().equals("<android.view.MotionEvent: int getPointerCount()>")){
								related_to_m=true;
				    			break;
							}
				    		if (sm.getDeclaringClass()==s){
			    				if (!already.contains(sm)){
				    				already.add(sm);
				    				explorer.add(sm);
				    			}
			    			}
				    	}
				    }
				    if (related_to_m)
				    	break;
					
				}
				if (related_to_m){
					//
					activity_ontouchevent.put(s.toString(), "m");
				}
			}
			
			/*
			SootClass cur=s;
			while (!cur.declaresMethodByName("onCreate")){
				cur=cur.getSuperclass();
			}
			SootMethod sm_oncreate=cur.getMethodByName("onCreate");
			ArrayList<SootMethod> explorer=new ArrayList<SootMethod>();
			explorer.add(sm_oncreate);
			HashSet<SootMethod> already=new HashSet<SootMethod>();
			already.add(sm_oncreate);
			*/
			//9.1 before we use cur, but now we directly use s
			
			ArrayList<SootMethod> explorer=new ArrayList<SootMethod>();		
			HashSet<SootMethod> already=new HashSet<SootMethod>();
			if (s.declaresMethodByName("onCreate")){
				SootMethod sm_oncreate=s.getMethodByName("onCreate");
				explorer.add(sm_oncreate);
				already.add(sm_oncreate);
			}
			for (SootMethod smm:s.getMethods()){
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
			    		InvokeExpr ie=st.getInvokeExpr();
			    		SootMethod sm=ie.getMethod();
			    		if (!sm.getName().equals("setOnTouchListener")){
			    			if (sm.getDeclaringClass()==s){
			    				if (!already.contains(sm)){
				    				already.add(sm);
				    				explorer.add(sm);
				    			}
			    			}
			    		}
			    		else{
			    			Value base=((VirtualInvokeExpr)ie).getBase();
			    			Value v=ie.getArg(0);
			    			//base is the view to register
			    			//v is the onTouchListener
			    			// baseView.setOnTouchListener(v)
			    			//we should calculate whether v is associated with the "consider_multi"
			    			
			    			if (!consider_multi.contains(Scene.v().getSootClass(v.getType().toString()))){
			    				debugInfo+="[MultiPointDetector.Debug]: the view ["+base.toString()+"] in "+csm.toString()+" may NOT use multi_touch because of the listener\n";
			    				continue;
			    				//this listener does not use multi_touch, so we do not consider it
			    			}
			    			
			    			debugInfo+="[MultiPointDetector.Debug]: the view ["+base.toString()+"] in "+csm.toString()+" may use multi_touch because of the listener\n";
		    				
			    			ViewResolver.resolve(s, csm, st, base);
			    			String rs_view=ViewResolver.getViewId();
			    			String rs_vact=ViewResolver.getViewActivity();
			    			
			    			if (!rs_view.equals("")){
			    				view_activity.put(rs_view, rs_vact);
			    			}
			    			else{
			    				debugInfo+="[MultiPointDetector.Debug]: we can not resolve the view but the **TYPE** is "+ ((VirtualInvokeExpr)ie).getBase().getType().toString()+  "\n";  				 				
			    			}
			    			
			    			
			    			
			    		}
			    	}
			    	
			    }
			}
			
		}
		
		
		//2015.8.18
		call_view_checker(view_otl);
		// the method changes uview_activity and uview_actions
		
		
		
		
	}
	
	
	
	
	private void call_view_checker(HashSet<SootClass> view_otl){
		
		uview_multi=new HashSet<SootClass>();
		
		for (SootClass s:view_otl){
			assert(s.declaresMethodByName("onTouchEvent"));
			SootMethod sm_ote=s.getMethodByName("onTouchEvent");

			ArrayList<SootMethod> explorer=new ArrayList<SootMethod>();
			explorer.add(sm_ote);
			HashSet<SootMethod> already=new HashSet<SootMethod>();
			already.add(sm_ote);
			boolean related_to_m=false;
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
			    		InvokeExpr ie=st.getInvokeExpr();
			    		SootMethod sm=ie.getMethod();
			    		if (sm.getSignature().equals("<android.view.MotionEvent: int getPointerCount()>")){
			    			related_to_m=true;
			    			uview_multi.add(s);
			    			break;
			    		}
			    		if (sm.getDeclaringClass()==s){
		    				if (!already.contains(sm)){
			    				already.add(sm);
			    				explorer.add(sm);
			    			}
		    			}
			    	}
	    		}
	    		if (related_to_m)
	    			break;
			}
			
		}
		
		Chain<SootClass> c=Scene.v().getApplicationClasses();
		for (SootClass s:c){
			SootClass cur=s;
			boolean isview=false;
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
			if (!isview) continue;
			
			//now check whether this view has sth to do with click/longclick
			
			for (SootMethod sm:s.getMethods()){
				if (!sm.hasActiveBody())
					continue;
				if (sm.isStatic())
					continue;	//we only consider this.setOnTouchListener
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
						
						if (ie.getMethod().getName().equals("setOnTouchListener")){
							Value arg=vie.getArg(0);
							if (!consider_multi.contains(Scene.v().getSootClass(arg.getType().toString()))){
								debugInfo+="[MultiPointDetector.Debug]: the UView ["+s.toString()+"] may NOT use multi_touch because of the listener\n";
			    				
							}
							else{
								debugInfo+="[MultiPointDetector.Debug]: the UView ["+s.toString()+"] may use multi_touch because of the listener\n";
			    				uview_multi.add(s);	//important
							}
							break;
				    			
						}
						else{
							//do nothing
						}
					}
				}
				
				
			}
			
			
			
		}
		
		
		
		//by now we get uview_multi  ,which is USER_DEFINED VIEW that may use multi-touch
		//uview_multi由两部分组成  一是view自己覆盖onTouchEvent 二是view自己setOnTouchListener
		
		c=Scene.v().getApplicationClasses();
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
			
			for (SootMethod csm:s.getMethods()){
				if (!csm.hasActiveBody())
					continue;
				Body bd=csm.getActiveBody();
				Chain<Unit> units = bd.getUnits(); 

				Iterator<Unit> stmtIt = units.snapshotIterator();
	    		while (stmtIt.hasNext()){
			    	Stmt st=(Stmt) stmtIt.next();
			    	if (! (st instanceof AssignStmt))
			    		continue;
			    	Value rop=((AssignStmt)st).getRightOp();
			    	if (rop instanceof NewExpr){
			    		NewExpr ne_rop=(NewExpr)rop;
			    		SootClass uview=Scene.v().getSootClass(ne_rop.getType().toString());
			    		if (uview_multi.contains(uview)){
		    				//发现了一个user-defined view 含有多点触控 不过是new出来的
		    				String uactivity=s.toString();		    				
		    				unewview_activity.put(uview.toString(), uactivity);	    				
		    			}
			    		
			    	}
			    	if (st.containsInvokeExpr()){
			    		InvokeExpr ie=st.getInvokeExpr();
			    		SootMethod sm=ie.getMethod();
			    		if (sm.getName().equals("findViewById")){
			    			Stmt nxt=(Stmt) units.getSuccOf(st);
			    			if (nxt==null || !(nxt instanceof AssignStmt)){
			    				continue;
			    			}
			    			AssignStmt anxt=(AssignStmt)nxt;
			    			Value rv=anxt.getRightOp();
			    			if (!(rv instanceof CastExpr))
			    				continue;
			    			CastExpr ce=(CastExpr)rv;
			    			SootClass uview=Scene.v().getSootClass(ce.getType().toString());
			    			if (uview_multi.contains(uview)){
			    				//发现了一个user-defined view 含有多点触控
			    				String uviewId=ie.getArg(0).toString();
			    				String uactivity=s.toString();		    				
			    				uview_activity.put(uviewId, uactivity);	    				
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
		for (String i:view_activity.keySet()){
			result+="[MultiPointDetector]: "+i+" in "+view_activity.get(i)+"\n";	
		}
		
		for (String i:uview_activity.keySet()){
			result+="[MultiPointDetector]: USER-DEFINED view "+i+" in "+uview_activity.get(i)+"\n";
		}
		
		for (String i:unewview_activity.keySet()){
			result+="[MultiPointDetector]: USER-DEFINED *NEW* view "+i+" in "+unewview_activity.get(i)+"\n";
		}		
		
		
		
		for (String i:activity_ontouchevent.keySet()){
			result+="[MultiPointDetector]: "+i+" overrides onTouchEvent";
			if (activity_ontouchevent.get(i).equals("")){
				result+=", but it may NOT use multi_touch\n";
			}
			else{
				if (!activity_ontouchevent.get(i).equals("m")){
					System.out.println("[MultiPointDetector]: some code must wrong");
					System.exit(-1);  //should never be executed
				}
				result+=", and it may use multi_touch\n";
			}
		}	
		return result;
	}
}
