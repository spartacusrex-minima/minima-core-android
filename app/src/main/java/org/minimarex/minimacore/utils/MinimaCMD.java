package org.minimarex.minimacore.utils;

import org.minima.database.MinimaDB;
import org.minima.objects.TxPoW;
import org.minima.system.Main;
import org.minima.utils.json.JSONObject;

public class MinimaCMD {

    public static void runMinima(String zCommand){
        runMinima(zCommand, new MinimaCMDListener() {
            @Override
            public void cmdResult(JSONObject zResult) {}
        });
    }

    public static void runMinima(String zCommand, MinimaCMDListener zListener){

        Runnable rr = new Runnable() {
            @Override
            public void run() {
                JSONObject res = Main.getInstance().runSingleMinimaCMD(zCommand);
                //logger.log("Run Minima CMD: "+res.toString());
                zListener.cmdResult(res);
            }
        };

        Thread tt = new Thread(rr);
        tt.start();
    }

    public static boolean checkMinimaStarted(){
        //Update the Values..
        MinimaDB mdb = MinimaDB.getDB();
        if(mdb == null){
            return false;
        }

        //Current TxPoW
        if(mdb.getTxPoWTree().getTip() == null){
            return false;
        }

        TxPoW txp = mdb.getTxPoWTree().getTip().getTxPoW();
        if(txp == null){
            return false;
        }

        return true;
    }
}
