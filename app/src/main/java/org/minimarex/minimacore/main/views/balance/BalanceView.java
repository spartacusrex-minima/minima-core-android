package org.minimarex.minimacore.main.views.balance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.main.BaseView;
import org.minimarex.minimacore.main.views.history.TxPowViewer;
import org.minimarex.minimacore.utils.MinimaCMD;
import org.minimarex.minimacore.utils.MinimaCMDListener;
import org.minimarex.minimacore.utils.logger;

public class BalanceView extends BaseView {

    ListView mBalanceList;

    BalanceAdapter mBalanceAdapter;

    public BalanceView(Activity zActivity){
        super(zActivity, R.layout.view_wallet_balance);

        mBalanceAdapter = new BalanceAdapter(zActivity);

        mBalanceList = getMainView().findViewById(R.id.wallet_balance_list);
        mBalanceList.setAdapter(mBalanceAdapter);

        mBalanceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Get the txpowid
                JSONObject bal = (JSONObject) mBalanceAdapter.getItem(position);

                //Jump to restore wallet..
                Intent myIntent = new Intent(getActivity(), TokenViewer.class);
                Bundle params = new Bundle();
                params.putString("token",bal.toJSONString());
                myIntent.putExtras(params);
                getActivity().startActivity(myIntent);
            }
        });

        refreshView();
    }

    @Override
    public void refreshView() {

        //Have we started
        if(!MinimaCMD.checkMinimaStarted()){

            JSONArray tempbal = new JSONArray();
            JSONObject bal = new JSONObject();
            bal.put("tokenid","0x00");
            bal.put("token","Awaiting Connection..");
            bal.put("confirmed","0");
            bal.put("unconfirmed","0");
            tempbal.add(bal);

            refreshBalance(tempbal);

            return;
        }

        //Run Cmd
        MinimaCMD.runMinima("balance tokendetails:true", new MinimaCMDListener() {
            @Override
            public void cmdResult(JSONObject zResult) {

                //Get the balance response
                JSONArray balance = (JSONArray)zResult.get("response");

                if(balance == null){
                    logger.log("NULL BALANCE : "+zResult.toString());
                    return;
                }

                refreshBalance(balance);
            }
        });
    }

    private void refreshBalance(JSONArray zBalance){
        mBalanceList.post(new Runnable() {
            @Override
            public void run() {
                mBalanceAdapter.updateValues(zBalance);
                mBalanceList.invalidate();
            }
        });
    }
}
