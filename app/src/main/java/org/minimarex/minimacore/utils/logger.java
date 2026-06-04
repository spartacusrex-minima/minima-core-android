package org.minimarex.minimacore.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import org.minimarex.minimacore.R;

public class logger {
    public static void log(String zLog){
        Log.d("Minima-Core",zLog);
    }

    public static void showDialog(Context zContext, String zTitle, String zMessage){
        showDialog(zContext, zTitle, zMessage, new Runnable() {
            @Override
            public void run() {}
        });
    }

    public static void showDialog(Context zContext, String zTitle, String zMessage, Runnable zOnOK){
        new AlertDialog.Builder(zContext)
                .setTitle(zTitle)
                .setMessage(zMessage)
                .setIcon(R.drawable.ic_minima)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int whichButton) {
                        zOnOK.run();
                    }}).show();

    }
}
