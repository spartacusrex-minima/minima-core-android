package org.minima.core.launcher.newwallet;

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
import org.minima.core.R;
import org.minima.core.launcher.LauncherActivity;
import org.minima.core.launcher.StartServiceActivity;
import org.minima.core.utils.logger;

public class NewWalletRestoreActivity extends AppCompatActivity {

    EditText mSeedInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.restorenewwallet_activity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.restorewallet_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar tb = findViewById(R.id.toolbar);
        tb.setTitle("New Wallet");
        setSupportActionBar(tb);

        mSeedInput = findViewById(R.id.restorewallet_seed);

        Button checkbutton = findViewById(R.id.restorewallet_button_check);
        checkbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String seed = mSeedInput.getText().toString().trim();
                if(seed.equals("")){
                    logger.showDialog(NewWalletRestoreActivity.this,"Seed Error","Cannot have an empty seed");
                    return;
                }

                //Check it..
                try{
                    String newseed = BIP39.cleanSeedPhrase(seed);
                    setSeedText(newseed);
                }catch (IllegalArgumentException exc){
                    logger.showDialog(NewWalletRestoreActivity.this,"Seed Error","There is an invalid word in your seed");
                    return;
                }

                logger.showDialog(NewWalletRestoreActivity.this,"Seed Phrase","Your seed phrase is now valid!");
            }
        });

        Button proceedbutton = findViewById(R.id.restorewallet_button_proceed);
        proceedbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Get the seed
                String seed = mSeedInput.getText().toString().trim();
                if(seed.equals("")){
                    logger.showDialog(NewWalletRestoreActivity.this,"Seed Error","Cannot have an empty seed");
                    return;
                }

//                //Get the Node
//                String seed = mSeedInput.getText().toString().trim();
//                if(seed.equals("")){
//                    logger.showDialog(NewWalletRestoreActivity.this,"Seed Error","Cannot have an empty seed");
//                    return;
//                }

                //Set the prefs..
                SharedPreferences prefs = getSharedPreferences("main_prefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("SEED_SET", true);
                editor.putString("SEED", seed);
                editor.commit();

                //Jump to restore wallet..
                Intent myIntent = new Intent(NewWalletRestoreActivity.this, StartServiceActivity.class);
                NewWalletRestoreActivity.this.startActivity(myIntent);

                //Close the main Launchers
                //ChooseKeySystemActivity.CHOOSESYSTEM_ACTIVITY.finish();
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
}
