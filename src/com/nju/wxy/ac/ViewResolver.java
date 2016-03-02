package com.nju.wxy.ac;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import soot.Body;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.util.Chain;

public class ViewResolver {
	public static String viewId;
	public static String viewActivity;
	public static String debugInfo;
	
	public static void resolve(SootClass sc,SootMethod sm,Stmt cur,Value v){
		
		debugInfo="";
		
		viewId="";
		viewActivity=sc.toString();
		
		Body b=sm.getActiveBody();
		Chain<Unit> u=b.getUnits();
		
		Stmt bf=(Stmt) u.getPredOf(cur);
		
		//String vname=v.toString();
		HashSet<String> vname1=new HashSet<String>();
		vname1.add(v.toString());
		
		
		String itisfield="";
		int staticfield=0;
		//System.out.println("=====:start");
		while (true){
			//System.out.println("===:"+bf);
			if (! (bf instanceof AssignStmt)){
				if (bf==u.getFirst()) break;
				bf=(Stmt) u.getPredOf(bf);
				continue;
			}
			AssignStmt as=(AssignStmt)bf;
			Value l=as.getLeftOp();
			
			
			
			if (!(   
					
					(vname1.contains(l.toString())) 
							||  	
					((l instanceof InstanceFieldRef) &&   
					((InstanceFieldRef)l).getBase().toString().equals("$r0") && 
					vname1.contains(((InstanceFieldRef)l).getField().getName())   )
					  		||
					((l instanceof StaticFieldRef) &&  
					((StaticFieldRef)l).getType()==sc.getType() &&	
					vname1.contains(((StaticFieldRef)l).getField().getName())    )
					
				)
				){
					if (bf==u.getFirst()) break;
					bf=(Stmt) u.getPredOf(bf);
					continue;
			}
			
			if (l instanceof InstanceFieldRef){
				InstanceFieldRef ifr=(InstanceFieldRef)l;
				if (!ifr.getBase().toString().equals("$r0") || 
				  !vname1.contains(ifr.getField().getName())   ){
					System.out.println("[ViewResolver]: code wrong before!");
					System.exit(-1);  	//it should be executed if the coding is right
				}
				Value r=as.getRightOp();
				vname1.add(r.toString());
				
				if (bf==u.getFirst()){
					break;
				}
				bf=(Stmt) u.getPredOf(bf);
				continue;
			}
			
			if (l instanceof StaticFieldRef){
				StaticFieldRef sfr=(StaticFieldRef)l;
				if (!vname1.contains(sfr.getField().getName()) ){
					System.out.println("[ViewResolver]: code wrong before!");
					System.exit(-1);  	//it should be executed if the coding is right
				}
				Value r=as.getRightOp();
				vname1.add(r.toString());
				
				if (bf==u.getFirst()){
					break;
				}
				bf=(Stmt) u.getPredOf(bf);
				continue;
				
			}
			
			
			
			
			
			Value r=as.getRightOp();
			if (as.containsInvokeExpr() && as.getInvokeExpr().getMethod().getName().equals("findViewById")){
				//here is the result!
				viewId=as.getInvokeExpr().getArg(0).toString();
				return;
			}
			if (r instanceof CastExpr){
				CastExpr ce=(CastExpr)r;
				vname1.add(ce.getOp().toString()); 	//important
				if (bf==u.getFirst()){
					System.out.println("[ViewResolver]: wrong in resolve "+v+" of "+cur+" of "+sm.toString()+" in "+sc.toString());
					System.exit(-1);	//code review
				}
				bf=(Stmt) u.getPredOf(bf);
				continue;
			}
			
			if (r instanceof InstanceFieldRef){
				// view=  $r0.field
				InstanceFieldRef ifr=(InstanceFieldRef)r;
				Value class_instance=ifr.getBase();
				SootField class_field=ifr.getField();
				if (!class_instance.toString().equals("$r0")){
					System.out.println("[ViewResolver]: wrong in getting field when resolving "+v+" of "+cur+" of "+sm.toString()+" in "+sc.toString()+" ## not $r0");
					System.exit(-1);	//code review, acceptable
				}
				
				vname1.add(class_field.getName());	
				itisfield=class_field.getName();
				
				if (bf==u.getFirst()){
					break;
				}
				bf=(Stmt) u.getPredOf(bf);
				continue;			
			}
			
			if (r instanceof StaticFieldRef){
				// view=  class.field
				StaticFieldRef sfr=(StaticFieldRef)r;
				SootField class_field=sfr.getField();

				if (!(class_field.getDeclaringClass()==sc)){
					System.out.println("[ViewResolver]: wrong in getting field when resolving "+v+" of "+cur+" of "+sm.toString()+" in "+sc.toString()+" ## not this_class");
					System.exit(-1);	//code review, acceptable
				}
				vname1.add(class_field.getName());	
				itisfield=class_field.getName();
				staticfield=1;
				
				if (bf==u.getFirst()){
					break;
				}
				bf=(Stmt) u.getPredOf(bf);
				continue;		
			}
			
			if (bf==u.getFirst()) break;
			bf=(Stmt) u.getPredOf(bf);
			continue;
			
		}
		
		//System.out.println("====FIELD:"+itisfield);
		
		if (itisfield.equals("") && sm.getName().equals("onCreate")){
			// not a field, and in a strange method
			debugInfo+="[ViewResolver]: strange that registers onTouchEvent but cannot get it. Abandon.";
			return;
		}
		if (sm.getName().equals("onCreate")){
			debugInfo+="[ViewResolver]: even in onCreate we cannot detect it";
			return;
		}
		if (itisfield.equals("")){
			debugInfo+="[ViewResolver]: not a field and we cannot handle it";
			return;
		}
		// sure itisfield not null  and  sm not onCreate
		if (!sc.declaresMethodByName("onCreate") || !sc.getMethodByName("onCreate").hasActiveBody()){
			debugInfo+="[ViewResolver]: does not have the onCreate/activityBody method ";
			return;
		}
		// already knows  $r4= (this/thisclass).somefield  and  $r4 is sensitive
		// we should find what "somefield" is,  that is to find   (this/thisclass).somefield=?
		SootMethod oc=sc.getMethodByName("onCreate");
		
		ArrayList<SootMethod> explorer=new ArrayList<SootMethod>();
		explorer.add(oc);
		HashSet<SootMethod> already=new HashSet<SootMethod>();
		already.add(oc);
		while (explorer.size()>0){
			SootMethod cm=explorer.get(0);
			explorer.remove(0);	
			if (!cm.hasActiveBody()) continue;
			//System.out.println("current Looking:"+cm.getActiveBody());
			Body ocd=cm.getActiveBody();
			Chain<Unit> ocu=ocd.getUnits();
			Iterator it=ocu.snapshotIterator();
			while (it.hasNext()){
				Stmt stmt=(Stmt) it.next();
				if (stmt.containsInvokeExpr()){
		    		SootClass sc_invoke=stmt.getInvokeExpr().getMethod().getDeclaringClass();
		    		if (sc_invoke==sc){
		    			SootMethod sm_invoke=stmt.getInvokeExpr().getMethod();
		    			if (!already.contains(sm_invoke)){
		    				already.add(sm_invoke);
		    				explorer.add(sm_invoke);
		    			}
		    		}
		    	}
				//System.out.println("----"+stmt);
				if (!(stmt instanceof AssignStmt) || !(stmt.containsFieldRef()))
					continue;
				
				//System.out.println("HERE");
				if ( ((AssignStmt)stmt).getRightOp() instanceof FieldRef)
					continue;		//we only consider (this/thisclass).somefield= $r4  so right is wrong!
				FieldRef fr=stmt.getFieldRef();
				
				if (!((fr instanceof InstanceFieldRef) || (fr instanceof StaticFieldRef)))
					continue;
				
				
				if (fr instanceof InstanceFieldRef && staticfield==0){
					InstanceFieldRef ifr=(InstanceFieldRef)fr;
					Value bs=ifr.getBase();
					SootField df=ifr.getField();
					//System.out.println(bs+" "+df.getName()+" "+itisfield);
					if (!bs.toString().equals("$r0") || !df.getName().equals(itisfield))
						continue;
					
					
					
					// this.somefield= ...
					nextStep(stmt,ocu);
					break;
				}
				if (fr instanceof StaticFieldRef && staticfield==1){
					StaticFieldRef sfr=(StaticFieldRef)fr;
					Type tp=sfr.getType();
					SootField df=sfr.getField();
					if (!(sfr.getField().getDeclaringClass()==sc) || !df.getName().equals(itisfield))
						continue;
					
					// thisclass.somefield= ...
					nextStep(stmt,ocu);
					break;
				}			
				
			}
			
			
			
		}
		
		
		
			
		
		
	}
	
