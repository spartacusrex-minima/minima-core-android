package org.minimarex.minimacore.main;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.minimarex.minimacore.main.views.balance.BalanceView;
import org.minimarex.minimacore.main.views.home.HomeView;
import org.minimarex.minimacore.main.views.receive.ReceiveView;
import org.minimarex.minimacore.main.views.send.SendView;
import org.minimarex.minimacore.main.views.terminal.TerminalView;

public class MainAdapter extends androidx.viewpager.widget.PagerAdapter {

    MainActivity mActivity;

    HomeView mHomeView;
    BalanceView mBalanceView;

    SendView mSendView;

    ReceiveView mReceiveView;
    TerminalView mTerminalView;

    BaseView[] mAllViews;

    public MainAdapter(MainActivity zContext){
        mActivity = zContext;

        mHomeView       = new HomeView(zContext);
        mBalanceView    = new BalanceView(zContext);
        mSendView       = new SendView(zContext);
        mReceiveView    = new ReceiveView(zContext);
        mTerminalView   = new TerminalView(zContext);

        mAllViews = new BaseView[4];
        mAllViews[0] = mHomeView;
        mAllViews[1] = mBalanceView;
        mAllViews[2] = mSendView;
        mAllViews[3] = mReceiveView;
//        mAllViews[4] = mTerminalView;
    }

    public BaseView getBaseView(int zPos){
        return mAllViews[zPos];
    }

    public HomeView getHomeView(){
        return mHomeView;
    }

    public BalanceView getBalanceView(){
        return mBalanceView;
    }

    @Override
    public int getCount() {
        return mAllViews.length;
    }

    @Override
    public Object instantiateItem(final ViewGroup container, int position) {

        //Get the View
        BaseView baseview = mAllViews[position];

        //Add and remove
        container.removeView(baseview.getMainView());
        container.addView(baseview.getMainView());

        return baseview.getMainView();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return object==view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View)object);
    }

    public void refreshAllViews(){
        for(int i=0;i<mAllViews.length;i++){
            mAllViews[i].refreshView();
        }
    }
}
