package org.minimarex.minimacore.main.views.history;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.minima.objects.base.MiniNumber;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.receiver.ReceiverDB;
import org.minimarex.minimacore.service.MinimaService;
import org.minimarex.minimacore.utils.logger;

import java.util.Date;
import java.util.Set;

public class HistoryAdapter extends BaseAdapter {

    Context mContext;

    private static LayoutInflater inflater = null;

    JSONArray mCurrentValues = new JSONArray();

    public HistoryAdapter(Context zContext){
        super();
        mContext = zContext;
        inflater = (LayoutInflater) zContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void updateValues(JSONArray zHistoryDetailValues){
        mCurrentValues = zHistoryDetailValues;
        notifyDataSetChanged();
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
        View row = convertView;
        if(row == null){
            row = inflater.inflate(R.layout.view_history_row, null);
        }

        LinearLayout background = row.findViewById(R.id.history_background);
        TextView nametxt        = row.findViewById(R.id.history_name);
        TextView amounttxt      = row.findViewById(R.id.history_amount);
        TextView tokenidtxt     = row.findViewById(R.id.history_tokenid);

        try{
            //Get the actual history log..
            JSONObject details = (JSONObject) mCurrentValues.get(position);

            JSONObject difference   = (JSONObject) details.get("difference");
            JSONObject tokens       = (JSONObject) details.get("tokens");

            Object[] keys = difference.keySet().toArray();
            int size = keys.length;

            if(size == 1) {
                String tokenid      = (String) keys[0];
                String trimtokenid  = new String(tokenid);

                if(trimtokenid.length()>32){
                    trimtokenid = trimtokenid.substring(0,32)+"..";
                }
                tokenidtxt.setText(trimtokenid);

                String amount = difference.getString(tokenid);
                String tokenname = tokens.getString(tokenid);

                MiniNumber numamount = new MiniNumber(amount);
                if (numamount.isMoreEqual(MiniNumber.ZERO)) {
                    //Receive
                    background.setBackgroundResource(R.drawable.rounded_shape_green);
                } else {
                    //Send
                    background.setBackgroundResource(R.drawable.rounded_shape_red);
                }

                nametxt.setText(tokenname);
                amounttxt.setText(amount);

                //Is it a token create
            }else if((size==2) && difference.containsKey("0xFF")){

                nametxt.setText(tokens.getString("0xFF"));
                amounttxt.setText(difference.getString("0xFF"));

                tokenidtxt.setText("Token Create");
                //Send
                background.setBackgroundResource(R.drawable.rounded_shape_yellow);

            }else{
                //Complicated txn..
                nametxt.setText("Complex Transaction..");

                //Send
                background.setBackgroundResource(R.drawable.rounded_shape_yellow);
            }
        } catch (Exception e) {
            //Hmm..
            nametxt.setText("Error..");
        }

        return row;
    }
}
