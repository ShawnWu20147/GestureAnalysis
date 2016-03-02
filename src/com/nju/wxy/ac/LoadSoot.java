package com.nju.wxy.ac;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class LoadSoot {
	File f;
	public LoadSoot(String url){
		f=new File(url);	
	}
	public String[] getInfo(){
		String[] result=new String[3];
		try {
			BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			String android_plateform=br.readLine();
			String jdk=br.readLine();
			String android_jar=br.readLine();
			result[0]=android_plateform;
			result[1]=jdk;
			result[2]=android_jar;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;		
	}

}
