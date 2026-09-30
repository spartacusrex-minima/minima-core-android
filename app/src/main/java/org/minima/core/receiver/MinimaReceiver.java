package org.minima.core.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.minima.Minima;
import org.minima.utils.json.JSONObject;
import org.minima.minimaapi.MinimaAPI;
import org.minima.minimaapi.MinimaAPILogger;
import org.minima.minimaapi.MinimaAPIMessages;
import org.minima.core.utils.logger;

import java.util.Objects;

public class MinimaReceiver extends BroadcastReceiver {

    public static final int MAX_MESSAGE_LEN = 256000;

    private Minima mMinima;

    ReceiverDB mDatabase;

    public MinimaReceiver(Minima zMinima, Context zContext){
        super();

        //Store for later
        mMinima = zMinima;

        mDatabase = new ReceiverDB(zContext);
        //mDatabase.wipeDB();

        MinimaAPILogger.log("MAIN - Started logging:"+MinimaAPI.LOGGING_ENABLED);
    }

    public ReceiverDB getDatabase(){
        return mDatabase;
    }

    public void onDestroy(){
        mDatabase.close();
    }

    @Override
    public void onReceive(Context zContext, Intent zIntent) {

        //Put the WHOLE thing in a try catch..
        try{
            String action = zIntent.getAction();

            //Get the Extra data - that is ALWAYS Sent
            String frompackage      = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_PACKAGE_CLASS);
            String frompackageuid   = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_APP_UID);
            String minimauid        = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_REGISTER_MINIMAID);
            String responseid       = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_ID);

            if(MinimaAPI.LOGGING_ENABLED){
                MinimaAPILogger.log("MAIN - RECEIVED BROADCAST respID:"+responseid+" frompackage:"+frompackage+" action:"+action);
            }

            //Get the App
            JSONObject app = mDatabase.selectApp(frompackage, frompackageuid, minimauid);

            //Check the message type
            if (Objects.equals(zIntent.getAction(), MinimaAPIMessages.MINIMA_API_REGISTER)) {

                //CHECK AND ADD to DB
                if(app == null){

                    //Add to the database..
                    mDatabase.insertApp(frompackage, frompackageuid, minimauid);

                    //Send response..
                    String basicmessage = getBasicMessage(true, false, false,"Registered OK!");
                    sendResponse(zContext, frompackage, responseid, minimauid, basicmessage);

                }else{

                    boolean enabled = (int)app.get("penabled")==1;
                    boolean admin   = (int)app.get("admin")==1;

                    //Send response..
                    String basicmessage = getBasicMessage(true, enabled, admin, "Already registered..");
                    sendResponse(zContext, frompackage, responseid, minimauid, basicmessage);
                }

            } else if (Objects.equals(zIntent.getAction(), MinimaAPIMessages.MINIMA_API_CMD)) {

                //Does the App exist
                if(app == null){
                    logger.log("UNKNOWN Package for request : " + frompackage);
                    return;
                }

                boolean enabled = (int)app.get("penabled")==1;
                boolean admin   = (int)app.get("admin")==1;

                //Is it enabled..!
                if(!enabled){
                    String basicmessage = getBasicMessage(false, enabled, admin, "Package NOT enabled in Minima-Core!");
                    sendResponse(zContext, frompackage, responseid, minimauid, basicmessage);
                    return;
                }

                //Is it an ADMIN user
                String Userid = "0xFF";
                if(admin){
                    Userid = "0x00";
                }

                //Update last used
                mDatabase.updateLastUsed(frompackage, frompackageuid);

                String cmd      = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_CMD_ACTION);

                //Check size.. is JSON format so will be longer than normal HEX
                //Need to be able to sign transactions etc..
                if(cmd.length() > MAX_MESSAGE_LEN){
                    String basicmessage = getBasicMessage(false, enabled, admin, "Command too long! MAX("+MAX_MESSAGE_LEN+")");
                    sendResponse(zContext, frompackage, responseid, minimauid, basicmessage);
                    return;
                }

                //Run the command
                String result   = mMinima.runMinimaCMD(cmd, false, Userid);

                //Check the result is within acceptable parameters
                if(result.length() > MAX_MESSAGE_LEN){
                    String basicmessage = getBasicMessage(false, enabled, admin, "Result too long! MAX("+MAX_MESSAGE_LEN+")");
                    sendResponse(zContext, frompackage, responseid, minimauid, basicmessage);
                    return;
                }

                //Send it back..
                sendResponse(zContext, frompackage, responseid, minimauid, result);
            }

        }catch(Exception exc){
            MinimaAPILogger.log("ERROR MinimaReceive :"+exc.toString());
        }
    }

    public void sendResponse(Context zContext, String zPackage, String zResponseID, String zMinimaID, String zResponse){

        if(MinimaAPI.LOGGING_ENABLED){
            MinimaAPILogger.log("MAIN - SEND BROADCAST respID:"+zResponseID+" resp:"+zResponse);
        }

        //Create the Response intent
        Intent intent = new Intent(MinimaAPIMessages.MINIMA_API_RESPONSE);

        //The MinimaID they expect
        intent.putExtra(MinimaAPIMessages.MINIMA_API_REGISTER_MINIMAID, zMinimaID);

        //The ResponseID they expect
        intent.putExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_ID, zResponseID);

        //The REsponse Data
        intent.putExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_RESULT, zResponse);

        //Set to send ONLY back to original sender
        intent.setPackage(zPackage);

        //And broadcast
        zContext.sendBroadcast(intent);
    }

    public void sendNotify(Context zContext, String zPackage, String zMinimaID, String zNotifyMessage){

        //Create the Response intent
        Intent intent = new Intent(MinimaAPIMessages.MINIMA_API_NOTIFY);

        //The MinimaID they expect
        intent.putExtra(MinimaAPIMessages.MINIMA_API_REGISTER_MINIMAID, zMinimaID);

        //The Notify Data
        intent.putExtra(MinimaAPIMessages.MINIMA_API_NOTIFY_DATA, zNotifyMessage);

        //Set to send ONLY back to original sender
        intent.setPackage(zPackage);

        //And broadcast
        zContext.sendBroadcast(intent);
    }

    private String getBasicMessage(boolean zStatus, boolean zEnabled, boolean zAdmin, String zMessage){
        JSONObject ret = new JSONObject();
        ret.put("status",zStatus);
        ret.put("enabled",zEnabled);
        ret.put("admin",zAdmin);
        ret.put("response",zMessage);
        return ret.toString();
    }
}
