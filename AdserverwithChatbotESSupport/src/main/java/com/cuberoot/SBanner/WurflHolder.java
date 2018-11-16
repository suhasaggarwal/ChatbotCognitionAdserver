package com.cuberoot.SBanner;
import java.io.IOException;

import net.sourceforge.wurfl.core.resource.XMLResource;
import net.sourceforge.wurfl.core.CustomWURFLHolder;
import net.sourceforge.wurfl.core.WURFLHolder;


public class WurflHolder {

	  public static WURFLHolder wurflHolder;

	    private static WURFLHolder INSTANCE=null;

		public static synchronized WURFLHolder getInstance() {

			try{
			
			if(INSTANCE == null){
				  
				INSTANCE= new CustomWURFLHolder(DeviceIdentification.class.getResource("/wurfl.xml").toString());
			    return INSTANCE;
			
			}

			else
				 {
					  return INSTANCE ;
				 }

			}
		    catch(Exception e)	
			{
		    	e.printStackTrace();
			}
			
			return null;
			
			}
                   

}




