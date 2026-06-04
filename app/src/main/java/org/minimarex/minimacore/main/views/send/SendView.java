package org.minimarex.minimacore.main.views.send;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.main.BaseView;
import org.minimarex.minimacore.utils.MinimaCMD;
import org.minimarex.minimacore.utils.MinimaCMDListener;
import org.minimarex.minimacore.utils.TokenUtils;
import org.minimarex.minimacore.utils.logger;

public class SendView extends BaseView {

    TextView mAmount;
    TextView mAddress;

    Button mSendButton;
    Spinner mTokens;

    TokenSpinnerAdapter mTokenAdapter;

    int mChosenToken=0;

    public SendView(Activity zActivity){
        super(zActivity, R.layout.view_wallet_send);

        mTokens = getMainView().findViewById(R.id.wallet_send_tokens);
        mTokenAdapter = new TokenSpinnerAdapter(zActivity);
        mTokens.setAdapter(mTokenAdapter);

        mTokens.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mChosenToken = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        mAmount     = getMainView().findViewById(R.id.wallet_send_amount);
        mAddress    = getMainView().findViewById(R.id.wallet_send_address);

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
                        Toast.makeText(getActivity(), "Funds Sent!", Toast.LENGTH_SHORT).show();
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
}
