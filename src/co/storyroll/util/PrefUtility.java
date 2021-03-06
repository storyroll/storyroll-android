/*******************************************************************************
 * 
 * martynas@storyroll.co
 * 
 ******************************************************************************/
package co.storyroll.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import co.storyroll.MainApplication;
import co.storyroll.base.Constants;
import co.storyroll.enums.AutofocusMode;
import co.storyroll.enums.AutostartMode;
import co.storyroll.enums.ServerPreference;
import com.androidquery.auth.BasicHandle;
import com.androidquery.util.AQUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class PrefUtility {

	private static final String LOGTAG = "PrefUtility";
	private static SharedPreferences pref;
	
	public static SharedPreferences getPref(){
		if(pref == null){
			pref = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
		}
			
		return pref;
	}
	
	
	public static void put(String name, String value){
		SharedPreferences.Editor edit = getPref().edit();
		edit.putString(name, value);
		edit.commit();	
	}
	
	public static void put(String name, Long value){
		SharedPreferences.Editor edit = getPref().edit();
		edit.putLong(name, value);
		edit.commit();	
	}
	
	public static void put(String name, Boolean value){
		SharedPreferences.Editor edit = getPref().edit();
		edit.putBoolean(name, value);
		edit.commit();	
	}
	
	public static boolean contains(String name){		
		return getPref().contains(name);
	}
	
	public static boolean getBoolean(String name, boolean defaultValue){		
		return getPref().getBoolean(name, defaultValue);
	}
	
	public static Long getLong(String name, Long defaultValue){
		return getPref().getLong(name, defaultValue);
	}
	
	public static String get(String name, String defaultValue){
		return getPref().getString(name, defaultValue);
	}
	
	public static <T extends Enum<T>> void putEnum(Enum<T> value){
		String key = value.getClass().getName();
		put(key, value.name());
		enums.put(key, value);
	}
	
	public static void clearEnum(Class<?> cls){
		String key = cls.getName();
		put(key, (String) null);
	}
	
	
	private static Map<String, Object> enums = new HashMap<String, Object>();
	
	
	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>> T getEnum(Class<T> cls, T defaultValue){
		
		String key = cls.getName();
		
		T result = (T) enums.get(key);
		if(result == null){
			result = PrefUtility.getPrefEnum(cls, defaultValue);
			enums.put(key, result);
		}
		
		return result;
	}
	
	private static <T extends Enum<T>> T getPrefEnum(Class<T> cls, T defaultValue){
		
		T result = null;
		
		String pref = get(cls.getName(), null);
		
		if(pref != null){
			try{
				result = Enum.valueOf(cls, pref); 
			}catch(Exception e){
				clearEnum(cls);
				AQUtility.report(e);
			}
		}
		
		if(result == null){
			result = defaultValue;
		}
		
		return result;
	}
	
	private static String[] deviceIds = {"00000000-64a3-993a-ffff-ffff87b812df", "ffffffff-aa8c-6136-a9de-4e41505adfd9", "00000000-64a3-993a-0bd0-f48a0e6c6d00"};
//    private static String[] deviceIds = {};
	private static Boolean testDevice = null;

    public static boolean isTestDevice(){
		
		if(testDevice == null){
			testDevice = isEmulator() || isTestDevice(getDeviceId());
		}
		
		return testDevice;
	}

    public static void setTestDevice(boolean val)
    {
        testDevice = val;
    }
	
	public static boolean isEmulator(){
		return "sdk".equals(Build.PRODUCT);
	}
	
	private static boolean isTestDevice(String deviceId){
		
		for(int i = 0; i < deviceIds.length; i++){
			if(deviceIds[i].equals(deviceId)){
				return true;
			}
		}
		return false;
	}
	
	private static String deviceId;
	public static String getDeviceId(){
		
		if(deviceId == null){
		
			Context context = MainApplication.getContext();
			
			TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	
		    String tmDevice, tmSerial, tmPhone, androidId;
		    tmDevice = "" + tm.getDeviceId();
		    tmSerial = "" + tm.getSimSerialNumber();
		    androidId = "" + android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
	
		    UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
		    deviceId = deviceUuid.toString();
		    
		}
		
		Log.d(LOGTAG, "device:" + deviceId);
		
	    return deviceId;
	}
	
	
	public static String getConfig(int id){
		
		try{
			//TODO:
//			return MainApplication.get(R.string.tablet);
			return null;
		}catch(Exception e){
			AQUtility.report(e);
		}
		return null;
	}
	
	public static AutostartMode getAutostartMode() {
		AutostartMode am = PrefUtility.getEnum(AutostartMode.class, AutostartMode.NEVER);
		return am;
	}

    public static AutofocusMode getAutofocusMode() {
        AutofocusMode am = PrefUtility.getEnum(AutofocusMode.class, AutofocusMode.FAST);
        return am;
    }

    public static String getApiUrl(String subj) {
        return getApiUrl(subj, null);
    }

	public static String getApiUrl(String subj, String params) {
		ServerPreference sp = PrefUtility.getEnum(ServerPreference.class, ServerPreference.AWS);
		String s = Constants.API_URL_AWS;
		if (sp.equals(ServerPreference.STAGING)) {
			s = Constants.API_URL_STAGING;
		} else if (sp.equals(ServerPreference.DEV)) {
			s = Constants.API_URL_DEV;
		} 
		if (!TextUtils.isEmpty(subj)) {
			s+=subj;
		}
		if (!TextUtils.isEmpty(params)) {
			s+="?"+params;
		}
		return s;
	}

	public static String getUuid() {
		Context context = MainApplication.getContext();
		SharedPreferences settings = context.getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		return settings.getString(Constants.PREF_EMAIL, null);
	}

    public static String getPassword() {
        Context context = MainApplication.getContext();
        SharedPreferences settings = context.getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
        return settings.getString(Constants.PREF_PASSWORD, null);
    }

    public static BasicHandle getBasicHandle(){
        return new BasicHandle(getUuid(), getPassword());
    }
}
