package org.minimarex.minimacore.main.views.history;

import android.app.Activity;
import android.widget.ListView;

import org.minimarex.minimacore.R;
import org.minimarex.minimacore.main.BaseView;
import org.minimarex.minimacore.main.views.apps.AppsAdapter;

public class HistoryView extends BaseView {

    ListView mHistoryList;
    HistoryAdapter mHistoryAdapter;

    public HistoryView(Activity zActivity) {
        super(zActivity, R.layout.view_history);




    }

    @Override
    public void refreshView() {
        /*mHistoryList.post(new Runnable() {
            @Override
            public void run() {

                //mAppsAdapter.updateValues();
                //mAppsList.invalidate();
            }
        });*/
    }
}
