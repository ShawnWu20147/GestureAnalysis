 package com.nju.wxy.ac.gesture;

import java.io.File;


import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParser;

import soot.PackManager;
import soot.Transform;







import android.content.res.AXmlResourceParser;

import com.nju.wxy.ac.LoadSoot;


public class Main_Detector {
	public static final String []demo_args=new String[]{new File("").getAbsolutePath()+"\\"+"ch09-jumper.apk","soot.cfg"};
	public static String PACKAGE_NAME="com.test.guide";
	
	public static MyGestureDetector mgd;
	public static ViewPagerDetector vpd;
	public static MultiPointDetector mpd;
	public static LongClickDetector lcd;
	
	public static void main(String[] args){
		/*
		String A="'w' or '2'='2'";
		A="\'w' or '2'='2\'";
		System.out.println(A);
		System.exit(-1);
		*/
		
		String []ta=args;
		if (args.length==0)
			ta=demo_args;
		String analysis_apk=ta[0];
		String config_soot_url=ta[1];
		
		
		PACKAGE_NAME=getPackageName(analysis_apk);
		System.out.println("[Package name]:"+PACKAGE_NAME); 
		String[] config_soot=new LoadSoot(config_soot_url).getInfo();
		String []margs={
				"-android-jars",config_soot[0],
				"-src-prec","apk",
				"-f","n",
				"-include-all",
				"-allow-phantom-refs",
				"-cp",config_soot[1]+config_soot[2]+new File("").getAbsolutePath()+";"+new File("").getAbsolutePath(),					
				"-d",".\\apk_output",
				//"-process-dir",new File("").getAbsolutePath()+"\\"+analysis_apk
				"-process-dir",analysis_apk
				
		};
		
		
		viewpagerchecker();
		mygesturechecker();
		multipointchecker();
		longclickchecker();
		
		
		soot.Main.main(margs);
		
		System.out.println("[Main]: following are the results");
		
		
		viewpagerprinter();
		mygestureprinter();
		multipointprinter();
		longclickprinter();

	}
	
	private static void longclickprinter() {
		
		String rs1=lcd.getDebug();
		System.out.print(rs1);
		
		String rs2=lcd.outputResult();
		System.out.print(rs2);		
	}
	
	
	private static void multipointprinter() {
		
		String rs1=mpd.getDebug();
		System.out.print(rs1);
		
		String rs2=mpd.outputResult();
		System.out.print(rs2);		
	}



	private static void viewpagerprinter() {
		String rs1=vpd.outputResult();
		System.out.print(rs1);
	}




	private static void mygestureprinter() {
		String rs1=mgd.getDebug();
		System.out.print(rs1);
		
		String rs2=mgd.outputResult();
		System.out.print(rs2);
	}

	
	
	private static void longclickchecker() {
		lcd=new LongClickDetector();
		Transform md=new Transform("jtp.myInstrumenter3", lcd);
		PackManager.v().getPack("jtp").add(md);	
	}
	


	private static void multipointchecker() {
		mpd=new MultiPointDetector();
		Transform md=new Transform("jtp.myInstrumenter2", mpd);
		PackManager.v().getPack("jtp").add(md);
	}
	

	private static void mygesturechecker() {
		mgd=new MyGestureDetector();
		Transform md=new Transform("jtp.myInstrumenter1", mgd);
		PackManager.v().getPack("jtp").add(md);
	}




	private static void viewpagerchecker() {
		vpd=new ViewPagerDetector();
		Transform vp=new Transform("jtp.myInstrumenter0", vpd);
		PackManager.v().getPack("jtp").add(vp);
	}




	public static String getPackageName(String url){
		String result=null;	
		try{
			ZipFile zipFile;
			zipFile = new ZipFile(new File(url));
			Enumeration enumeration = zipFile.entries();
			ZipEntry zipEntry = null ; 
			zipEntry=zipFile.getEntry("AndroidManifest.xml");		
			AXmlResourceParser parser=new AXmlResourceParser();
			parser.open(zipFile.getInputStream(zipEntry));		
			while (true) {		
				
				int type=parser.next();
				if (type==XmlPullParser.END_DOCUMENT) {
					break;
				}
				if(type==XmlPullParser.START_TAG){
					for (int i=0;i!=parser.getAttributeCount();++i) {					
						if("package".equals(parser.getAttributeName(i))){
							result=parser.getAttributeValue(i);
							return result;
						}		
					}
				}
			}	
			zipFile.close();
		}catch(Exception e){
			e.printStackTrace();
		}
        return result;
	}
}
