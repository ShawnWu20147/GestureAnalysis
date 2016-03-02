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



public class Instrument {
	public static final String []demo_args=new String[]{new File("").getAbsolutePath()+"\\"+"AsyncTask.apk","soot.cfg"};
	public static String PACKAGE_NAME="com.example.asynctask";
	
	
	
	public static void main(String[] args){
		
		
		
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
				"-f","dex",
				"-include-all",
				"-allow-phantom-refs",
				"-cp",config_soot[1]+config_soot[2]+new File("").getAbsolutePath()+";"+new File("").getAbsolutePath(),					
				"-d",".\\apk_output",
				//"-process-dir",new File("").getAbsolutePath()+"\\"+analysis_apk
				"-process-dir",analysis_apk
				
		};
		
		Transform vp=new Transform("jtp.myInstrumenter0", new InstrumentTransformer());
		PackManager.v().getPack("jtp").add(vp);
		
		
		soot.Main.main(margs);
		
		System.out.println("[Main]: following are the results");
		
	

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
