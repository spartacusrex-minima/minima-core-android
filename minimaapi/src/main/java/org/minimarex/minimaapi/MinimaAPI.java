package org.minimarex.minimaapi;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MinimaAPI {

    public static boolean LOGGING_ENABLED = false;

    /**
     * Minima Receiver will need to check The MinimaID
     *
     * Call this from your Main Minima Receiver
     */
    private static String RECEIVER_MINIMA_ID = "";
    public static boolean checkMinimaID(Context zContext, Intent zIntent){

        //Is Minima ID set..
        if(RECEIVER_MINIMA_ID.equals("")){
            SharedPreferences prefs = zContext.getSharedPreferences("minima_api_prefs", zContext.MODE_PRIVATE);
            RECEIVER_MINIMA_ID      = prefs.getString("minima_uid", "");
        }

        //Now get the sent ID
        String minimaid = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_REGISTER_MINIMAID);

        //Return if equal
        return minimaid.equals(RECEIVER_MINIMA_ID);
    }

    //Details used by the CMD receiver
    private String mPackage;
    private String MY_APP_ID;
    private static String MINIMA_ID;

    Context mContext;

    Hashtable<String, MinimaAPIListener> mResponseHandlers = new Hashtable<>();

    MinimaAPIReceive mMinimaAPIReceiver;

    //Large responses arrive as a content:// URI - read the file OFF the main
    //thread (ResponseReceived runs in a BroadcastReceiver on main)
    ExecutorService mFileExecutor = Executors.newSingleThreadExecutor();

    public MinimaAPI(Context zContext, MinimaAPIListener zRegisterListener){
        mContext = zContext;
        mPackage = zContext.getPackageName();

        //Create the 2 IDs
        SharedPreferences prefs = zContext.getSharedPreferences("minima_api_prefs", zContext.MODE_PRIVATE);
        MY_APP_ID = prefs.getString("myapp_uid", "");
        MINIMA_ID = prefs.getString("minima_uid", "");
        if(MY_APP_ID.equals("")){

            //Need to set these..
            MY_APP_ID = getRandomString();
            MINIMA_ID = getRandomString();

            //And store..
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("myapp_uid", MY_APP_ID);
            editor.putString("minima_uid", MINIMA_ID);
            editor.commit();
        }

        //Create a Receiver..
        mMinimaAPIReceiver = new MinimaAPIReceive(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MinimaAPIMessages.MINIMA_API_RESPONSE);

        zContext.registerReceiver(mMinimaAPIReceiver, filter, Context.RECEIVER_EXPORTED);

        //Always send the Resgister broadcast
        Register(zRegisterListener);
    }

    private String getRandomString() {
        String SALTCHARS = "ABCDEF1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 32) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        return "0x"+salt.toString();
    }

    public void onDestroy(){
        try{
            mContext.unregisterReceiver(mMinimaAPIReceiver);
        }catch(Exception exc){}
        try{
            mFileExecutor.shutdown();
        }catch(Exception exc){}
    }

    public void ResponseReceived(Intent zIntent){

        //Check the Minima ID values..
        String minimaid = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_REGISTER_MINIMAID);
        if(!minimaid.equals(MINIMA_ID)){
            MinimaAPILogger.log("Received Invalid MinimaID from broadcast! : "+minimaid);
            return;
        }

        final String responseid = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_ID);

        //Is the payload a content:// file.. (result was too big for an Intent extra)
        final String uristr = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_URI);
        if(uristr != null){

            //Read the file OFF the main thread then deliver as normal
            mFileExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    String result;
                    try{
                        result = readResponseUri(uristr);
                    }catch(Exception exc){
                        MinimaAPILogger.log("ERROR reading response URI : "+exc);

                        //NEVER go silent - callers would wait forever
                        result = "{\"status\":false,\"error\":\"Failed to read large response : "+exc+"\"}";
                    }
                    deliverResult(responseid, result);
                }
            });
            return;
        }

        String result = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_RESULT);
        deliverResult(responseid, result);
    }

    private String readResponseUri(String zUriStr) throws Exception {
        InputStream is = mContext.getContentResolver().openInputStream(Uri.parse(zUriStr));
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int read;
            while((read = is.read(buf)) != -1){
                baos.write(buf, 0, read);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }finally{
            is.close();
        }
    }

    private void deliverResult(String zResponseID, String zResult){

        //Convert the Result to a JSON
        JSONObject json = null;
        try {
            json = new JSONObject(zResult);
        } catch (JSONException e) {
            MinimaAPILogger.log("Received Invalid JSONObject from broadcast! : "+zResult);

            json = new JSONObject();
        }

        //Find the Listener..
        MinimaAPIListener listener = mResponseHandlers.get(zResponseID);

        if(MinimaAPI.LOGGING_ENABLED){
            MinimaAPILogger.log("MinimaAPI - RECEIVED respID:"+zResponseID+" resp:"+zResult);
        }

        //Did we find it..
        if(listener == null){
            MinimaAPILogger.log("Received Invalid ResponseID.. not found : "+zResponseID);
        }else{
            //Remove this response handler..
            mResponseHandlers.remove(zResponseID);

            //Handle reply..
            listener.response(json);
        }
    }

    private void addResponseHandler(Intent zIntent, MinimaAPIListener zListener){

        //Create a random string
        String randomid = getRandomString();

        //Add this to the message
        zIntent.putExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_ID, randomid);

        if(MinimaAPI.LOGGING_ENABLED){
            MinimaAPILogger.log("MinimaAPI - ADD respID:"+randomid);
        }

        //Add to the table..
        mResponseHandlers.put(randomid, zListener);
    }

    private Intent getBaseIntent(String zType){
        //Create Intent
        Intent intent = new Intent(zType);

        //ALWAYS say who you are..
        intent.putExtra(MinimaAPIMessages.MINIMA_API_PACKAGE_CLASS, mPackage);

        //Add the PRIVATE app uid
        intent.putExtra(MinimaAPIMessages.MINIMA_API_APP_UID, MY_APP_ID);

        //Add the PRIVATE Minima uid
        intent.putExtra(MinimaAPIMessages.MINIMA_API_REGISTER_MINIMAID, MINIMA_ID);

        //Set to send ONLY to the Minima Core APK
        intent.setPackage(MinimaAPIMessages.MINIMA_BASE_CLASS);

        return intent;
    }

    private void Register(MinimaAPIListener zListener){

        //Create the register Intent
        Intent intent = getBaseIntent(MinimaAPIMessages.MINIMA_API_REGISTER);

        //Create the Reponse UID
        addResponseHandler(intent, zListener);

        //And broadcast
        mContext.sendBroadcast(intent);
    }

    public void Command(String zCommand, MinimaAPIListener zListener){

        //Create the register Intent
        Intent intent = getBaseIntent(MinimaAPIMessages.MINIMA_API_CMD);

        //What you expect from Minima responses
        intent.putExtra(MinimaAPIMessages.MINIMA_API_CMD_ACTION, zCommand);

        //We can consume oversized results as a content:// file (old nodes ignore this)
        intent.putExtra(MinimaAPIMessages.MINIMA_API_CMD_FILERESP, true);

        //Create the Reponse UID
        addResponseHandler(intent, zListener);

        //And broadcast
        mContext.sendBroadcast(intent);
    }
}
