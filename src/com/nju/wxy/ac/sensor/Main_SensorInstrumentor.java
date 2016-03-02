package com.nju.wxy.ac.sensor;

import java.io.File;
import java.util.ArrayList;

import soot.PackManager;
import soot.Transform;

import com.nju.wxy.ac.LoadSoot;

public class Main_SensorInstrumentor {
	public static final String []demo_args=new String[]{"wd.apk","soot.cfg"};
	public static void main(String[] args){
		String []ta=args;
		if (args.length==0)
			ta=demo_args;
		String analysis_apk=ta[0];
		String config_soot_url=ta[1];
		
		String[] config_soot=new LoadSoot(config_soot_url).getInfo();
		String []margs={
				"-android-jars",config_soot[0],
				"-src-prec","apk",
				"-f","dex",
				"-include-all",
				"-allow-phantom-refs",
				"-cp",config_soot[1]+config_soot[2]+new File("").getAbsolutePath()+";"+new File("").getAbsolutePath(),					
				"-d",".\\apk_output",
				"-process-dir",new File("").getAbsolutePath()+"\\"+analysis_apk
				
		};
		
		/*
		for (String i: margs){
			System.out.print(i+" ");
		}*/
		
		
		Transform one=new Transform("jtp.myInstrumenter0", new ApkTransformer());
		PackManager.v().getPack("jtp").add(one);
		soot.Main.main(margs);
		
		
		
		
	}
}
