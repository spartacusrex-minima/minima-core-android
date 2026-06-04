package org.minimarex.minimacore.launcher.restore;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

import org.minima.utils.BIP39;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.launcher.LauncherActivity;
import org.minimarex.minimacore.utils.logger;

public class RestoreWalletSyncActivity extends AppCompatActivity {

    EditText mSeedInput;
    EditText mKeyUsesInput;

    EditText mMegaNode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.restorewallet_activity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.restorewallet_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar tb = findViewById(R.id.toolbar);
        tb.setTitle("Restore Wallet");
        setSupportActionBar(tb);

        mSeedInput      = findViewById(R.id.restorewallet_seed);
        mKeyUsesInput   = findViewById(R.id.restorewallet_keyuses);
        mMegaNode       = findViewById(R.id.restorewallet_megammr);

        Button checkbutton = findViewById(R.id.restorewallet_button_check);
        checkbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String seed = mSeedInput.getText().toString().trim();
                if(seed.equals("")){
                    showDialog("Seed Error","Cannot have an empty seed");
                    return;
                }

                //Check it..
                try{
                    String newseed = BIP39.cleanSeedPhrase(seed);
                    setSeedText(newseed);
                }catch (IllegalArgumentException exc){
                    showDialog("Seed Error","There is an invalid word in your seed");
                    return;
                }

                showDialog("Seed Phrase","Your seed phrase is now valid!");
            }
        });

        Button proceedbutton = findViewById(R.id.restorewallet_button_proceed);
        proceedbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Get the seed
                String seed = mSeedInput.getText().toString().trim();
                if(seed.equals("")){
                    showDialog("Seed Error","Cannot have an empty seed");
                    return;
                }

                String keyusesstr = mKeyUsesInput.getText().toString().trim();
                if(keyusesstr.equals("")){
                    showDialog("Seed Error","Cannot have an empty seed");
                    return;
                }
                int keyuses = Integer.parseInt(keyusesstr);

                String megammr = mMegaNode.getText().toString().trim();

                //Set the prefs..
                SharedPreferences prefs = getSharedPreferences("main_prefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("SEED_SET", true);
                editor.putString("SEED", seed);
                editor.putInt("KEYUSES", keyuses);
                editor.putString("default_peers", megammr);
                editor.commit();

                //Jump to restore wallet..
                Intent myIntent = new Intent(RestoreWalletSyncActivity.this, SeedSyncServiceActivity.class);
                RestoreWalletSyncActivity.this.startActivity(myIntent);

                //Close the main Laumcher
                LauncherActivity.LAUNCHER_ACTIVITY.finish();

                finish();
            }
        });
    }

    public void setSeedText(String zSeed){
        mSeedInput.post(new Runnable() {
            @Override
            public void run() {
                mSeedInput.setText(zSeed);
            }
        });
    }

    private void showDialog(String zTitle, String zMessage){
        new AlertDialog.Builder(this)
                .setTitle(zTitle)
                .setMessage(zMessage)
                .setIcon(R.drawable.ic_minima)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }}).show();
    }


}
