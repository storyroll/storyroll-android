/*******************************************************************************
 * Copyright 2012 AndroidQuery (tinyeeliu@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Additional Note:
 * 1. You cannot use AndroidQuery's Facebook app account in your own apps.
 * 2. You cannot republish the app as is with advertisements.
 ******************************************************************************/
package com.storyroll.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Base64;

import com.androidquery.util.AQUtility;

public class DataUtility {

    public static byte[] toBytes(Serializable obj){
    	
    	byte[] result = null;
    	
    	try {
    		
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        	ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			close(oos);
			close(baos);
			result = baos.toByteArray();
		} catch (IOException e){
			AQUtility.report(e);
		}
    	
 	
    	return result;
    	
    }
    
    @SuppressWarnings("unchecked")
	public static <T> T toObject(Class<T> cls, InputStream is){
    	
    	T result = null;
    	
    	try {
    		ObjectInputStream ois = new ObjectInputStream(is);
			result = (T) ois.readObject();
			close(is);
		}catch(Exception e){
			AQUtility.report(e);
		}
    	
 	
    	return result;
    	
    }
    
    public static void close(InputStream is){
    	try{
    		if(is != null){
    			is.close();
    		}
    	}catch(Exception e){   		
    	}
    }
    
    public static void close(OutputStream os){
    	try{
    		if(os != null){
    			os.close();
    		}
    	}catch(Exception e){   		
    	}
    }
    
    public static String getBase64Filename(String urlString) throws MalformedURLException 
    {
    	URL url = new URL(urlString);
        String extension = url.getPath().replaceFirst("^.*/[^/]*(\\.[^\\./]*|)$", "$1");
        extension = ".mp4";
        return Base64.encodeToString(urlString.getBytes(), Base64.DEFAULT)+extension;
    }
    
    public static String md5(String s) {
    	if (s==null) return null;
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
