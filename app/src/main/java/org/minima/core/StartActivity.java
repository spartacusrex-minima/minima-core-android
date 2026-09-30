package org.minima.core;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.minima.core.launcher.LauncherActivity;
import org.minima.core.launcher.StartServiceActivity;
import org.minima.core.utils.logger;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logger.log("Start Minima-Core..");

        //Use this System..
        SharedPreferences prefs = getSharedPreferences("main_prefs", MODE_PRIVATE);

        //Have we already setup..
        boolean seedset = prefs.getBoolean("SEED_SET", false);

        if(seedset){

            logger.log("Start Minima-Service..");

            //Start Main..
            Intent myIntent = new Intent(StartActivity.this, StartServiceActivity.class);
            StartActivity.this.startActivity(myIntent);

        }else{

            logger.log("Start Choose..");

            //Start Launcher
            Intent myIntent = new Intent(StartActivity.this, LauncherActivity.class);
            StartActivity.this.startActivity(myIntent);
        }

        finish();
    }
}
