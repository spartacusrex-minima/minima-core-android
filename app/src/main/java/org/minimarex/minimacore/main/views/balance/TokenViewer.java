package org.minimarex.minimacore.main.views.balance;

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
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.utils.MinimaCMD;
import org.minimarex.minimacore.utils.MinimaCMDListener;
import org.minimarex.minimacore.utils.logger;

public class TokenViewer extends AppCompatActivity {

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
        tb.setTitle("Token Details");
        setSupportActionBar(tb);

        mMainText = findViewById(R.id.txpowviewer_json);

        //Get the txpowid
        String token = getIntent().getExtras().getString("token");

        //Convert to a JSON
        try {
            JSONObject tokenjson = (JSONObject) new JSONParser().parse(token);

            setmMainText(tokenjson);

        } catch (ParseException e) {
            //throw new RuntimeException(e);
            logger.log("Error parsing Token JSON : "+token);
            finish();
        }
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
