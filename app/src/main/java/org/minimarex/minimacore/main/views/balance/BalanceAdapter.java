package org.minimarex.minimacore.main.views.balance;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.utils.TokenUtils;
import org.minimarex.minimacore.utils.logger;

public class BalanceAdapter extends BaseAdapter {

    Context mContext;

    String[] mText;

    private static LayoutInflater inflater = null;

    JSONArray mCurrentBalance = new JSONArray();

    public BalanceAdapter(Context zContext, String id){
        super();

        inflater = (LayoutInflater) zContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void updateValues(JSONArray zCurrentBalance){
        mCurrentBalance = zCurrentBalance;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mCurrentBalance.size();
    }

    @Override
    public Object getItem(int position) {
        return mCurrentBalance.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if(row == null){
            row = inflater.inflate(R.layout.view_balance_row, null);
        }

        TextView tokenname      = row.findViewById(R.id.balance_tokenname);
        TextView tokenamount    = row.findViewById(R.id.balance_tokenamount);
        TextView tokenid        = row.findViewById(R.id.balance_tokenid);

        //Get the balance..
        JSONObject bal = (JSONObject) mCurrentBalance.get(position);

        //Token ID
        String id = bal.get("tokenid").toString();
        tokenid.setText(id);

        //Token name
        String name = TokenUtils.getTokenName(bal);
        tokenname.setText(name);

        //Amount
        String confirmed    = bal.get("confirmed").toString();
        String unconfirmed  = bal.get("unconfirmed").toString();
        if(confirmed.length() > 12){
            confirmed = confirmed.substring(0,12)+"..";
        }
        if(unconfirmed.length() > 12){
            unconfirmed = unconfirmed.substring(0,12)+"..";
        }
        if(unconfirmed.equals("0")){
            tokenamount.setText(confirmed);
        }else{
            tokenamount.setText(confirmed+"("+unconfirmed+")");
        }

        return row;
    }
}
