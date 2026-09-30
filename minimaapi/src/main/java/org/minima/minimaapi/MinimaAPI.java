package org.minima.minimaapi;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Hashtable;
import java.util.Random;

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
    }

    public void ResponseReceived(Intent zIntent){

        //Check the Minima ID values..
        String minimaid = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_REGISTER_MINIMAID);
        if(!minimaid.equals(MINIMA_ID)){
            MinimaAPILogger.log("Received Invalid MinimaID from broadcast! : "+minimaid);
            return;
        }

        String responseid   = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_ID);
        String result       = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_RESULT);

        //Convert the Result to a JSON
        JSONObject json = null;
        try {
            json = new JSONObject(result);
        } catch (JSONException e) {
            MinimaAPILogger.log("Received Invalid JSONObject from broadcast! : "+result);

            json = new JSONObject();
        }

        //Find the Listener..
        MinimaAPIListener listener = mResponseHandlers.get(responseid);

        if(MinimaAPI.LOGGING_ENABLED){
            MinimaAPILogger.log("MinimaAPI - RECEIVED respID:"+responseid+" resp:"+result);
        }

        //Did we find it..
        if(listener == null){
            MinimaAPILogger.log("Received Invalid ResponseID.. not found : "+responseid);
        }else{
            //Remove this response handler..
            mResponseHandlers.remove(responseid);

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

        //Create the Reponse UID
        addResponseHandler(intent, zListener);

        //And broadcast
        mContext.sendBroadcast(intent);
    }
}
