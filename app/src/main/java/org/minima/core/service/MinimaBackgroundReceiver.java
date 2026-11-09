package org.minima.core.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.minima.Minima;
import org.minima.minimaapi.MinimaAPI;
import org.minima.minimaapi.MinimaAPILogger;
import org.minima.minimaapi.MinimaAPIMessages;
import org.minima.utils.messages.Message;
import org.minima.utils.messages.MessageProcessor;

public class MinimaBackgroundReceiver extends MessageProcessor  {

    public static final String MINIMA_BACKGROUND_RECEIVER_REQUEST = "MINIMA_BACKGROUND_RECEIVER_REQUEST";

    Minima mMinima = null;

    Context mContext;

    public static Message createMessage(String zCommand, String zUserID, String zFromPackage,
                                        String zResponseID, String zMiniaUID){

        Message msg = new Message(MINIMA_BACKGROUND_RECEIVER_REQUEST);

        msg.addString("command", zCommand);
        msg.addString("userid", zUserID);
        msg.addString("frompackage", zFromPackage);
        msg.addString("responseid", zResponseID);
        msg.addString("minimauid", zMiniaUID);

        return msg;
    }

    public MinimaBackgroundReceiver(Context zContext, Minima zMinima){
        super("MINIMA_BACKGROUND_RECEIVER");

        mMinima     = zMinima;
        mContext    = zContext;
    }

    @Override
    protected void processMessage(Message zMessage) throws Exception {

        //Log.d("MinimaAPILogger","RECEIVED : "+zMessage.toString());

        if(zMessage.getMessageType().equals(MINIMA_BACKGROUND_RECEIVER_REQUEST)){

            //What is the command
            String cmd = zMessage.getString("command");

            //Get User details..
            String userid       = zMessage.getString("userid");
            String frompackage  = zMessage.getString("frompackage");
            String responseid   = zMessage.getString("responseid");
            String minimauid    = zMessage.getString("minimauid");

            //Run the command..
            String result = mMinima.runMinimaCMD(cmd, false, userid);

            //Send back the result..
            sendResponse(frompackage, responseid, minimauid, result);
        }
    }

    public void sendResponse(String zPackage, String zResponseID, String zMinimaID, String zResponse){

//        if(true){
//            MinimaAPILogger.log("BACKGROUND RECEIVER - SEND BROADCAST respID:"+zResponseID+" resp:"+zResponse);
//        }

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
        mContext.sendBroadcast(intent);
    }
}
