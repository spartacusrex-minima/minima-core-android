package org.minimarex.minimacore.launcher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.minimarex.minimacore.R;
import org.minimarex.minimacore.launcher.restore.RestoreWalletSyncActivity;
import org.minimarex.minimacore.receiver.ReceiverDB;
import org.minimarex.minimacore.utils.logger;

public class ChooseKeySystemActivity extends AppCompatActivity {

    public static ChooseKeySystemActivity CHOOSESYSTEM_ACTIVITY;

    public boolean isNightMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logger.log("Minima Launcher started..");

        CHOOSESYSTEM_ACTIVITY = this;

        EdgeToEdge.enable(this);
        setContentView(R.layout.choosekeysystem_activity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.launcher_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Are we night mode..
        if(isNightMode()){
            ImageView logo = findViewById(R.id.launcher_mainicon);
            logo.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
        }

        Toolbar tb = findViewById(R.id.toolbar);
        tb.setTitle("Minima-Core");
        setSupportActionBar(tb);

        Button newwallet = findViewById(R.id.launcher_button_newsystem);
        newwallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Use this System..
//                SharedPreferences prefs = getSharedPreferences("main_prefs", MODE_PRIVATE);
//                SharedPreferences.Editor editor = prefs.edit();
//                editor.putBoolean("BLOCKS_AS_KEYUSES", true);
//                editor.commit();

                //Start new activity
                Intent myIntent = new Intent(ChooseKeySystemActivity.this, LauncherActivity.class);
                ChooseKeySystemActivity.this.startActivity(myIntent);
            }
        });

        Button restorewallet = findViewById(R.id.launcher_button_legacy);
        restorewallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Use this System..
//                SharedPreferences prefs = getSharedPreferences("main_prefs", MODE_PRIVATE);
//                SharedPreferences.Editor editor = prefs.edit();
//                editor.putBoolean("BLOCKS_AS_KEYUSES", false);
//                editor.commit();

                //Start new activity
                Intent myIntent = new Intent(ChooseKeySystemActivity.this, RestoreWalletSyncActivity.class);
                ChooseKeySystemActivity.this.startActivity(myIntent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
