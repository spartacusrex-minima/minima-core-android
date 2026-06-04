package org.minimarex.minimacore.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Peers {

    public static String DEFAULT_MINIMAPEERS = "https://spartacusrex.com/minimapeers.txt";

    private static String DEFAULT_PEER = "spartacusrex.com:9001";
    //public static String DEFAULT_PEER = "https://spartacusrex.com/minimapeers.txt";
    //private static String DEFAULT_PEER = "10.0.2.2:12001";

    public static void setDefaultPeers(Context zContext, String zDefaultPeers){
        SharedPreferences prefs = zContext.getSharedPreferences("main_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("default_peers", zDefaultPeers);
        editor.commit();
    }

    public static String getDefaultPeers(Context zContext){
        SharedPreferences prefs = zContext.getSharedPreferences("main_prefs", Context.MODE_PRIVATE);
        return prefs.getString("default_peers",DEFAULT_PEER);
    }
}
