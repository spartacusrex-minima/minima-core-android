package org.minima.core.main.views.send;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.journeyapps.barcodescanner.ScanOptions;

import org.minima.core.main.MainActivity;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.core.R;
import org.minima.core.main.BaseView;
import org.minima.core.utils.MinimaCMD;
import org.minima.core.utils.MinimaCMDListener;
import org.minima.core.utils.TokenUtils;
import org.minima.core.utils.logger;

public class SendView extends BaseView {

    TextView mAmount;
    TextView mAddress;

    Button mSendButton;
    Button mQRButton;
    AutoCompleteTextView mTokens;

    TokenSpinnerAdapter mTokenAdapter;

    int mChosenToken=0;

    MainActivity mMainQR;

    public SendView(MainActivity zActivity){
        super(zActivity, R.layout.view_wallet_send);

        mMainQR = zActivity;

        mTokens = getMainView().findViewById(R.id.wallet_send_tokens);
        mTokenAdapter = new TokenSpinnerAdapter(zActivity);
        mTokens.setAdapter(mTokenAdapter);
        mTokens.setOnItemClickListener((parent, view, position, id) -> {
            mChosenToken = position;
        });

        mAmount     = getMainView().findViewById(R.id.wallet_send_amount);
        mAddress    = getMainView().findViewById(R.id.wallet_send_address);

        mQRButton = getMainView().findViewById(R.id.wallet_send_qrscan);
        mQRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMainQR.startQRScanner();
            }
        });

        mSendButton = getMainView().findViewById(R.id.wallet_send_sendbutton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amount       = mAmount.getText().toString().trim();
                String address      = mAddress.getText().toString().trim();

                if(amount.equals("") || address.equals("")){
                    logger.showDialog(getActivity(),"Error","Cannot have blank inputs..");
                    return;
                }

                JSONObject token    = mTokenAdapter.getToken(mChosenToken);
                String tokenid      = token.get("tokenid").toString();
                String tokenname    = TokenUtils.getTokenName(token);

                showConfirmDialog(amount, address, tokenname, tokenid);
            }
        });

        refreshView();
    }

    private void showConfirmDialog(String zAmount, String zAddress, String zTokenName, String zTokenid ){
        new AlertDialog.Builder(getActivity())
                .setTitle("Confirm")
                .setMessage("You are about to send "+zAmount+" "+zTokenName+" to \n"+zAddress)
                .setIcon(R.drawable.ic_minima)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int whichButton) {
                        sendFunds(zAmount, zAddress, zTokenid);
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    protected void sendFunds(String zAMount, String zAddress, String zTokenid){

        //Clear inputs
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), "Sending funds..", Toast.LENGTH_SHORT).show();

                mAmount.setText("");
                mAddress.setText("");
            }
        });

        String cmd = "send amount:"+zAMount+" address:"+zAddress+" tokenid:"+zTokenid;
        MinimaCMD.runMinima(cmd, new MinimaCMDListener() {
            @Override
            public void cmdResult(JSONObject zResult) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if(!zResult.getBoolean("status")){
                            //Something went wrong..
                            Toast.makeText(getActivity(), zResult.getString("error"), Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(getActivity(), "Funds Sent!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void refreshView() {

        //Have we started
        if(!MinimaCMD.checkMinimaStarted()){
            return;
        }

        //Run Cmd
        MinimaCMD.runMinima("balance", new MinimaCMDListener() {
            @Override
            public void cmdResult(JSONObject zResult) {

                //Get the balance response
                JSONArray balance = (JSONArray)zResult.get("response");

                if(balance == null){
                    logger.log("NULL BALANCE : "+zResult.toString());
                    return;
                }

                refreshTokenSpinner(balance);
            }
        });
    }

    public void refreshTokenSpinner(JSONArray zBalance){
        mTokens.post(new Runnable() {
            @Override
            public void run() {
                mTokenAdapter.updateTokens(zBalance);
                mTokens.invalidate();
            }
        });
    }

    public void setAddressValue(String zValue){
        mAddress.setText(zValue);
    }
}