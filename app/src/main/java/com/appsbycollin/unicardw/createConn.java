package com.appsbycollin.unicardw;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class createConn {
    //go through this at a point
    private static final String TAG = "AppLog";
    StringBuilder sb = new StringBuilder();

    public String sendPostRequest(String requestURL,
                                  HashMap<String, String> postDataParams) {

        //String response = "";
        try {
            URL url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000); //10s
            conn.setConnectTimeout(10000); //10s
            conn.setRequestMethod("POST");
            conn.setDoInput(true); //if false, getInputStream() can't be called.
            conn.setDoOutput(true); //if false, getOutputStream() can't be called.

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();

            int responseCode = conn.getResponseCode();


            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String JSON;
                while((JSON =  br.readLine()) != null) {
                    sb.append(JSON + "\n");
                     Log.i(TAG, "Appended: " + sb.toString());
                }
            }
            else {
             Log.i(TAG, "An error occured! Are you connected to the right network? " +
                        "Is your password correct?");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return result.toString();
    }
}
