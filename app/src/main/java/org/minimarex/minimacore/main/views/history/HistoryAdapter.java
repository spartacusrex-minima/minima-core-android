package org.minimarex.minimacore.main.views.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.minima.utils.json.JSONArray;
import org.minimarex.minimacore.receiver.ReceiverDB;

public class HistoryAdapter extends BaseAdapter  {

    private LayoutInflater inflater = null;

    JSONArray mCurrentValues = new JSONArray();

    public HistoryAdapter(){

    }

    @Override
    public int getCount() {
        return mCurrentValues.size();
    }

    @Override
    public Object getItem(int position) {
        return mCurrentValues.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }
}
