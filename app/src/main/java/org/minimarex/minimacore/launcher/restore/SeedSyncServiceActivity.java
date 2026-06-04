package org.minimarex.minimacore.launcher.restore;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.minima.database.wallet.Wallet;
import org.minima.system.Main;
import org.minima.utils.json.JSONObject;
import org.minimarex.minimacore.launcher.StartServiceActivity;
import org.minimarex.minimacore.service.MinimaService;
import org.minimarex.minimacore.service.MinimaServiceListener;
import org.minimarex.minimacore.utils.MinimaCMD;
import org.minimarex.minimacore.utils.MinimaCMDListener;
import org.minimarex.minimacore.utils.Peers;
import org.minimarex.minimacore.utils.logger;


public class SeedSyncServiceActivity extends AppCompatActivity implements ServiceConnection, MinimaServiceListener {

    MinimaService mMinima = null;

    ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgress = new ProgressDialog(this);
        mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgress.setTitle("Syncing Minima Wallet..");
        mProgress.setMessage("Please wait...");
        mProgress.setIndeterminate(true);
        mProgress.setCanceledOnTouchOutside(false);

        mProgress.show();

        //Start the Service..
        startMinimaService();

        //Wait for startup
        waitForMinimaToStartUp();
    }

    public void waitForMinimaToStartUp(){

        Runnable wait = new Runnable() {
            @Override
            public void run() {
                logger.log("Wait for Minima Service to start..");

                while(mMinima == null){
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                //Set this to listen for shutdown after sync..
                mMinima.mServiceListener = SeedSyncServiceActivity.this;

                logger.log("Now wait for Minima to say..");

                while(!Main.getInstance().isStartUpComplete()){
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                //Get Key uses..
                SharedPreferences pref  = getSharedPreferences("main_prefs",MODE_PRIVATE);
                int keyuses = pref.getInt("KEYUSES",-1);

                //Now run command to create ALL key uses..
                MinimaCMD.runMinima("keys action:createallkeys keyuses:"+keyuses, new MinimaCMDListener() {
                    @Override
                    public void cmdResult(JSONObject zResult) {}
                });
            }
        };

        Thread tt = new Thread(wait);
        tt.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Unbind from the service..
        if(mMinima != null) {
            mMinima = null;
            unbindService(this);
        }
    }

    public void startMinimaService(){
        //Start the Minima Service..
        Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
        startForegroundService(minimaintent);

        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mMinima = null;
    }

    @Override
    public void MinimaServiceShutdown() {

        //Service has finished resync..
        mProgress.dismiss();

        //Start Main..
        Intent myIntent = new Intent(SeedSyncServiceActivity.this, StartServiceActivity.class);
        SeedSyncServiceActivity.this.startActivity(myIntent);

        finish();
    }

    @Override
    public void MinimaNewBlock() {}

    @Override
    public void MinimaLoadKeys(int zKeys, boolean zFinished) {

        if(!zFinished) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mProgress.isShowing()) {
                        mProgress.setMessage("Generating Keys : " + zKeys + " / " + Wallet.NUMBER_GETADDRESS_KEYS);
                    }
                }
            });
        }else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mProgress.isShowing()) {
                        mProgress.setMessage("Syncing to MegaMMR @ "+ Peers.getDefaultPeers(SeedSyncServiceActivity.this));
                    }
                }
            });

            startMegaMMRSync();
        }
    }

    public void startMegaMMRSync(){

        //Unbind..
        mMinima = null;
        unbindService(SeedSyncServiceActivity.this);

        //Shuts down service on finish
        MinimaCMD.runMinima("megammrsync action:resync host:"+Peers.getDefaultPeers(SeedSyncServiceActivity.this), new MinimaCMDListener() {
            @Override
            public void cmdResult(JSONObject zResult) {

                //Get status..
                boolean status = (boolean)zResult.get("status");

                if(!status){
                    //Service has finished resync..
                    mProgress.dismiss();

                    String error = zResult.getString("error");

                    SeedSyncServiceActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logger.showDialog(SeedSyncServiceActivity.this, "Error", error+"\n\nResync your node when Minima starts..", new Runnable() {
                                @Override
                                public void run() {
                                    //Start Main..
                                    Intent myIntent = new Intent(SeedSyncServiceActivity.this, StartServiceActivity.class);
                                    SeedSyncServiceActivity.this.startActivity(myIntent);

                                    finish();
                                }
                            });
                        }
                    });
                }
            }
        });
    }
}