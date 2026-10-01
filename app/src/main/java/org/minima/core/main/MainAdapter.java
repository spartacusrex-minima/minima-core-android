package org.minima.core.main;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.minima.core.main.views.apps.AppsView;
import org.minima.core.main.views.balance.BalanceView;
import org.minima.core.main.views.history.HistoryView;
import org.minima.core.main.views.receive.ReceiveView;
import org.minima.core.main.views.send.SendView;

public class MainAdapter extends androidx.viewpager.widget.PagerAdapter {

    MainActivity mActivity;

    BaseView[] mAllViews;

    public static String RECEIVE_ADDRESS = null;

    public MainAdapter(MainActivity zContext){
        mActivity = zContext;

        //Clear receive address
        RECEIVE_ADDRESS = null;

        //Store of all current valid views..
        mAllViews = new BaseView[4];

        //mAllViews[0] = new HomeView(mActivity);
        mAllViews[0] = new BalanceView(mActivity);
        mAllViews[1] = new SendView(mActivity);
        mAllViews[2] = new ReceiveView(mActivity);
        mAllViews[3] = new HistoryView(mActivity);
    }

    public void refreshPagerView(int zPosition){
        mAllViews[zPosition].refreshView();
        mAllViews[zPosition].getMainView().invalidate();
    }

    public AppsView getAppsView(){
        return (AppsView) mAllViews[3];
    }
    public SendView getSendView(){
        return (SendView) mAllViews[1];
    }

    //Refresh the Balance..
    public void refreshHomeView(){
        mAllViews[0].refreshView();
        mAllViews[0].getMainView().invalidate();
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public Object instantiateItem(final ViewGroup container, int position) {
        //Remove if added
        container.removeView(mAllViews[position].getMainView());

        //Add to our view..
        container.addView(mAllViews[position].getMainView());

        return mAllViews[position].getMainView();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return object==view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        //Remove from container
        container.removeView((View)object);
    }

    public void refreshAllViews(){
        for(int i=0;i<mAllViews.length;i++){
            if(mAllViews[i] != null){
                mAllViews[i].refreshView();
                mAllViews[i].getMainView().invalidate();
            }
        }
    }
}
