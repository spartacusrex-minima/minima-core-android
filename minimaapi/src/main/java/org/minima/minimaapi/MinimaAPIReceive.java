package org.minima.minimaapi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

public class MinimaAPIReceive extends BroadcastReceiver {

    private MinimaAPI mMinimaAPI;

    public MinimaAPIReceive(MinimaAPI zMinimaAPI){
        super();

        mMinimaAPI = zMinimaAPI;
    }

    @Override
    public void onReceive(Context zContext, Intent zIntent) {

        //What Action
        String action = zIntent.getAction();

        //Check is a Response message
        if (Objects.equals(action, MinimaAPIMessages.MINIMA_API_RESPONSE)) {

            //Process the response..
            mMinimaAPI.ResponseReceived(zIntent);

        }else{
            MinimaAPILogger.log("MinimaAPIReceive RECEIVED UNKNOWN action : "+action);
        }
    }
}
