package org.minimarex.minimacore.main.views.history;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.minima.objects.base.MiniNumber;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.main.BaseView;
import org.minimarex.minimacore.utils.MinimaCMD;
import org.minimarex.minimacore.utils.MinimaCMDListener;
import org.minimarex.minimacore.utils.logger;

public class HistoryView extends BaseView {

    ListView mHsitoryList;
    HistoryAdapter mHistoryAdapter;

    JSONObject mCurrentHistory;

    MiniNumber mLatestTxPoWTime = MiniNumber.ZERO;

    public HistoryView(Activity zActivity){
        super(zActivity, R.layout.view_apps);

        mHistoryAdapter = new HistoryAdapter(zActivity);

        mHsitoryList = getMainView().findViewById(R.id.wallet_apps_list);
        mHsitoryList.setAdapter(mHistoryAdapter);

        mHsitoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Get the selecetd app
                JSONObject app = (JSONObject) mHistoryAdapter.getItem(position);

                //showInfoDialog(app);
            }
        });
    }

    @Override
    public void refreshView() {

        if(!MinimaCMD.checkMinimaStarted()){
            return;
        }

        //Run Cmd
        MinimaCMD.runMinima("history", new MinimaCMDListener() {
            @Override
            public void cmdResult(JSONObject zResult) {

                //Get the balance response
                JSONObject history = (JSONObject) zResult.get("response");

                //Has it changed.. ?
                JSONArray alltxpow = (JSONArray) history.get("txpows");

                logger.log("HISTORY SIZE : "+alltxpow.size());

                boolean needrefresh = false;
                if(alltxpow.size()>0){

                    //Get the latest TXPOW
                    JSONObject latesttxpow  = (JSONObject) alltxpow.get(0);
                    JSONObject header       = (JSONObject) latesttxpow.get("header");

                    //Get time milli
                    String timemilli = header.getString("timemilli");

                    //Get time milli..
                    MiniNumber lasttime = new MiniNumber(timemilli);
                    if(lasttime.isMore(mLatestTxPoWTime)){
                        needrefresh         = true;
                        mLatestTxPoWTime    = lasttime;
                        logger.log("REFRESH HISTORY");
                    }
                }

                if(needrefresh) {
                    mCurrentHistory = history;

                    JSONArray alldetails = (JSONArray) history.get("details");

                    updateHistoryDetails(alldetails);
                }
            }
        });
    }

    private void updateHistoryDetails(JSONArray zDetails){
        mHsitoryList.post(new Runnable() {
            @Override
            public void run() {
                mHistoryAdapter.updateValues(zDetails);
                mHsitoryList.invalidate();
            }
        });
    }
}
