package org.minimarex.minimacore.main;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.minima.system.params.GeneralParams;
import org.minima.utils.MiniFile;
import org.minima.utils.json.JSONObject;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.main.views.terminal.TerminalActivity;
import org.minimarex.minimacore.service.MinimaService;
import org.minimarex.minimacore.service.MinimaServiceListener;
import org.minimarex.minimacore.utils.MinimaCMD;
import org.minimarex.minimacore.utils.MinimaCMDListener;
import org.minimarex.minimacore.utils.logger;

import java.io.File;


public class MainActivity extends AppCompatActivity implements ServiceConnection, MinimaServiceListener {

    public static MainActivity MAIN_ACTIVITY;

    MinimaService mMinima = null;

    ViewPager mMainPager;

    MainAdapter mMainAdapter;

    ProgressDialog mShutdownDialog = null;

    boolean WIPE_ON_SHUTDOWN = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MAIN_ACTIVITY = this;

        //Start the Service..
        startMinimaService();

        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar tb = findViewById(R.id.toolbar);
        tb.setTitle("Minima-Core");
        tb.getOverflowIcon().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);

        setSupportActionBar(tb);

        mMainAdapter = new MainAdapter(this);

        mMainPager = findViewById(R.id.main_pager);
        mMainPager.setAdapter(mMainAdapter);

        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(mMainPager);

        //Set up Tabs
        tabs.getTabAt(0).setText("Home");
        tabs.getTabAt(0).setIcon(R.drawable.ic_minima);

        tabs.getTabAt(1).setText("Balance");
        tabs.getTabAt(1).setIcon(R.drawable.ic_network);

        tabs.getTabAt(2).setText("Send");
        tabs.getTabAt(2).setIcon(R.drawable.ic_transfer);

        tabs.getTabAt(3).setText("Receive");
        tabs.getTabAt(3).setIcon(R.drawable.ic_freedom);

        //tabs.getTabAt(4).setText("Terminal");
        //tabs.getTabAt(4).setIcon(R.drawable.ic_dapps);

        tabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //Refresh View
                mMainAdapter.refreshPagerView(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                //Refresh View
                mMainAdapter.refreshPagerView(tab.getPosition());
            }
        });

        //Get Files Permission
        String[] perms = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.POST_NOTIFICATIONS
        };
        checkPermission(perms,99);

        checkBattery();
    }

    public void checkBattery() {

        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isIgnoringBatteryOptimizations(packageName)) {

            //Wait for startup sequence..
            waitForMinimaToStartUp();

        } else {

            //Run intent and get the result
            ActivityResultLauncher<Intent> startActivityForResult = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        //Wait for startup sequence..
                        waitForMinimaToStartUp();
                    }
            );

            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));

            startActivityForResult.launch(intent);
        }
    }

    public void waitForMinimaToStartUp(){
        return;
    }

    public void checkPermission(String[] permissions, int requestCode){

        //Check all the requested permissions
        boolean allok = true;
        for(String perm : permissions){
            if(ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED){
                allok = false;
                break;
            }
        }

        //Ask for all the permissions
        if(!allok){
            ActivityCompat.requestPermissions(this, permissions , requestCode);
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.optionsmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
//            case R.id.mainmenu_addpeer :
//
//                addPeerDialog();
//
//                return true;

            case R.id.mainmenu_terminal :

                //addPeerDialog();
                MainActivity.this.startActivity(new Intent(MainActivity.this, TerminalActivity.class));

                return true;

            case R.id.mainmenu_resync :

                MainActivity.this.startActivity(new Intent(MainActivity.this, SeedSyncActivity.class));

                return true;

            case R.id.mainmenu_shhowseed :
                //Show the seed
                MinimaCMD.runMinima("vault", new MinimaCMDListener() {
                    @Override
                    public void cmdResult(JSONObject zResult) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                JSONObject resp = (JSONObject) zResult.get("response");
                                logger.showDialog(MainActivity.this, "Minima Seed",resp.get("phrase").toString());
                            }
                        });
                    }
                });

                return true;

            case R.id.mainmenu_shutdown :
                //Wipe everything..
                shutdownMinima();

                return true;

            case R.id.mainmenu_wipeminima :

                //Wipe everything..
                wipeMinima();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void addPeerDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Minima Peer");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String peer = input.getText().toString();

                MinimaCMD.runMinima("peers action:addpeers peerslist:"+peer);

                Toast.makeText(MainActivity.this, "Peer added..", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void shutdownMinima(){

        //Has the service shutdown
        if(MinimaService.haveStartedShutdown()){
            Toast.makeText(this, "Minima Service Stopped", Toast.LENGTH_SHORT).show();

            //Are we WIPING..
            if(WIPE_ON_SHUTDOWN){
                wipeData();
            }

            return;
        }

        //Cancel the alarm
        if(mMinima != null){
            mMinima.cancelAlarm();
        }

        //Show a shutdown spinning dialog
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mShutdownDialog = new ProgressDialog(MainActivity.this); // this = YourActivity
                mShutdownDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mShutdownDialog.setTitle("Minima Shutting down..");
                mShutdownDialog.setMessage("Please wait...");
                mShutdownDialog.setIndeterminate(true);

                mShutdownDialog.setCanceledOnTouchOutside(true);

                mShutdownDialog.show();
            }
        });

        //Unbind from service
        if(mMinima!=null) {
            mMinima = null;
            try{
                unbindService(this);
            }catch(Exception exc){

            }
        }

        Intent minimaintent = new Intent(MainActivity.this, MinimaService.class);
        boolean stopped = stopService(minimaintent);
    }

    public void wipeMinima(){

        new AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("You are about to RESET Minima!\n\n" +
                        "This will wipe ALL DATA..\n\n" +
                        "Make sure you have backed up your seed and key uses!\n\n" +
                        "Are you sure ?")
                .setIcon(R.drawable.ic_minima)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //Now in shutdopwn mode..
                        logger.log("Minima WIPE STARTED");

                        WIPE_ON_SHUTDOWN = true;

                        shutdownMinima();
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    public void wipeData(){
        logger.log("WIPING BASE FOLDER");

        MiniFile.deleteFileOrFolder(GeneralParams.BASE_FILE_FOLDER, new File(GeneralParams.BASE_FILE_FOLDER));

        //reset prefs..
        SharedPreferences prefs = getSharedPreferences("main_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("SEED_SET", false);
        editor.putString("SEED", "");
        editor.putInt("KEYUSES", 0);
        editor.commit();
    }

    @Override
    public void MinimaServiceShutdown(){

        //Are we WIPING..
        if(WIPE_ON_SHUTDOWN){
            wipeData();
        }

        //Hide the window
        if(mShutdownDialog!=null){
            if(mShutdownDialog.isShowing()){
                mShutdownDialog.dismiss();
            }
        }

        //And shutdown..
        finish();
    }

    @Override
    public void MinimaNewBlock() {
        //New BLOCK - update HOME page..
        mMainAdapter.refreshHomeView();
    }

    @Override
    public void MinimaLoadKeys(int zKeys, boolean zFinished) {}

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();

        //Tell the service Who we are..
        mMinima.mServiceListener = this;

        //Now refresh the views..
        mMainAdapter.refreshAllViews();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mMinima = null;
    }
}