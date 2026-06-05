package org.minimarex.minimacore.launcher;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.minima.system.Main;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.launcher.newwallet.NewWalletRestoreActivity;
import org.minimarex.minimacore.main.MainActivity;
import org.minimarex.minimacore.service.MinimaService;
import org.minimarex.minimacore.utils.MinimaCMD;
import org.minimarex.minimacore.utils.Peers;
import org.minimarex.minimacore.utils.logger;


public class StartServiceActivity extends AppCompatActivity implements ServiceConnection {

    MinimaService mMinima = null;

    ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logger.log("Start Minima service..");

        mProgress = new ProgressDialog(this);
        mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgress.setTitle("Minima Starting..");
        mProgress.setMessage("Please wait...");
        mProgress.setIndeterminate(true);
        mProgress.setCanceledOnTouchOutside(false);

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

                logger.log("Now wait for Minima to say..");
                if(!Main.getInstance().isStartUpComplete()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgress.show();
                        }
                    });
                }

                while(!Main.getInstance().isStartUpComplete()){
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                //Now add some default peers.. from spartacusrex.com
                //MinimaCMD.runMinima("peers action:addpeers peerslist:"+ Peers.getDefaultPeers(StartServiceActivity.this));

                //Add the spartacus peers list
                MinimaCMD.runMinima("peers action:addpeers peerslist:"+ Peers.DEFAULT_MINIMAPEERS);
                //MinimaCMD.runMinima("peers action:addpeers peerslist:10.0.2.2:12001");

                //Internal
                //MinimaCMD.runMinima("peers action:addpeers peerslist:10.0.2.2:12001");

                //Now hide the Progress Dialog
                if(mProgress.isShowing()){
                    mProgress.dismiss();
                }

                //And start the Main activity
                if(Main.getInstance().isStartupError()){
                    StartServiceActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(StartServiceActivity.this)
                                    .setTitle("Startup Error")
                                    .setMessage("There was a critical error starting Minima..\n\n" +
                                            "Please 'Resync' your node\n\n" +
                                            "If that does not work you will need to 'Reset' your node\n\n" +
                                            "Look in the options menu (3 dots)")
                                    .setIcon(R.drawable.ic_minima)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            //Start Main..
                                            Intent myIntent = new Intent(StartServiceActivity.this, MainActivity.class);
                                            StartServiceActivity.this.startActivity(myIntent);

                                            finish();
                                        }}).show();
                        }
                    });
                }else{
                    //Start Main..
                    Intent myIntent = new Intent(StartServiceActivity.this, MainActivity.class);
                    StartServiceActivity.this.startActivity(myIntent);

                    finish();
                }
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
            logger.log("StartServiceActivity Unbind Minima..");
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
}