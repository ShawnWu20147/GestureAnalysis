package com.nju.wxy.ac.gesture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.support.v4.view.GestureDetectorCompat;
import android.widget.ImageView;

import com.nju.wxy.ac.ViewResolver;








import soot.Body;
import soot.BodyTransformer;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.util.Chain;


//handle complex gestures
public class MyGestureDetector extends BodyTransformer {
	//还需要实现一套view的分析机制...
	int setup;
	SootClass sol;
	SootClass gd,gdc,sgd;
	SootClass ogl,osgl;
	SootClass otl;
	SootClass act;
	
	HashMap<String,String>   view_activity_self;		//bind id -> Activity_self
	HashMap<String,String>	 view_actions;		//bind id -> action
	
	HashMap<String,String> uview_activity_self;		//bind UView.id -> Activity
	HashMap<String,String> uview_actions;		//bind UView.id -> action
	
	HashMap<String,String> unewview_activity;	//bind UView.class -> Activity
	HashMap<String,String> unewview_actions;	//bind UView.class -> action
	
	
	HashMap<String,String> activity_ontouchevent;	//bind Activity -> gestures (if not ,then "")
	
	HashMap<SootClass,HashSet<String>> hm;  // gestureListener - > whatActions
	
	String debugInfo;
	
	public MyGestureDetector(){
		setup=0;
		
		Scene.v().addBasicClass("android.view.GestureDetector", 3);
		Scene.v().addBasicClass("android.support.v4.view.GestureDetectorCompat", 3);
		Scene.v().addBasicClass("android.view.ScaleGestureDetector",3);
		
		Scene.v().addBasicClass("android.view.GestureDetector$SimpleOnGestureListener", 3);
		Scene.v().addBasicClass("android.view.GestureDetector$OnGestureListener", 3);
		Scene.v().addBasicClass("android.view.ScaleGestureDetector$OnScaleGestureListener",3);
		
		
		Scene.v().addBasicClass("android.view.View$OnTouchListener", 3);
		Scene.v().addBasicClass("android.app.Activity", 3);
		
		view_activity_self=new HashMap<String,String>();
		view_actions=new HashMap<String,String>();
		activity_ontouchevent=new HashMap<String,String>();
		
		uview_activity_self=new HashMap<String,String>();
		uview_actions=new HashMap<String,String>();
		
		unewview_activity=new HashMap<String,String>();
		unewview_actions=new HashMap<String,String>();		
		
		
		debugInfo="";
	}
	
	
	@Override
	protected void internalTransform(Body arg0, String arg1,
			Map<String, String> arg2) {
		if (setup==0){
			setup=1;
			
			gd=Scene.v().loadClassAndSupport("android.view.GestureDetector");
			gdc=Scene.v().loadClassAndSupport("android.support.v4.view.GestureDetectorCompat");
			sgd=Scene.v().loadClassAndSupport("android.view.ScaleGestureDetector");
			
			sol=Scene.v().loadClassAndSupport("android.view.GestureDetector$SimpleOnGestureListener");
			ogl=Scene.v().loadClassAndSupport("android.view.GestureDetector$OnGestureListener");
			osgl=Scene.v().loadClassAndSupport("android.view.ScaleGestureDetector$OnScaleGestureListener");
			
			otl=Scene.v().loadClassAndSupport("android.view.View$OnTouchListener");	//if a class implements this
			
			
			act=Scene.v().loadClassAndSupport("android.app.Activity");
			
			/*
			for (SootMethod s:act.getMethods()){
				System.out.println(s.getBytecodeSignature());
			}
			System.exit(-1);
			*/
			
			
			checker();
		}
		return;

	}
	
	
	private void checker(){
		Chain<SootClass> c=Scene.v().getApplicationClasses();
		HashSet<SootClass> hg=new HashSet<SootClass>();	//gestureListener!
		
		HashSet<SootClass> hotl=new HashSet<SootClass>(); //for  val.onTouchEvent(...)
		
		HashSet<SootClass> view_otl=new HashSet<SootClass>();	//for a super view that overrides onTouchEvent
		HashSet<SootClass> activity_otl=new HashSet<SootClass>(); //for a Activity that overrides onTouchEvent ,but in fact no need because we consider it before the summer
		
		boolean changed=true;
		while (changed){
			changed=false;
			for (SootClass s:c){
				if (s.toString().startsWith("android")) continue;
				if (s.hasSuperclass() && (s.getSuperclass()==sol || hg.contains(s.getSuperclass())                      )  ){
					if (!hg.contains(s)){
						hg.add(s);
						changed=true;
					}
				}
				if (s.getInterfaces().contains(ogl)){
					if (!hg.contains(s)){
						hg.add(s);
						changed=true;
					}
				}
				if (s.getInterfaces().contains(osgl)){
					if (!hg.contains(s)){
						hg.add(s);
						changed=true;
					}
				}
				
				if (s.getInterfaces().contains(otl) ||   ( s.hasSuperclass() && hotl.contains(s.getSuperclass()) )           ){
					if (!hotl.contains(s)){
						hotl.add(s);
						changed=true;
					}
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
		}
		
		//now hg and hotl may contain classes whose super classes matter
		//view_otl is useful,  activity_otl is useless
		
		
		
		HashSet<String> keymethods=new HashSet<String>();
		keymethods.add("onSingleTapUp");
		keymethods.add("onLongPress");
		keymethods.add("onScroll");
		keymethods.add("onFling");
		keymethods.add("onShowPress");
		keymethods.add("onDown");
		keymethods.add("onDoubleTap");
		keymethods.add("onDoubleTapEvent");
		keymethods.add("onSingleTapConfirmed");
		
		keymethods.add("onScale");  //important

		
		hm=new HashMap<SootClass,HashSet<String>>();
		for (SootClass sa:hg){
			HashSet<String> mine=new HashSet<String>();
			SootClass cursa=sa;
			while (cursa.isApplicationClass()){
				for (SootMethod sm:cursa.getMethods()){
					if (!keymethods.contains(sm.getName())) continue;
					mine.add(sm.getName());
				}
				if (cursa.hasSuperclass())
					cursa=cursa.getSuperclass();
				else
					break;
			}
			hm.put(sa, mine);	// gestureListener - > whatActions  considered super class
		}
		
		/*
		for (SootClass sss:hm.keySet()){
			System.out.println(sss+"\t"+hm.get(sss));
		}
		System.exit(-1);
		*/
		
		
		HashSet<SootClass> consider_gesture=new HashSet<SootClass>();
		HashSet<SootClass> consider_scale=new HashSet<SootClass>();
		
		for (SootClass sa:hotl){
			
			SootClass cssa=sa;
			while (!cssa.declaresMethodByName("onTouch")){
				if (!cssa.hasSuperclass()) break;
				cssa=cssa.getSuperclass();
			}
			
			if (!cssa.declaresMethodByName("onTouch")) continue;
			
			SootMethod sm_otl=cssa.getMethodByName("onTouch");
			
			if (sm_otl.getParameterCount()!=2){
				System.out.println("[MyGestureDetector.Debug]: ouTouch has not 2 paras in "+sm_otl.toString());
				System.exit(-1);
			}
			if (!sm_otl.hasActiveBody()){
				System.out.println("[MyGestureDetector.Debug]: ouTouch has no activity body in "+sm_otl.toString());
				System.exit(-1);
			}
			
			
			
			Body ab=sm_otl.getActiveBody();
			Chain<Unit> cu=ab.getUnits();
			Iterator itu=cu.snapshotIterator();
			
			ArrayList<SootMethod> more=new ArrayList<SootMethod>();
			HashSet<SootMethod> already_s=new HashSet<SootMethod>();
			
			already_s.add(sm_otl);
			
			
			while (itu.hasNext()){
				Stmt st=(Stmt)itu.next();
				//System.out.println("----:"+st);
				if (st.containsInvokeExpr()){
					InvokeExpr ie=st.getInvokeExpr();
					if (ie.getMethod().getSignature().equals("<android.view.GestureDetector: boolean onTouchEvent(android.view.MotionEvent)>")){
						consider_gesture.add(sa);
						//break;
					}
					if (ie.getMethod().getSignature().equals("<android.support.v4.view.GestureDetectorCompat: boolean onTouchEvent(android.view.MotionEvent)>")){
						consider_gesture.add(sa);
						//break;
					}
					if (ie.getMethod().getSignature().equals("<android.view.ScaleGestureDetector: boolean onTouchEvent(android.view.MotionEvent)>")){
						consider_scale.add(sa);
						//break;
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
						if (ie.getMethod().getSignature().equals("<android.view.GestureDetector: boolean onTouchEvent(android.view.MotionEvent)>")){
							consider_gesture.add(sa);
							//break;
						}
						if (ie.getMethod().getSignature().equals("<android.support.v4.view.GestureDetectorCompat: boolean onTouchEvent(android.view.MotionEvent)>")){
							consider_gesture.add(sa);
							//break;
						}
						if (ie.getMethod().getSignature().equals("<android.view.ScaleGestureDetector: boolean onTouchEvent(android.view.MotionEvent)>")){
							consider_scale.add(sa);
							//break;
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
		
		for (SootClass sss:consider_gesture){
			debugInfo+="[MyGestureDetector.Debug]: find a onTouchListener that is associated with gestures :"+sss.toString()+"\n";
		}
		for (SootClass sss:consider_scale){
			debugInfo+="[MyGestureDetector.Debug]: find a onTouchListener that is associated with [Scale] operation :"+sss.toString()+"\n";
		}		
		
		//only finish htol , which knows consider_gesture and consider_scale
		//has activity_otl and view_otl to consider, but activity_otl is no longer needed
		
		
		
		//System.out.println("########"+consider_gesture.iterator().next());
		
		//consider_gesture stores the onTouchListener that may contain gestures
		//consider_scale stores the onTouchListener that may constain scale
		//to make it easy, we only calculate the views that register those listeners
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
				
				boolean related_to_g=false;
				boolean related_to_s=false;

			    
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
				    		if (sm.getSignature().equals("<android.view.GestureDetector: boolean onTouchEvent(android.view.MotionEvent)>")){
								related_to_g=true;
				    			//break;
							}
				    		if (sm.getSignature().equals("<android.support.v4.view.GestureDetectorCompat: boolean onTouchEvent(android.view.MotionEvent)>")){
								related_to_g=true;
				    			//break;
							}
				    		if (sm.getSignature().equals("<android.view.ScaleGestureDetector: boolean onTouchEvent(android.view.MotionEvent)>")){
								related_to_s=true;
				    			//break;
							}
				    		if (sm.getDeclaringClass()==s){
			    				if (!already.contains(sm)){
				    				already.add(sm);
				    				explorer.add(sm);
				    			}
			    			}
				    	}
				    }
				    if (related_to_g && related_to_s)
				    	break;
					
				}
				if (related_to_g){
					String rsa=getListener_version2(hg, s);
					if (!rsa.equals("")){
						if (related_to_s) rsa+=",Scale";
						activity_ontouchevent.put(s.toString(), rsa);
					}
					else{
						rsa=",Unknown";
						if (related_to_s) rsa+="Scale";
						activity_ontouchevent.put(s.toString(), rsa);
					}
				}
				else if (related_to_s){
					String rsa=",Scale";
					activity_ontouchevent.put(s.toString(), rsa);
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
			    		if (!(ie instanceof VirtualInvokeExpr))
			    			continue;	//no need to see since it is not "this"
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
			    			//we should first calculate whether v is associated with the "consider_gesture"
			    			
			    			if (!consider_gesture.contains(Scene.v().getSootClass(v.getType().toString())) &&  !consider_scale.contains(Scene.v().getSootClass(v.getType().toString()))){
			    				continue;
			    				//this listener does not use gestures( including scale) , so we do not consider it
			    			}
			    			
			    			debugInfo+="[MyGestureDetector.Debug]: find one view ["+base.toString()+"] that uses gesture in"+csm.toString()+"\n";
			    			
			    			
			    			
			    			ViewResolver.resolve(s, csm, st, base);
			    			String rs_view=ViewResolver.getViewId();
			    			String rs_vact=ViewResolver.getViewActivity();
			    			if (!rs_view.equals("")){
			    				view_activity_self.put(rs_view, rs_vact+"#"+base.getType().toString());
			    			}
			    			
			    			if (rs_view.equals("")){
				    			debugInfo+="[MyGestureDetector.Debug]: we can not resolve the view but the **TYPE** is "+ base.getType().toString()+  "\n";  				 							
			    				continue;
			    			}
			    			
			    			//next is to consider v to gestureListener
			    			// in onTouch must call  $r5.onTouchEvent(...) while r5 is sensitive
			    			// we should decide r5 is associated to what GestureListener?
			    			
			    			//to make it easy, we assume there is only one GestureListener for this class......
			    			//a better idea is assuming the listenering is in a field, but still difficult
			    			
			    			boolean r_g=consider_gesture.contains(Scene.v().getSootClass(v.getType().toString()));
			    			boolean r_s=consider_scale.contains(Scene.v().getSootClass(v.getType().toString()));
			    			String rsa=(r_g==true)?getListener_version2(hg,s):"";
			    			//这里一个问题是 getListener似乎应该是r_g确认后才应该调用 不过目前还是直接调用了 ok fixed
			    			if (rsa.equals("")){
			    				if (r_s && r_g){
			    					view_actions.put(rs_view, ",OnScale and some unknown gestures");
				    			}
			    				else if (r_s){
			    					view_actions.put(rs_view, ",OnScale");	
			    				}
			    				else if (r_g){
			    					view_actions.put(rs_view, ",some unknown gestures");
			    				}
			    				else{
			    					System.out.println("R_S and R_G must be wrong!");
			    					System.exit(-1);
			    				}
			    				continue;
			    			}
			    			String []all_ls=rsa.split(",");
			    			HashSet<String> overall=new HashSet<String>();
			    			if (r_s){
			    				overall.add("OnScale");
			    			}
			    			for (int i=1;i<all_ls.length;i++){
			    				SootClass tf=Scene.v().getSootClass(all_ls[i]);
			    				HashSet<String> hehe=hm.get(tf);
			    				overall.addAll(hehe);
			    			}
			    			String overall_str="";
			    			for (String ii:overall){
			    				overall_str+=","+ii;
			    			}
			    			view_actions.put(rs_view, overall_str);
			    			
			    			
			    		}
			    		
			    	}
			    }
			}

		}
		
		
		
		//2015.8.18
		call_view_checker(view_otl,hg);
		// the method changes uview_activity and uview_actions
		
		
		
		
	}
	
