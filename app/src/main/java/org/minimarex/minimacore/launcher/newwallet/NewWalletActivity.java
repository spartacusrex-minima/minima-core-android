package org.minimarex.minimacore.launcher.newwallet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.minima.utils.BIP39;
import org.minimarex.minimacore.R;

public class NewWalletActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.newwallet_activity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.newwallet_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar tb = findViewById(R.id.toolbar);
        tb.setTitle("New Wallet");
        setSupportActionBar(tb);

        //get a new seed
        String seed = BIP39.convertWordListToString(BIP39.getNewWordList());

        TextView seedtxt = findViewById(R.id.newwallet_seed);
        seedtxt.setText(seed);

        Button proceedbutton = findViewById(R.id.newwallet_button_proceed);
        proceedbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Jump to restore wallet..
                Intent myIntent = new Intent(NewWalletActivity.this, NewWalletRestoreActivity.class);
                NewWalletActivity.this.startActivity(myIntent);

                finish();
            }
        });
    }
}
