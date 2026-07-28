package org.minimarex.minimacore.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.minima.Minima;
import org.minima.database.MinimaDB;
import org.minima.objects.TxPoW;
import org.minima.system.Main;
import org.minima.system.network.webhooks.NotifyManager;
import org.minima.system.params.ParamConfigurer;
import org.minima.utils.MinimaLogger;
import org.minima.utils.MinimaUncaughtException;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.messages.Message;
import org.minima.utils.messages.MessageListener;
import org.minimarex.minimaapi.MinimaAPIMessages;
import org.minimarex.minimacore.main.MainActivity;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.receiver.MinimaReceiver;
import org.minimarex.minimacore.receiver.ReceiverDB;
import org.minimarex.minimacore.utils.logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

/** Foreground Service for the Minima Node
 *
 *  Elias Nemr
 *
 * 23 April 2020
 * */
public class MinimaService extends Service {

    public static SimpleDateFormat DATEFORMAT       = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.ENGLISH);
    public static SimpleDateFormat DATEFORMAT_TIME  = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);

    public static boolean mHaveStartedShutdown = false;

    //Currently Binding doesn't work as we run in a separate process..
    public class MyBinder extends Binder {
        public MinimaService getService() {
            return mService;
        }
    }
    private IBinder mBinder = new MyBinder();
    MinimaService mService;

    public MinimaServiceListener mServiceListener = null;

    //The alarm to ensure Minima doesn't stop
    static boolean mCancelAlarmOnShutdown = false;
    Alarm mAlarm;

    //The Battery receiver
    BroadcastReceiver mBatteryReceiver;

    //Minima Main Starter
    public static Minima minima;

    //Used to update the Notification
    Handler mHandler;

    NotificationManager mNotificationManager;
    Notification mNotification;

    //Information for the Notification
    JSONObject mTxPowJSON = null;

    public static final String CHANNEL_ID = "MinimaServiceChannel";

    PowerManager.WakeLock mWakeLock;
    WifiManager.WifiLock mWifiLock;

    //So we start with a seed
    String mSeed = null;

    //Receiver listening for Broadcast Messages
    private MinimaReceiver mMinimaReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        logger.log("Minima Service Starting..");

        if(mSeed != null){
            MinimaLogger.log("onCreate Service seed provided : "+mSeed);
        }

        mServiceListener = null;

        //Catch ALL Uncaught Exceptions..
        Thread.setDefaultUncaughtExceptionHandler(new MinimaUncaughtException());

        //Have not started shutdown
        mHaveStartedShutdown    = false;
        mCancelAlarmOnShutdown  = false;

        //Power
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Minima::MiniPower");
        if(!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }

        //WiFi..
        WifiManager wifi = (WifiManager) getSystemService(WIFI_SERVICE);
        mWifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL, "Minima::MiniWiFi");
        if(!mWifiLock.isHeld()){
            mWifiLock.acquire();
        }

        //Start Minima
        minima = new Minima();

        mHandler = new Handler(Looper.getMainLooper());

        // Create our notification channel here & add channel to Manager's list
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Minima Node Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
        );

        mNotificationManager = getSystemService(NotificationManager.class);
        mNotificationManager.createNotificationChannel(serviceChannel);

        //Set the Alarm..
        mAlarm = new Alarm();
        mAlarm.cancelAlarm(this);
        mAlarm.setAlarm(this);

        //Store this
        mService = this;

        //Add a Minima listener..
        Main.setMinimaListener(new MessageListener() {
            @Override
            public void processMessage(Message zMessage) throws Exception {

                if(zMessage.getMessageType().equals(NotifyManager.NOTIFY_POST)){
                    //Get the JSON..
                    JSONObject notify = (JSONObject) zMessage.getObject("notify");

                    //What is the Event..
                    String event    = (String) notify.get("event");
                    JSONObject data = (JSONObject) notify.get("data");

                    //if(!event.equals("MINIMALOG")){
                        //MinimaLogger.log("SERVICE received event:"+event+" data:"+data.toString(), false);

                        JSONObject broadmessage = new JSONObject();
                        broadmessage.put("event",event);
                        broadmessage.put("data",data);

                        sendBroadcastNotify(broadmessage.toString());
                    //}

                    if(event.equals("NEWBLOCK")) {

                        //Get the TxPoW
                        mTxPowJSON = (JSONObject) data.get("txpow");

                        //Show a notification
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                //Set status Bar notification
                                setMinimaNotification();
                            }
                        });

                        if(mServiceListener != null){
                            mServiceListener.MinimaNewBlock();
                        }

                    }else if(event.equals("NEWBALANCE")){

                        //Notify the User
                        createNotification("Your balance has changed");

                    }else if(event.equals("SHUTDOWN")){

                        MinimaLogger.log("SERVICE Received SHUTDOWN!");

                        if(!haveStartedShutdown()){
                            stopSelf();
                        }else{
                            MinimaLogger.log("SERVICE allready SHUTDOWN");
                        }

                    }else if(event.equals("LOAD_ALL_KEYS_START")){
                        //MinimaLogger.log("SERVICE received "+event, false);

                    }else if(event.equals("LOAD_ALL_KEYS_FINISH")){
                        //MinimaLogger.log("SERVICE received "+event, false);

                        if(mServiceListener != null) {
                            mServiceListener.MinimaLoadKeys((int) data.get("keys"), true);
                        }
                    }else if(event.equals("LOAD_ALL_KEYS_VALUE")){
                        //MinimaLogger.log("SERVICE received "+event+" "+data.toString(), false);

                        if(mServiceListener != null) {
                            mServiceListener.MinimaLoadKeys((int)data.get("keys"), false);
                        }
                    }
                }
            }
        });

        //Start her up..
        ArrayList<String> vars = new ArrayList<>();

        vars.add("-daemon");

        vars.add("-data");
        vars.add(getFilesDir().getAbsolutePath());

        vars.add("-basefolder");
        vars.add(getFilesDir().getAbsolutePath());

        vars.add("-port");
        vars.add("11001");

        //Normal
        vars.add("-isclient");
        vars.add("-mobile");
        vars.add("-limitbandwidth");

        vars.add("-allowallip");

        //TESTER HACK
        //vars.add("-clean");
        //vars.add("-solo");

        vars.add("-nosyncibd");

        vars.add("-noshutdownhook");

        //Are there any EXTRA params..
        SharedPreferences pref  = getSharedPreferences("main_prefs",MODE_PRIVATE);

        //Add the seed
        vars.add("-anyseed");
        String seed = pref.getString("SEED","");
        vars.add("-seed");
        vars.add(seed);

        //EXTRA params
        String prefstring       = pref.getString("minima_extra_params","");

        //Remove -clean..
        String newprefs = prefstring.replaceAll("-clean","");
        SharedPreferences.Editor edit = pref.edit();
        edit.putString("minima_extra_params",newprefs);
        edit.apply();

        //Check if Valid!
        boolean validparams = ParamConfigurer.checkParams(prefstring);

        if(validparams) {
            if (!prefstring.equals("")) {
                StringTokenizer strtok = new StringTokenizer(prefstring, " ");
                while (strtok.hasMoreTokens()) {
                    String param = strtok.nextToken();
                    vars.add(param);
                }
            }
        }else{

            //Notify User service is now running!
            Toast.makeText(this, "[!] Minima EXTRA Params Error.. pls fix", Toast.LENGTH_LONG).show();
        }

        //Start her up!
        minima.mainStarter(vars.toArray(new String[0]));

        //Notify User service is now running!
        Toast.makeText(this, "Minima Service Started", Toast.LENGTH_SHORT).show();

        //Listen to Battery Events
        addBatteryListener();

        //And the broadcast receiver
        mMinimaReceiver = new MinimaReceiver(minima, this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MinimaAPIMessages.MINIMA_API_REGISTER);
        filter.addAction(MinimaAPIMessages.MINIMA_API_CMD);
        filter.addAction(MinimaAPIMessages.MINIMA_API_FILE);

        registerReceiver(mMinimaReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    public Minima getMinima(){
        return minima;
    }

    public ReceiverDB getReceiverDatabase(){
        return mMinimaReceiver.getDatabase();
    }

    public void sendBroadcastNotify(String zMessage){
        JSONArray apps = mMinimaReceiver.getDatabase().selectAllApps();

        int tot=apps.size();
        for(int i=0;i<tot;i++){

            //Get the App
            JSONObject app = (JSONObject) apps.get(i);

            if((int)app.get("penabled") == 1){
                //Get the package
                String packageclass = app.getString("package");
                String minimaid     = app.getString("minimaid");

                mMinimaReceiver.sendNotify(this, packageclass, minimaid, zMessage);
            }
        }
    }

    public void setTopBlock(){
        try{
            //Get the Tip
            TxPoW tip =  MinimaDB.getDB().getTxPoWTree().getTip().getTxPoW();

            //Set it..
            mTxPowJSON = tip.toJSON();

            //Show a notification
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    //Set status Bar notification
                    setMinimaNotification();
                }
            });

        }catch(Exception exc){
            //MinimaLogger.log(exc);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Set status Bar notification
        setMinimaNotification();

        return START_STICKY;
    }

    private PendingIntent createDefaultPending(){
        //What to start
        PendingIntent pending =  null;
        Intent NotificationIntent = new Intent(getBaseContext(), MainActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pending = PendingIntent.getActivity(getBaseContext(), 0
                    , NotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }else {
            pending = PendingIntent.getActivity(getBaseContext(), 0
                    , NotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }


        return pending;
    }

    public Notification createNotification(String zText){
        mNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(zText)
                .setContentText("Minima Status Channel")
                .setSilent(true)
                .setSmallIcon(R.drawable.ic_minima)
                .setContentIntent(createDefaultPending())
                .build();

        return mNotification;
    }

    public void cancelNotification(String zMiniDAPPUID){
        mNotificationManager.cancel(zMiniDAPPUID,2);
    }

    private void setMinimaNotification(){

        //Set the default message
        if(mTxPowJSON == null){

            //Basic message
            startForeground(1, createNotification("Starting up.. please wait.."));

        }else{
            JSONObject header = (JSONObject)mTxPowJSON.get("header");

            String block    = header.getString("block");
            long timemilli  = Long.valueOf(header.getString("timemilli"));
            String date     = DATEFORMAT.format(new Date(timemilli));

            //String date     = (String) header.get("date");

            //Set block time message
            startForeground(1, createNotification(block + " @ " + date));
        }
    }

    public void stopMinimaService(){
        stopSelf();
    }

    public static boolean haveStartedShutdown(){
        return mHaveStartedShutdown;
    }

    public static void cancelAlarm(){
        mCancelAlarmOnShutdown = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Have started shutdown
        mHaveStartedShutdown = true;
        logger.log("Minima Service onDestroy start");

        //QUIT nicely..
        try{
            String resp = minima.runMinimaCMD("quit compact:true");;

            //Do we cancel the alarm
            if(mCancelAlarmOnShutdown){
                MinimaLogger.log("SERVICE Cancel Restart Alarm");
                mAlarm.cancelAlarm(this);
            }

        }catch(Exception exc){
            MinimaLogger.log(exc);
        }

        //Unregister receiver
        try{
            unregisterReceiver(mMinimaReceiver);
        }catch(Exception exc){}

        //Close the database
        mMinimaReceiver.onDestroy();

        //Not listening anymore..
        Main.setMinimaListener(null);

        //Shut the channel..
        mNotificationManager.deleteNotificationChannel(CHANNEL_ID);

        //Mention..
        Toast.makeText(this, "Minima Service Stopped", Toast.LENGTH_SHORT).show();

        //Release the wakelocks..
        mWakeLock.release();
        mWifiLock.release();

        //Remove the receiver
        if(mBatteryReceiver != null){
            unregisterReceiver(mBatteryReceiver);
        }

        logger.log("Minima Service onDestroy finish");
        if(mServiceListener != null){
            mServiceListener.MinimaServiceShutdown();
        }

        //NULL the main Instance..
        Main.ClearMainInstance();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public static boolean isPlugged(Context context) {
        boolean isPlugged= false;
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;

        return isPlugged;
    }

    public void addBatteryListener(){
        //Listen for Battery Events..
        mBatteryReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

                //Make sure has started up..
                if(getMinima().getMain() == null){
                    return;
                }

                //What Happened..
                if (    plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                        plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS ||
                        plugged == BatteryManager.BATTERY_PLUGGED_DOCK ||
                        plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                    // on AC power
                    //MinimaLogger.log("BATTERY PLUGGED IN");

                    //Set PoW to regular
                    getMinima().getMain().setNormalAutoMineSpeed();

                } else if (plugged == 0) {

                    // on battery power
                    //MinimaLogger.log("BATTERY NOT PLUGGED IN");

                    //Set PoW to regular
                    getMinima().getMain().setLowPowAutoMineSpeed();


                } else {
                    // intent didnt include extra info
                    MinimaLogger.log("BATTERY PLUGGED INFO "+plugged);
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        //Seen an error here
        try{
            registerReceiver(mBatteryReceiver, filter);
        }catch(Exception exc){
            MinimaLogger.log("ERROR adding Battery listener..");
            getMinima().getMain().setLowPowAutoMineSpeed();
        }
    }
}

