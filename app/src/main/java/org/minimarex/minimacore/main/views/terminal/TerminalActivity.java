package org.minimarex.minimacore.main.views.terminal;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.minima.system.params.GlobalParams;
import org.minima.utils.MiniFormat;
import org.minima.utils.json.JSONObject;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.launcher.StartServiceActivity;
import org.minimarex.minimacore.main.MainActivity;
import org.minimarex.minimacore.utils.MinimaCMD;
import org.minimarex.minimacore.utils.MinimaCMDListener;
import org.minimarex.minimacore.utils.Peers;
import org.minimarex.minimacore.utils.logger;

public class TerminalActivity extends AppCompatActivity {

    public int MAX_TERMINAL_LENGTH = 20000;

    ScrollView mScroller;
    TextView mMainText;
    EditText mInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_terminal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sync_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar tb = findViewById(R.id.toolbar);
        tb.setTitle("Terminal");
        setSupportActionBar(tb);

        mScroller   = findViewById(R.id.terminal_scroller);
        mMainText   = findViewById(R.id.terminal_maintext);
        mMainText.setTypeface(Typeface.MONOSPACE);
        mMainText.setTextIsSelectable(true);

        appendText("**********************************************");
        appendText("*  __  __  ____  _  _  ____  __  __    __    *");
        appendText("* (  \\/  )(_  _)( \\( )(_  _)(  \\/  )  /__\\   *");
        appendText("*  )    (  _)(_  )  (  _)(_  )    (  /(__)\\  *");
        appendText("* (_/\\/\\_)(____)(_)\\_)(____)(_/\\/\\_)(__)(__) *");
        appendText("*                                            *");
        appendText("**********************************************");
        appendText("Welcome to Minima Core "+ GlobalParams.getFullMicroVersion());
        appendText("You can run commands in this Terminal..");

        mInput      = findViewById(R.id.terminal_input);
        mInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String cmd = mInput.getText().toString().trim();
                mInput.post(new Runnable() {
                    @Override
                    public void run() {
                        mInput.setText("");
                    }
                });

                if(actionId == EditorInfo.IME_ACTION_DONE){
                    runCMD(cmd);
                    return true;
                }

                if(event != null) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (cmd.equals("")) {
                            appendText("\n");
                            return false;
                        }

                        //Run this command
                        runCMD(cmd);

                        return true;
                    }
                }
                return false;
            }
        });

        Button but  = findViewById(R.id.terminal_send);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cmd = mInput.getText().toString().trim();
                mInput.post(new Runnable() {
                    @Override
                    public void run() {
                        mInput.setText("");
                    }
                });

                if(cmd.equals("")){
                    return;
                }
                //Run this command
                runCMD(cmd);
            }
        });
    }

    public void runCMD(String zCommand){

        //First append the command
        appendText(zCommand);

        //Run and append the result..
        MinimaCMD.runMinima(zCommand, new MinimaCMDListener() {
            @Override
            public void cmdResult(JSONObject zResult) {
                appendText(MiniFormat.JSONPretty(zResult));
            }
        });
    }

    public void appendText(String zText) {
        mMainText.post(new Runnable() {
            @Override
            public void run() {
                String text = mMainText.getText().toString();
                int len     = text.length();
                if(len > MAX_TERMINAL_LENGTH){
                    String newtext = text.substring(len-MAX_TERMINAL_LENGTH,len)+"\n" + zText;
                    mMainText.setText(newtext);
                }else{
                    mMainText.append("\n" + zText);
                }

                mScroller.post(new Runnable() {
                    @Override
                    public void run() {
                        mScroller.scrollTo(0,1000000);
                    }
                });
            }
        });
    }
}
