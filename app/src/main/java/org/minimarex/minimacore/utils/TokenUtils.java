package org.minimarex.minimacore.utils;

import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

public class TokenUtils {
    public static String getTokenName(JSONObject zToken){
        //Token ID
        String id = zToken.get("tokenid").toString();

        //Token name
        if(id.equals("0x00")){
            return zToken.get("token").toString();
        }else{

            JSONObject tokendetails = null;
            try {
                tokendetails = (JSONObject) new JSONParser().parse(zToken.get("token").toString());
                return tokendetails.get("name").toString();

            } catch (ParseException e) {
                return "Error - no token name";
            }
        }
    }
}
