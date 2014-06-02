package co.storyroll.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class PushTester {

    final static private String deviceId = "APA91bGgAlPIEyfv9niXXsYV5xdukh7q5mOzRQC_lUYe-WYYpuPUoskzlK24FZdAndQ-k-sPHEs94GPguqOVfTHjaOnbVdLrLgd286lgVKOp7vcsu3X25-IckdtWIIF8AgIZdpwfbf_vgJHzfpWE45yW6XIivtztWQ";
    final static private String apiId = "AIzaSyDZJyN7sY_kqPAacSAGyJXLi_fK56H0MK0";
    final static private String sendUrl = "https://android.googleapis.com/gcm/send";

    public static void testPush_() {
        URL url;
        HttpsURLConnection urlConnection;
        OutputStream os = null;
        InputStream is = null;;
        try {
            url = new URL(sendUrl);
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setUseCaches(false);
            urlConnection.setConnectTimeout(3000);
            urlConnection.setReadTimeout(3000);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("User-Agent", "Android Push tester");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Authorization", "key="+apiId);
            JSONObject message = new JSONObject();
            JSONArray regIds = new JSONArray();
            JSONObject data = new JSONObject();
            regIds.put(deviceId);
            message.put("registration_ids", regIds);
            //message.put("collapse_key", value)
            data.put("count", new Integer(2));
            data.put("uuid", "martynas@ore.lt");
            int[] stories = new int[]{80,82};
            message.put("data", data);
            message.put("stories", stories.toString());
            
            urlConnection.setDoOutput(true);
            os = urlConnection.getOutputStream();
            os.write(message.toString().getBytes());
            os.flush();
            int status = urlConnection.getResponseCode();
            System.out.println( urlConnection.getResponseMessage()+" "+status);
            is = urlConnection.getInputStream();
            byte[] response = new byte[4096];
            is.read(response);
            String responseText = String.valueOf(response); 
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (is != null) {
                    is.close();
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            os = null;
            is = null;
        }
    }

   }
