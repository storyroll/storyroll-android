package co.storyroll.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import co.storyroll.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class LogReadActivity extends ListActivity{
    private static final String LOGTAG = "LOGREAD";
    private LogStringAdaptor adaptor = null;
    private ArrayList<String> logarray = null;
    private static LogReaderTask logReaderTask = null;
    
    private static final String processId = Integer.toString(android.os.Process.myPid());
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        logarray = new ArrayList<String>();
        adaptor = new LogStringAdaptor(this, R.id.txtLogString, logarray);

        setListAdapter(adaptor);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (logReaderTask==null) {
            logReaderTask = new LogReaderTask();
            logReaderTask.execute();
        }

        printKey();
    }

    private void printKey() {
        try {

            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);

            for (Signature signature : info.signatures)
            {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }

        } catch (PackageManager.NameNotFoundException e) {
            Log.e("name not found", e.toString());
        } catch (NoSuchAlgorithmException e) {
            Log.e("no such an algorithm", e.toString());
        }
    }

    @Override
    protected void onDestroy() {
            logReaderTask.stopTask();
            
            super.onDestroy();
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
            super.onListItemClick(l, v, position, id);
            
            final AlertDialog.Builder builder = new AlertDialog.Builder(LogReadActivity.this);
            SpannableString text = ((SpannableString) ((TextView)v).getText());
            builder.setMessage(text);
            builder.show();
    }

    private int getLogColor(String type) {
            int color = Color.BLUE;
            
            if(type.equals("D"))
            {
                    color = Color.rgb(0, 0, 200);
            }
            else if(type.equals("W"))
            {
                    color = Color.rgb(128, 0, 0);
            }
            else if(type.equals("E"))
            {
                    color = Color.rgb(255, 0, 0);;
            }
            else if(type.equals("I"))
            {
                    color = Color.rgb(0, 128, 0);;
            }
            
            return color;
    }
    
    private class LogStringAdaptor extends ArrayAdapter<String>{
    private List<String> objects = null;
    
            public LogStringAdaptor(Context context, int textviewid, List<String> objects) {
                    super(context, textviewid, objects);
                    
                    this.objects = objects;
            }
            
            @Override
            public int getCount() {
                    return ((null != objects) ? objects.size() : 0);
            }
            
            @Override
            public long getItemId(int position) {
                    return position;
            }
            
            @Override
            public String getItem(int position) {
                    return ((null != objects) ? objects.get(position) : null);
            }
            
            public View getView(int position, View convertView, ViewGroup parent) {
                    View view = convertView;
                    
                    if(null == view)
                    {
                            LayoutInflater vi = (LayoutInflater)LogReadActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            view = vi.inflate(R.layout.log_item, null);
                    }
                    
                    String data = objects.get(position);
                    
                    if(null != data)
                    {
                            TextView textview = (TextView)view.findViewById(R.id.txtLogString);
//                            String type = data.substring(0, 1);
//                            String line = data.substring(2);
                            String type = data.substring(19, 20);
                            String line = data.substring(6);
                            textview.setText(line);
                            textview.setTextColor(getLogColor(type));
                    }
                    
                    return view;
            }
}
    
    private class LogReaderTask extends AsyncTask<Void, String, Void>
{
//            private final String[] LOGCAT_CMD = new String[] { "logcat", "-t", "500", "AndroidRuntime:E [Your Log Tag Here]:V *:S"};
    		private final String[] LOGCAT_CMD = new String[] { "logcat", "-v", "time", "AndroidRuntime:E *:V"};
//    		private final String[] LOGCAT_CMD = new String[] { "logcat"};
            private final int BUFFER_SIZE = 1024;
            
            private boolean isRunning = true;
            private Process logprocess = null;
            private BufferedReader reader = null;
            private String[] line = null;
            private String lastLine = "";
            
            @Override
            protected Void doInBackground(Void... params) {
                    try {
                            logprocess = Runtime.getRuntime().exec(LOGCAT_CMD);
                    } catch (IOException e) {
                            e.printStackTrace();
                            
                            isRunning = false;
                    }
                    
                    try {
                            reader = new BufferedReader(new InputStreamReader(
                                            logprocess.getInputStream()),BUFFER_SIZE);
                    }
                    catch(IllegalArgumentException e){
                            e.printStackTrace();
                            
                            isRunning = false;
                    }
                    
                    line = new String[1];
                    
                    try {
                            while(isRunning)
                            {
                                    line[0] = reader.readLine();
                                    publishProgress(line);
                            }
                                    
                    } 
                    catch (IOException e) {
                            e.printStackTrace();
                            
                            isRunning = false;
                    }
                    
                    return null;
            }
    
            @Override
            protected void onCancelled() {
                    super.onCancelled();
            }
            
            @Override
            protected void onPreExecute() {
                    super.onPreExecute();
            }
            
            @Override
            protected void onPostExecute(Void result) {
                    super.onPostExecute(result);
            }
            
            @Override
            protected void onProgressUpdate(String... values) {
                    super.onProgressUpdate(values);
                    if (!lastLine.equals(values[0])) 
                    {
                    	lastLine = values[0];
                    	adaptor.add(values[0]);
                    }
            }
            
            public void stopTask(){
                    isRunning = false;
                    logprocess.destroy();                   
            }
}
}