	private static void nextStep(Stmt st,Chain<Unit> u){
		// assume in the same method with somefield=...  we can decide the id
		String monitor=((AssignStmt)st).getRightOp().toString();	//this.somefield = here!
		Stmt stmt=st;
		//System.out.println("montitor+stmt:"+monitor+" "+stmt);
		while (true){
			if (stmt==u.getFirst()){
				System.out.println("[ViewResolver]: wrong in nextStep");
				System.exit(-1);
			}
			stmt=(Stmt) u.getPredOf(stmt);
			if (!(stmt instanceof AssignStmt) ){
				continue;
			}
			AssignStmt as=(AssignStmt)stmt;
			Value lft=as.getLeftOp();
			Value rft=as.getRightOp();
			if (!(lft.toString().equals(monitor)))
					continue;
			if (rft instanceof CastExpr){
				CastExpr ce=(CastExpr)rft;
				monitor=ce.getOp().toString();
				continue;
			}
			if ( (rft instanceof InvokeExpr) &&  ((InvokeExpr)rft).getMethod().getName().equals("findViewById")   ){
				viewId=((InvokeExpr)rft).getArg(0).toString();
				return;
			}
			debugInfo+="[ViewResolver]: may not succeed in nextStep";
			
			
		}
	}
	public static String getDebug(){
		return debugInfo;
	}
	public static String getViewId(){
		return viewId;
	}
	
	public static String getViewActivity(){
		return viewActivity;
	}

}
