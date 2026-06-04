package org.minimarex.minimacore.main.views.home;

import android.app.Activity;
import android.widget.TextView;

import org.minima.database.MinimaDB;
import org.minima.objects.TxPoW;
import org.minima.system.params.GlobalParams;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.utils.MinimaCMD;
import org.minimarex.minimacore.utils.MinimaCMDListener;
import org.minimarex.minimacore.utils.logger;
import org.minimarex.minimacore.main.BaseView;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.StringTokenizer;

public class HomeView extends BaseView {

    TextView mKeyUses;
    TextView mVersion;
    TextView mMinima;
    TextView mBlock;
    TextView mBlockTime;
    TextView mConnections;
    TextView mPeers;

    public HomeView(Activity zActivity){
        super(zActivity, R.layout.view_home);

        mKeyUses    = getMainView().findViewById(R.id.home_keyuses);
        mMinima    = getMainView().findViewById(R.id.home_version);
        mBlock      = getMainView().findViewById(R.id.home_blocks);
        mBlockTime  = getMainView().findViewById(R.id.home_block_time);
        mConnections  = getMainView().findViewById(R.id.home_connections);
        mPeers      = getMainView().findViewById(R.id.home_peers);
        mVersion    = getMainView().findViewById(R.id.home_app_version);
    }

    @Override
    public void refreshView(){

        //Have we started
        if(!MinimaCMD.checkMinimaStarted()){
            return;
        }

        //Run some Minima Commands..
        MinimaCMD.runMinima("keys", new MinimaCMDListener() {
            @Override
            public void cmdResult(JSONObject zResult) {
                mKeyUses.post(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            JSONObject resp = (JSONObject) zResult.get("response");
                            int maxuses     = (int) resp.get("maxuses");
                            mKeyUses.setText(""+maxuses);
                        }catch(Exception exc){}
                    }
                });
            }
        });

        MinimaCMD.runMinima("peers", new MinimaCMDListener() {
            @Override
            public void cmdResult(JSONObject zResult) {
                mPeers.post(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            JSONObject resp     = (JSONObject) zResult.get("response");
                            String peerslist    = resp.getString("peerslist");

                            ArrayList<String> allpeers = new ArrayList<>();
                            StringTokenizer strtok = new StringTokenizer(peerslist,",");
                            while(strtok.hasMoreTokens()){
                                allpeers.add(strtok.nextToken());
                            }

                            //Pick a random peer
                            int count       = allpeers.size();
                            Random rand     = new Random();
                            String peer1    = allpeers.get(rand.nextInt(count));

                            mPeers.setText(peer1);
                        }catch(Exception exc){}
                    }
                });
            }
        });

        MinimaCMD.runMinima("network", new MinimaCMDListener() {
            @Override
            public void cmdResult(JSONObject zResult) {
                mConnections.post(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            JSONObject resp     = (JSONObject) zResult.get("response");
                            JSONObject details  = (JSONObject) resp.get("details");
                            int connected       = (int)details.get("connected");
                            mConnections.setText(""+connected);

                        }catch(Exception exc){}
                    }
                });
            }
        });

        mMinima.setText(GlobalParams.getFullMicroVersion());

        //Get tip
        TxPoW txp = MinimaDB.getDB().getTxPoWTree().getTip().getTxPoW();

        int block = txp.getBlockNumber().getAsInt();
        mBlock.setText(""+block);

        long timemilli  = txp.getTimeMilli().getAsLong();
        Date dd         = new Date(timemilli);
        String datestr  = MinimaLogger.DATEFORMAT.format(new Date(timemilli));
        mBlockTime.setText(datestr);

        try {
            String vcode  = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            mVersion.setText(vcode);
        } catch (Exception e) {}

    }
}
