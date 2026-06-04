package org.minimarex.minimacore;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.minimarex.minimacore.launcher.LauncherActivity;
import org.minimarex.minimacore.launcher.StartServiceActivity;
import org.minimarex.minimacore.utils.logger;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logger.log("Start Minima-Core..");

        //Have we already setup..
        SharedPreferences prefs = getSharedPreferences("main_prefs", MODE_PRIVATE);
        boolean seedset = prefs.getBoolean("SEED_SET", false);

        if(seedset){

            //Start Main..
            Intent myIntent = new Intent(StartActivity.this, StartServiceActivity.class);
            StartActivity.this.startActivity(myIntent);

        }else{

            //Start Launcher
            Intent myIntent = new Intent(StartActivity.this, LauncherActivity.class);
            StartActivity.this.startActivity(myIntent);
        }

        finish();
    }
}
