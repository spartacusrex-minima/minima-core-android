package org.minimarex.minimacore.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.minima.utils.json.JSONObject;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.StartActivity;
import org.minimarex.minimacore.launcher.StartServiceActivity;
import org.minimarex.minimacore.launcher.restore.SeedSyncServiceActivity;
import org.minimarex.minimacore.utils.MinimaCMD;
import org.minimarex.minimacore.utils.MinimaCMDListener;
import org.minimarex.minimacore.utils.Peers;
import org.minimarex.minimacore.utils.logger;

public class SeedSyncActivity extends AppCompatActivity {

    EditText mHost;

    Button mProceedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.seed_sync);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sync_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar tb = findViewById(R.id.toolbar);
        tb.setTitle("Minima-Core");
        setSupportActionBar(tb);

        mHost = findViewById(R.id.seed_sync_host);
        mHost.setText(Peers.getDefaultPeers(this));

        mProceedButton = findViewById(R.id.seed_sync_proceed);
        mProceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mProceedButton.setEnabled(false);

                //Get the Host
                String host = mHost.getText().toString().trim();

                MinimaCMD.runMinima("megammrsync action:resync host:" + host, new MinimaCMDListener() {
                    @Override
                    public void cmdResult(JSONObject zResult) {
                        logger.log("SYNC "+zResult.toString());
                        if(!zResult.getBoolean("status")){
                            SeedSyncActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProceedButton.setEnabled(true);
                                    logger.showDialog(SeedSyncActivity.this,"Error",zResult.getString("error"));
                                }
                            });
                        }else{

                            //Set as new default
                            Peers.setDefaultPeers(SeedSyncActivity.this, mHost.getText().toString().trim());

                            MainActivity.MAIN_ACTIVITY.finish();

                            Intent myIntent = new Intent(SeedSyncActivity.this, StartServiceActivity.class);
                            SeedSyncActivity.this.startActivity(myIntent);

                            finish();
                        }
                    }
                });



            }
        });
    }
}
