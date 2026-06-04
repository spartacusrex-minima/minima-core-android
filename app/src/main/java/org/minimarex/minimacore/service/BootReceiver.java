package org.minimarex.minimacore.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.minimarex.minimacore.utils.logger;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context zContext, Intent intent) {
        logger.log("MINIMA RECEIVER "+intent.getAction());

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            //Have we already setup..
            SharedPreferences prefs = zContext.getSharedPreferences("main_prefs", Context.MODE_PRIVATE);
            boolean seedset = prefs.getBoolean("SEED_SET", false);

            if(seedset){
                Intent serviceintent = new Intent(zContext, MinimaService.class);
                zContext.startForegroundService(serviceintent);
            }

            //ServiceStarterJobService.enqueueWork(context, new Intent());
        }
    }

}