	private HashSet<SootClass> get_listener_from_view_version2(SootClass s,HashSet<SootClass> hg,SootClass uv){
		//version2  从Activity和UView中共同寻找gestureListener
		HashSet<SootClass> result=new HashSet<SootClass>();
		for (SootMethod sm:s.getMethods()){
			if (!sm.hasActiveBody())
				continue;
			Body bd=sm.getActiveBody();
			List<ValueBox> udb=bd.getUseAndDefBoxes();
			for (ValueBox vb:udb){
				Type tp=vb.getValue().getType();
				if (hg.contains(Scene.v().getSootClass(tp.toString()))){
					//System.out.println("11223344"+tp.toString());
					result.add(Scene.v().getSootClass(tp.toString()));
				}
			}
		}
		for (SootMethod sm:uv.getMethods()){
			if (!sm.hasActiveBody())
				continue;
			Body bd=sm.getActiveBody();
			List<ValueBox> udb=bd.getUseAndDefBoxes();
			for (ValueBox vb:udb){
				Type tp=vb.getValue().getType();
				if (hg.contains(Scene.v().getSootClass(tp.toString()))){
					//System.out.println("11223344"+tp.toString());
					result.add(Scene.v().getSootClass(tp.toString()));
				}
			}
		}		
		
		return result;
		
		
		
	}
	
	

