package com.google.codelabs.cosu;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by MS on 02/10/17.
 */
public class FileAsyncTask extends AsyncTask<String,String,String> { //change Object to required type
    private OnTaskCompleted listener;

    public FileAsyncTask(OnTaskCompleted listener){
        this.listener=listener;
    }

    // required methods

    @Override
    protected String doInBackground(String... params) {
        String resposne="Error occured while downloading";
        try {
            URL u = new URL("http://www.crh3.com/spirits.pdf");
            InputStream is = u.openStream();

            DataInputStream dis = new DataInputStream(is);

            byte[] buffer = new byte[1024];
            int length;
            File folder = new File(Environment.getExternalStorageDirectory() + "/spiritspdf");
            if (!folder.exists())
            {
                folder.mkdir();
            }

            FileOutputStream fos = new FileOutputStream(folder.getAbsolutePath()+"/spirits.pdf");
            while ((length = dis.read(buffer))>0) {
                fos.write(buffer, 0, length);
            }
            resposne="Updated file!";

        } catch (MalformedURLException mue) {
            Log.e("SYNC getUpdate", "malformed url error", mue);
            resposne=mue.getMessage();
        } catch (IOException ioe) {
            Log.e("SYNC getUpdate", "io error", ioe);
            resposne=ioe.getMessage();

        } catch (SecurityException se) {
            Log.e("SYNC getUpdate", "security error", se);
            resposne=se.getMessage();

        }
        return resposne;
    }

    protected void onPostExecute(String o){
        // your stuff
        listener.onTaskCompleted(o);
    }
}