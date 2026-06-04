package org.minimarex.minimacore.main;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import org.minimarex.minimacore.utils.logger;

public class BaseView {

    protected Activity mActivity;

    protected View mMainView;

    public BaseView(Activity zActivity, int zViewResID){
        mActivity = zActivity;

        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(mActivity.LAYOUT_INFLATER_SERVICE);
        mMainView = inflater.inflate(zViewResID, null);
    }

    public Activity getActivity(){
        return mActivity;
    }

    public View getMainView(){
        return mMainView;
    }

    public void refreshView(){
        logger.log("BASE VIEW REFRESH..!");
    }
}