	private HashSet<SootClass> get_listener_from_view(SootClass s,HashSet<SootClass> hg){
		//只考虑listener 不再考虑detector这里了
		//s是 某个onTouchListener hg就是所有GestureListener
		//是否考虑增加考虑  view本身里面是否可能有listener?
		
		ArrayList<SootMethod> explorer=new ArrayList<SootMethod>();
		HashSet<SootMethod> already=new HashSet<SootMethod>();
		
		HashSet<SootClass> result=new HashSet<SootClass>();
		
		for (SootMethod sm:s.getMethods()){
			explorer.add(sm);
			already.add(sm);
		}
		
		while (explorer.size()>0){
			SootMethod csm=explorer.get(0);
			explorer.remove(0);	
			if (!csm.hasActiveBody()) continue;
			Body bd=csm.getActiveBody();
			Chain<Unit> units = bd.getUnits(); 
			Iterator<Unit> stmtIt = units.snapshotIterator();
			while (stmtIt.hasNext()){
				Stmt stmt=(Stmt) stmtIt.next();
				
				if (! (stmt instanceof AssignStmt))
		    		continue;
				
		    	AssignStmt as=(AssignStmt)stmt;
		    	if (as.getRightOp() instanceof NewExpr){
		    		NewExpr ne=(NewExpr) (as.getRightOp());
		    		Type tp=ne.getType();
		    		SootClass ts=Scene.v().getSootClass(tp.toString());
		    		//compare it with hg GestureListener
		    		boolean isit=false;
		    		while (true){
		    			if (hg.contains(ts)){
		    				isit=true;
		    				break;
		    			}
		    			if (!ts.hasSuperclass()) break;
		    			ts=ts.getSuperclass();
		    		}
		    		if (isit){
		    			result.add(ts);
		    		}
		    	}
			}	
		}
		return result;
	}
	
	
	private void calculate_related_gs(SootMethod start,HashMap<SootClass,String>uview_action,SootClass s,HashSet<SootClass>hg,SootClass uv){
		//this method starts from a method [start] to calculate the gestures associated with it
		//uv is the user-defined View class, s is the onTouchListener class
		//start is the method "onTouch" of s
		ArrayList<SootMethod> explorer=new ArrayList<SootMethod>();
		explorer.add(start);
		HashSet<SootMethod> already=new HashSet<SootMethod>();
		already.add(start);
		
		boolean related_to_g=false;
		boolean related_to_s=false;

	    
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
		    		if (sm.getSignature().equals("<android.view.GestureDetector: boolean onTouchEvent(android.view.MotionEvent)>")){
						related_to_g=true;
		    			//break;
					}
		    		if (sm.getSignature().equals("<android.support.v4.view.GestureDetectorCompat: boolean onTouchEvent(android.view.MotionEvent)>")){
						related_to_g=true;
		    			//break;
					}
		    		if (sm.getSignature().equals("<android.view.ScaleGestureDetector: boolean onTouchEvent(android.view.MotionEvent)>")){
						related_to_s=true;
		    			//break;
					}
		    		if (sm.getDeclaringClass()==s){
	    				if (!already.contains(sm)){
		    				already.add(sm);
		    				explorer.add(sm);
		    			}
	    			}
		    	}
		    }
		    if (related_to_g && related_to_s)
		    	break;
		}
		if (!related_to_g && !related_to_s)
			return;	//this user-defined view is useless 
		if (related_to_g && related_to_s){
			HashSet<SootClass> hglfv=get_listener_from_view_version2(s,hg,uv);
			HashSet<String> overall=new HashSet<String>();
			overall.add("onScale");
			for (SootClass ss:hglfv){
				HashSet<String> one=hm.get(ss);
				overall.addAll(one);
			}
			if (overall.size()==1){
				overall.add("some unknown gestures");
			}
			String rsa="";
			for (String str:overall){
				rsa+=","+str;
			}
			uview_action.put(uv,rsa);		// rsa may be "" !!!
			
		}
		else if (related_to_g && !related_to_s){
			HashSet<SootClass> hglfv=get_listener_from_view_version2(s,hg,uv);
			HashSet<String> overall=new HashSet<String>();
			for (SootClass ss:hglfv){
				HashSet<String> one=hm.get(ss);
				overall.addAll(one);
			}
			if (overall.size()==0){
				overall.add("some unknown gestures");
			}
			String rsa="";
			for (String str:overall){
				rsa+=","+str;
			}
			uview_action.put(uv,rsa);	//rsa may be ""  !!
		}
		else if (!related_to_g && related_to_s){
			uview_action.put(uv, ",onScale");
		}
		
		
	}
	

	private void call_view_checker(HashSet<SootClass> view_otl,HashSet<SootClass> hg) {
		//首先检测onTouchEvent是否与手势有关  3个调用
		//其次看看能不能导出对应的手势是什么  get_listener_from_view
		//第三看看这些view出现在哪里
		
		HashMap<SootClass,String> uview_action=new HashMap<SootClass,String>();  //temp val  bind UView.class -> actions
		
		for (SootClass s:view_otl){
			assert(s.declaresMethodByName("onTouchEvent"));
			SootMethod sm_ote=s.getMethodByName("onTouchEvent");
			calculate_related_gs(sm_ote, uview_action, s, hg,s);
		}
		
		
		
		//here consider view  this.setOnTouchListener
		Chain<SootClass> c=Scene.v().getApplicationClasses();
		for (SootClass s:c){
			if (uview_action.containsKey(s))
				continue;
			SootClass cur=s;
			boolean itis=false;
			while (true){
				if (cur.toString().equals("android.view.View")){
					itis=true;
					break;
				}
				if (cur.hasSuperclass())
					cur=cur.getSuperclass();
				else
					break;
			}
			if (!itis)
				continue;
			
			//now we are sure it is a view
			for (SootMethod sm:s.getMethods()){
				if (!sm.hasActiveBody())
					continue;
				
				Body bd=sm.getActiveBody();
				Iterator<Unit> itu=bd.getUnits().snapshotIterator();
				while (itu.hasNext()){
					Stmt st=(Stmt)(itu.next());
					if (st.containsInvokeExpr()){
						InvokeExpr ie=st.getInvokeExpr();
						if (ie instanceof VirtualInvokeExpr){
							Value v=((VirtualInvokeExpr)ie).getBase();
							if (v.toString().equals("$r0") && ie.getMethod().getName().equals("setOnTouchListener")){
								//to see the listener
								Value ls=ie.getArg(0);
								Type tp=ls.getType();
								
								if (tp.toString().equals("null_type")) continue;
								
								SootClass ttp=Scene.v().getSootClass(tp.toString());
								assert(ttp.declaresMethodByName("onTouch"));
								
								
								if (!ttp.declaresMethodByName("onTouch")) continue;
								
								SootMethod start=ttp.getMethodByName("onTouch");
								calculate_related_gs(start, uview_action, ttp, hg,s);
								break;
								
								
								
							}
						}
					}
					
				}
				
			}
		}
		
		
		
		
		
		
		
		
		
		
		
		for (SootClass ds:uview_action.keySet()){
			debugInfo+="[MyGestureDetector.Debug]: UView ["+ds.toString()+"] contains gestures: "+uview_action.get(ds).substring(1)+"\n";
					
		}
		
		
		//by now we get UView_Action (User-defined view class  <-> Specific actions like onFling,onScale etc.)
		
		for (SootClass s:c){
			if (!s.isApplicationClass())
				continue;
			
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
			    		if (uview_action.containsKey(uview)){
		    				//发现了一个user-defined view 含有gesture操作 不过是new出来的
		    				String uactivity=s.toString();		    				
		    				unewview_activity.put(uview.toString(), uactivity);	 
		    				String uaction=uview_action.get(uview).substring(1);
		    				unewview_actions.put(uview.toString(), uaction);
		    				
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
			    			if (uview_action.containsKey(uview)){
			    				//发现了一个user-defined view 含有手势操作
			    				String uviewId=ie.getArg(0).toString();
			    				String uactivity=s.toString();
			    				String uaction=uview_action.get(uview).substring(1);
			    				
			    				uview_activity_self.put(uviewId, uactivity+"#"+uview.toString());
			    				uview_actions.put(uviewId, uaction);
			    				
			    				
			    			}
			    			
			    		}
			    	}
	    		}
			}
		}
		
	}

	private String getListener_version2(HashSet<SootClass> hg,SootClass cur){
		//cur is a Activity
		//find all the gestureListener from the class by  USE and DEF 
		String lst="";
		HashSet<SootClass> gesture_listener_set=new HashSet<SootClass>();
		for (SootMethod sm:cur.getMethods()){
			if (!sm.hasActiveBody())
				continue;
			Body bd=sm.getActiveBody();
			List<ValueBox> udb=bd.getUseAndDefBoxes();
			for (ValueBox vb:udb){
				Type tp=vb.getValue().getType();
				if (hg.contains(Scene.v().getSootClass(tp.toString()))){
					//System.out.println("55443322"+tp.toString());
					gesture_listener_set.add(Scene.v().getSootClass(tp.toString()));
				}
			}
		}
		for (SootClass s:gesture_listener_set){
			lst+=","+s.toString();
		}
		return lst;
	}

	private String getListener(HashSet<SootClass> hg,SootClass cur){
		//为什么这里要考虑GestureDetector? 而且只考虑了gd 还有2个并没有考虑呢
		String lst="";
		
		/*
		SootMethod sm_oncreate=cur.getMethodByName("onCreate");
		ArrayList<SootMethod> explorer=new ArrayList<SootMethod>();
		explorer.add(sm_oncreate);
		HashSet<SootMethod> already=new HashSet<SootMethod>();
		already.add(sm_oncreate);
		*/
		
		
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
		
		
		int new_listener=0;
		int new_detector=0;
		
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
		    		if (sc_invoke==cur){
		    			SootMethod sm_invoke=st.getInvokeExpr().getMethod();
		    			if (!already.contains(sm_invoke)){
		    				already.add(sm_invoke);
		    				explorer.add(sm_invoke);
		    			}
		    		}
		    	}
		    	
		    	if (! (st instanceof AssignStmt))
		    		continue;
		    	AssignStmt as=(AssignStmt)st;
		    	if (as.getRightOp() instanceof NewExpr){
		    		NewExpr ne=(NewExpr) (as.getRightOp());
		    		Type tp=ne.getType();
		    		SootClass ts=Scene.v().getSootClass(tp.toString());
		    		//compare it with hg GestureListener
		    		boolean isit=false;
		    		while (true){
		    			if (hg.contains(ts)){
		    				isit=true;
		    				break;
		    			}
		    			if (!ts.hasSuperclass()) break;
		    			ts=ts.getSuperclass();
		    		}
		    		if (isit){
		    			new_listener+=1;
		    			lst+=","+tp.toString();
		    		}
		    		
		    		ts=Scene.v().getSootClass(tp.toString());
		    		//compare it with gd GestureDetector
		    		isit=false;
		    		while (true){
		    			if (gd==ts){
		    				isit=true;
		    				break;
		    			}
		    			if (!ts.hasSuperclass()) break;
		    			ts=ts.getSuperclass();
		    		}
		    		if (isit) new_detector+=1;
		    		
		    		
		    	}
		    	
		    }
		}
		if (new_detector*new_listener==0){
			debugInfo+="[MyGestureDetector.Debug]: no Detector or Listener, but we still give the result\n";
		}
		
		return lst;
	}
	
	public String getDebug(){
		return debugInfo;
	}
	public String outputResult(){
		String result="";
		for (String i:view_activity_self.keySet()){
			String []activity_tp=view_activity_self.get(i).split("#");
			result+="[MyGestureDetector]: "+i+"["+activity_tp[1]+"] in "+activity_tp[0]+"\n";
			if (view_actions.containsKey(i)){
				String rrr=view_actions.get(i).substring(1);
				result+="[MyGestureDetector]: this view contains the following actions :"+rrr+"\n";
			}
			else{
				System.out.println("View_action does not contaion "+i+" , there must be something wrong!");
				System.exit(-1);
				result+="[MyGestureDetector]: we cannot decide the specific actions associated with the view\n";
			}
		}
		
		
		for (String i:uview_activity_self.keySet()){
			String []activity_tp=uview_activity_self.get(i).split("#");
			
			result+="[MyGestureDetector]: "+i+"["+activity_tp[1]+"] in "+activity_tp[0]+"\n";
			String info=uview_actions.get(i);
			result+="[MyGestureDetector]: this USER-DEFINED view contains the following actions :"+info+"\n";
		}
		
		for (String i:unewview_activity.keySet()){
			result+="[MyGestureDetector]: ["+i+"] in "+unewview_activity.get(i)+"\n";
			//String info=uview_actions.get(i);	//fuck!!! I made a big mistake
			String info=unewview_actions.get(i);
			result+="[MyGestureDetector]: this USER-DEFINED *NEW* view contains the following actions :"+info+"\n";
		}		
		
		
		
		for (String i:activity_ontouchevent.keySet()){
			result+="[MyGestureDetector]: the Activity ["+i+"] overrides onTouchEvent";
			if (!activity_ontouchevent.get(i).equals("")){
				result+=", and it is associated with gestures: ";
				
				String rsa=activity_ontouchevent.get(i);
				String []all_ls=rsa.split(",");
    			HashSet<String> overall=new HashSet<String>();
    			for (int ii=1;ii<all_ls.length;ii++){
    				String css=all_ls[ii];
    				if (css.equals("Scale")){
    					overall.add("OnScale");
    					continue;
    				}
    				else if (css.equals("UnknownScale")){
    					overall.add("OnScale");
    					overall.add("UnknownOperations");
    					continue;
    				}
    				else if (css.equals("Unknown")){
    					overall.add("UnknownOperations");
    					continue;
    				}
    				SootClass tf=Scene.v().getSootClass(all_ls[ii]);
    				HashSet<String> hehe=hm.get(tf);
    				overall.addAll(hehe);
    			}
    			String overall_str="";
    			for (String ii:overall){
    				overall_str+=","+ii;
    			}
    			result+=overall_str.substring(1);
				
			}
			else{
				result+=", but we cannot decide whether it is related to gestures";	
			}
			result+="\n";
		}
		return result;
	}

}
