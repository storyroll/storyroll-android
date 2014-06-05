package co.storyroll;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import co.storyroll.base.Constants;
import co.storyroll.https.AdditionalKeyStoresSSLSocketFactory;
import co.storyroll.util.AppUtility;
import co.storyroll.util.ErrorReporter;
import co.storyroll.util.PrefUtility;
import com.androidquery.callback.AbstractAjaxCallback;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.BitmapAjaxCallback;
import com.androidquery.util.AQUtility;
import com.bugsense.trace.BugSenseHandler;
import org.apache.http.conn.ssl.SSLSocketFactory;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;

public class MainApplication extends Application implements Thread.UncaughtExceptionHandler{

	
	public static final String MOBILE_AGENT = "Mozilla/5.0 (Linux; U; Android 2.2) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533";
    private static final String TAG = "Application";

    private static SSLSocketFactory ssf = null;

    @Override
    public void onCreate() {     
        
		AQUtility.setContext(this);

        
		boolean test = PrefUtility.isTestDevice();
		Log.v(TAG, "isTestDevice: " + test);
		
        if( test ){
        	AQUtility.setDebug(true);
        }
        
        bugSense();
        
//        ErrorReporter.installReporter(AQUtility.getContext());
//
//        AQUtility.setExceptionHandler(this);
        
        AQUtility.setCacheDir(null);

        ssf = createAdditionalCertsSSLSocketFactory();
        AbstractAjaxCallback.setSSF(ssf);

        AjaxCallback.setNetworkLimit(8);
        //AjaxCallback.setAgent(MOBILE_AGENT);

        BitmapAjaxCallback.setIconCacheLimit(200);
        BitmapAjaxCallback.setCacheLimit(80);
        BitmapAjaxCallback.setPixelLimit(400 * 400);
        BitmapAjaxCallback.setMaxPixelLimit(2000000);
        
        File workingDir = new File(AppUtility.getAppWorkingDir(this));
        workingDir.mkdirs();
        workingDir = new File(AppUtility.getVideoCacheDir(getApplicationContext()));
        workingDir.mkdirs();
        
        
        super.onCreate();
    }
	
	private void bugSense(){
		
		try{
			AQUtility.debug("tracking!");
			BugSenseHandler.initAndStartSession(this, Constants.BUGSENSE_API_KEY);
		}catch(Exception e){
			AQUtility.debug(e);
		}
	}
	
	
	
	@Override
	public void onLowMemory(){	
    	BitmapAjaxCallback.clearCache();
    }
	
	public static Context getContext(){
		return AQUtility.getContext();
	}
	public static String get(int id){
		return getContext().getString(id);
	}
	

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		ErrorReporter.report(ex, true);
	}

//    private SSLSocketFactory StoryRollSSLSocketFactory() {
//        SSLSocketFactory ret = null;
//        try {
//            final KeyStore ks = KeyStore.getInstance("BKS");
//
//            final InputStream inputStream = getApplicationContext().getResources().openRawResource(R.raw.cert_storyroll_lb);
//
//            ks.load(inputStream, getApplicationContext().getString(R.string.store_pass).toCharArray());
//            inputStream.close();
//
//            ret = new SSLSocketFactory(ks);
//            Log.d(TAG, "SSLSocketFactory done");
//        } catch (UnrecoverableKeyException ex) {
//            Log.d(TAG, ex.getMessage());
//        } catch (KeyStoreException ex) {
//            Log.d(TAG, ex.getMessage());
//        } catch (KeyManagementException ex) {
//            Log.d(TAG, ex.getMessage());
//        } catch (NoSuchAlgorithmException ex) {
//            Log.d(TAG, ex.getMessage());
//        } catch (IOException ex) {
//            Log.d(TAG, ex.getMessage());
//        } catch (Exception ex) {
//            Log.d(TAG, ex.getMessage());
//        } finally {
//            return ret;
//        }
//    }

    private SSLSocketFactory newSslSocketFactory() {
        try {
            // Get an instance of the Bouncy Castle KeyStore format
            KeyStore trusted = KeyStore.getInstance("BKS");

            // Get the raw resource, which contains the keystore with your trusted certificates (root and any intermediate certs)
            InputStream in = getApplicationContext().getResources().openRawResource(R.raw.cert_storyroll_lb);
            try {
                // Initialize the keystore with the provided trusted certificates.
                // Also provide the password of the keystore
                trusted.load(in, getApplicationContext().getString(R.string.store_pass).toCharArray());
                Log.d(TAG, "Keystore initialized with StoryRoll trusted certificate");
            } finally {
                in.close();
            }



            // Pass the keystore to the SSLSocketFactory. The factory is responsible for the verification of the server certificate.
            SSLSocketFactory sf = new SSLSocketFactory(trusted);


            // Hostname verification from certificate
            // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d4e506
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            return sf;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private SSLSocketFactory createAdditionalCertsSSLSocketFactory() {
        try {
            // Get an instance of the Bouncy Castle KeyStore format
            KeyStore trusted = KeyStore.getInstance("BKS");

            // Get the raw resource, which contains the keystore with your trusted certificates (root and any intermediate certs)
            InputStream in = getApplicationContext().getResources().openRawResource(R.raw.cert_storyroll_lb);
            try {
                // Initialize the keystore with the provided trusted certificates.
                // Also provide the password of the keystore
                trusted.load(in, getApplicationContext().getString(R.string.store_pass).toCharArray());
                Log.d(TAG, "Keystore initialized with StoryRoll trusted certificate");
            } finally {
                in.close();
            }



            // Pass the keystore to the SSLSocketFactory. The factory is responsible for the verification of the server certificate.
//            SSLSocketFactory sf = new SSLSocketFactory(trusted);
            SSLSocketFactory sf = new AdditionalKeyStoresSSLSocketFactory(trusted);

            // Hostname verification from certificate
            // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d4e506
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            return sf;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

}
