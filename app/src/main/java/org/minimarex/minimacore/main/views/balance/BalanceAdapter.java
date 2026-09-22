package org.minimarex.minimacore.main.views.balance;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.minima.objects.base.MiniString;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.utils.ImageDownloader;
import org.minimarex.minimacore.utils.TokenUtils;
import org.minimarex.minimacore.utils.logger;

public class BalanceAdapter extends BaseAdapter {

    private static LayoutInflater inflater = null;

    JSONArray mCurrentBalance = new JSONArray();

    Context mContext;

    public BalanceAdapter(Context zContext){
        super();

        mContext = zContext;

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

        //Get the balance..
        JSONObject bal = (JSONObject) mCurrentBalance.get(position);

        TextView tokenname      = row.findViewById(R.id.balance_tokenname);
        TextView tokenamount    = row.findViewById(R.id.balance_tokenamount);

        //Token name
        String name = TokenUtils.getTokenName(bal);
        tokenname.setText(name);

        //Get the tokenid
        String tokenid = bal.getString("tokenid");

        //Load the Icon
        ImageView icon = row.findViewById(R.id.balance_icon);

        if(tokenid.equals("0x00")){

            //Set Minima ICON
            icon.setImageResource(R.drawable.ic_minima);
        }else{

            String tok = bal.get("token").toString();

            try {
                JSONObject tokendetails = (JSONObject) new JSONParser().parse(tok);

//                if(name.equals("popo")) {
//                    String url = "<artimage>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</artimage>";
//                    ImageDownloader.getimageDownloader().downloadImage(url, icon);
//
//                }else
                if(tokendetails.containsKey("url")){

                    //Get the full image
                    String url = tokendetails.getString("url");

                    //Set it
                    ImageDownloader.getimageDownloader().downloadImage(url, icon);

                }else{
                    //No ICON
                    icon.setImageResource(R.drawable.ic_dapps);
                }

            } catch (ParseException e) {
                //throw new RuntimeException(e);
            }
        }

        //Amount
        String confirmed    = bal.get("confirmed").toString();
        String unconfirmed  = bal.get("unconfirmed").toString();

        if(confirmed.length() > 21){
            confirmed = confirmed.substring(0,21)+"..";
        }

        if(unconfirmed.length() > 12){
            unconfirmed = unconfirmed.substring(0,12)+"..";
        }
        if(unconfirmed.equals("0")){
            tokenamount.setText(confirmed);
        }else{
            if(confirmed.length() > 12){
                confirmed = confirmed.substring(0,12)+"..";
            }
            tokenamount.setText(confirmed+"("+unconfirmed+")");
        }

        return row;
    }
}
