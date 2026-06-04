package org.minimarex.minimacore.main.views.send;

import android.content.Context;
import android.widget.ArrayAdapter;

import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;
import org.minimarex.minimacore.R;

import java.util.ArrayList;

public class TokenSpinnerAdapter extends ArrayAdapter<String> {

    JSONArray mBalance;

    public TokenSpinnerAdapter(Context zContext){
        super(zContext, R.layout.send_spinner_item, R.id.send_spinner_text, new ArrayList<>());
    }

    public void updateTokens(JSONArray zBalance){
        //Remove the old
        clear();

        mBalance = zBalance;

        //Get a string list
        ArrayList<String> tokens = new ArrayList<>();
        for(int i=0;i<mBalance.size();i++){
            JSONObject bal = (JSONObject) mBalance.get(i);

            String tokenid = bal.getString("tokenid").toString();
            if(tokenid.equals("0x00")){
                tokens.add("Minima");
            }else{
                String fullname = bal.get("token").toString();

                try {
                    JSONObject namejson = (JSONObject) new JSONParser().parse(fullname);
                    tokens.add(namejson.getString("name").toString());

                } catch (ParseException e) {
                    tokens.add(fullname);
                }
            }
        }

        //Add the new
        addAll(tokens);

        notifyDataSetChanged();
    }

    public JSONObject getToken(int zPosition){
        JSONObject bal = (JSONObject) mBalance.get(zPosition);
        return bal;
    }
}
