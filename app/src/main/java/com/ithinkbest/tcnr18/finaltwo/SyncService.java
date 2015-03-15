package com.ithinkbest.tcnr18.finaltwo;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by u1 on 2015/3/15.
 */
//public class MyService {
//}
public class SyncService extends IntentService {
    public final static String LOG_TAG = "markchen987";
    static String oldRaw="";
    public SyncService() {
        super("MyService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG,"... GOING TO FETCH DATA   ***********************************");
        String raw=fetchCloudData();
        if (raw.equals(oldRaw)){
            Log.d(LOG_TAG, "...%%% CONTENT NO CHANGE, NO NEED TO TOUCH SQLITE");
            return;
        }

        //
        oldRaw=raw;

        //

        updateSQLite(raw);
        Log.d(LOG_TAG, "...*** GOING TO INFORM UI TO POPULATE SPINNER");

        //
        Intent alarmIntent = new Intent(this, MainActivity.PopulateSpinnerReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_ONE_SHOT);//getBroadcast(context, 0, i, 0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        //Set the AlarmManager to wake up the system.
           alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 100, pi);
//        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME
//                ,0
//                ,15000
//                ,pi);

//        return true;

    }
    private String fetchCloudData() {
        String result=null;
        Log.d(LOG_TAG, "Starting sync");
        //  String locationQuery = Utility.getPreferredLocation(getContext());

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;
        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String BASE_URL =
                    "http://opensource-forever.com/final/list.php";


            URL url = new URL(BASE_URL);

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
          //  forecastJsonStr = buffer.toString();
          //  updateSQLite(buffer.toString());
            //    getWeatherDataFromJson(forecastJsonStr, locationQuery);
            Log.d(LOG_TAG, "...going to return result as "+ buffer.toString());
            result= buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
//        } catch (JSONException e) {
//            Log.e(LOG_TAG, e.getMessage(), e);
//            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return result;
    }

    private void updateSQLite(String str) {
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(str);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (jsonArray.length() < 1) return;
        int cnt = getContentResolver().delete(MembersProvider.CONTENT_URL, "", null);
        Log.d(LOG_TAG, "delete cnt= " + cnt);


        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                // *** NEED TO PAY ATTENTION TO ID
                int memberid = jsonObject.getInt("ID");
                String username = jsonObject.getString(MembersProvider.COLUMN_USERNAME);
                String nickname = jsonObject.getString(MembersProvider.COLUMN_NICKNAME);
                String email = jsonObject.getString(MembersProvider.COLUMN_EMAIL);
                String grp = jsonObject.getString(MembersProvider.COLUMN_GRP);


                Log.d(LOG_TAG, "===> " + memberid + "," + username+ "," + nickname+ "," + email+ "," + grp);
                ContentValues values = new ContentValues();

                values.put(MembersProvider.COLUMN_MEMBERID, memberid);
                values.put(MembersProvider.COLUMN_USERNAME, username);
                values.put(MembersProvider.COLUMN_NICKNAME, nickname);
                values.put(MembersProvider.COLUMN_EMAIL, email);
                values.put(MembersProvider.COLUMN_GRP, grp);


                // Provides access to other applications Content Providers
                Uri uri = getContentResolver().insert(MembersProvider.CONTENT_URL, values);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

}
