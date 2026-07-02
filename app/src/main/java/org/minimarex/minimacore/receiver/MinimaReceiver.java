package org.minimarex.minimacore.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.minima.Minima;
import org.minima.utils.json.JSONObject;
import org.minimarex.minimaapi.MinimaAPI;
import org.minimarex.minimaapi.MinimaAPILogger;
import org.minimarex.minimaapi.MinimaAPIMessages;
import org.minimarex.minimacore.utils.logger;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MinimaReceiver extends BroadcastReceiver {

    public static final int MAX_MESSAGE_LEN = 256000;

    private Minima mMinima;

    ReceiverDB mDatabase;

    //Commands run on a single background thread - Android forbids network on the main thread
    //(megammrsync / archive / restoresync open sockets inline) and a slow command must not
    //freeze the UI or the broadcast queue. One thread keeps commands serialised as before.
    ExecutorService mCmdExecutor = Executors.newSingleThreadExecutor();

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
        mCmdExecutor.shutdown();
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

                //Run the command OFF the broadcast (main) thread and respond from there
                final Context appcontext = zContext.getApplicationContext();
                final String  userid     = Userid;
                final boolean fenabled   = enabled;
                final boolean fadmin     = admin;
                final String  fpackage   = frompackage;
                final String  fresponse  = responseid;
                final String  fminimauid = minimauid;

                mCmdExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            //Run the command
                            String result = mMinima.runMinimaCMD(cmd, false, userid);

                            //Check the result is within acceptable parameters
                            if(result.length() > MAX_MESSAGE_LEN){
                                String basicmessage = getBasicMessage(false, fenabled, fadmin, "Result too long! MAX("+MAX_MESSAGE_LEN+")");
                                sendResponse(appcontext, fpackage, fresponse, fminimauid, basicmessage);
                                return;
                            }

                            //Send it back..
                            sendResponse(appcontext, fpackage, fresponse, fminimauid, result);

                        }catch(Exception exc){
                            MinimaAPILogger.log("ERROR MinimaReceive CMD :"+exc.toString());

                            //Tell the caller rather than go silent
                            try{
                                String basicmessage = getBasicMessage(false, fenabled, fadmin, "Command failed : "+exc);
                                sendResponse(appcontext, fpackage, fresponse, fminimauid, basicmessage);
                            }catch(Exception ignore){}
                        }
                    }
                });
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
