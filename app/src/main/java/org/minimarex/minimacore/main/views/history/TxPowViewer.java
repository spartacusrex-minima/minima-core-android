package org.minimarex.minimacore.main.views.history;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.minima.utils.MiniFormat;
import org.minima.utils.json.JSONObject;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.utils.MinimaCMD;
import org.minimarex.minimacore.utils.MinimaCMDListener;
import org.minimarex.minimacore.utils.logger;

public class TxPowViewer extends AppCompatActivity {

    TextView mMainText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_txpowviewer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sync_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar tb = findViewById(R.id.toolbar);
        tb.setTitle("Transaction Viewer");
        setSupportActionBar(tb);

        mMainText = findViewById(R.id.txpowviewer_json);

        //Get the txpowid
        String txpowid = getIntent().getExtras().getString("txpowid");

        //Run Cmd
        MinimaCMD.runMinima("txpow txpowid:"+txpowid, new MinimaCMDListener() {
            @Override
            public void cmdResult(JSONObject zResult) {

                //Get the balance response
                JSONObject txpow = (JSONObject) zResult.get("response");

                if(txpow != null){
                    //Set it..
                    setmMainText(txpow);
                }else{
                    mMainText.setText("TxPoWID not found!");
                }
            }
        });

    }

    public void setmMainText(JSONObject zTxPoWJSON){
        mMainText.post(new Runnable() {
            @Override
            public void run() {
                mMainText.setText(MiniFormat.JSONPretty(zTxPoWJSON));
            }
        });
    }

}
