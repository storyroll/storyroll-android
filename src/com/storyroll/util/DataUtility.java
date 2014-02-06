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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONObject;

import android.util.Base64;
import android.util.Log;

import com.androidquery.util.AQUtility;
import com.bugsense.trace.BugSenseHandler;
import com.storyroll.activity.VideoCaptureActivity;

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
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytesOfMessage = s.getBytes("UTF-8");
            digest.update(bytesOfMessage);
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++) {
            	String hex = Integer.toHexString(0xFF & messageDigest[i]);
            	// append leading zeros
            	if (hex.length() == 1) {
            	    // could use a for loop, but we're only dealing with a single byte
            	    hexString.append('0');
            	}
            	hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
        return "";
    }
    
    public static String stateStr(int state) {
    	String stateStr = "_UNKNOWN_";
		switch (state) {
		case VideoCaptureActivity.STATE_NO_STORY:
			stateStr = "STATE_NO_STORY";
			break;
		case VideoCaptureActivity.STATE_PREV_LAST:
			stateStr = "STATE_PREV_LAST";
			break;
		case VideoCaptureActivity.STATE_INITIAL:
			stateStr = "STATE_INITIAL";
			break;
		case VideoCaptureActivity.STATE_PREV_CAM:
			stateStr = "STATE_PREV_CAM";
			break;
		case VideoCaptureActivity.STATE_REC:
			stateStr = "STATE_REC";
			break;
		case VideoCaptureActivity.STATE_PREV_NEW:
			stateStr = "STATE_PREV_NEW";
			break;
		default:
			stateStr = "_UNKNOWN_";
			break;
		}
		return stateStr;
    }
}
