package org.minimarex.minimacore.main.views.apps;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.receiver.ReceiverDB;
import org.minimarex.minimacore.service.MinimaService;
import org.minimarex.minimacore.utils.TokenUtils;
import org.minimarex.minimacore.utils.logger;

import java.util.Date;

public class AppsAdapter extends BaseAdapter {

    Context mContext;

    private LayoutInflater inflater = null;

    ReceiverDB mDatabase;

    JSONArray mCurrentValues = new JSONArray();

    public AppsAdapter(Context zContext){
        super();
        mContext = zContext;
        inflater = (LayoutInflater) zContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setDatabase(ReceiverDB zDatabase){
        mDatabase = zDatabase;
    }

    public ReceiverDB getDatabase(){
        return mDatabase;
    }

    public void updateValues(){
        mCurrentValues = mDatabase.selectAllApps();
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
            row = inflater.inflate(R.layout.view_apps_row, null);
        }

        ImageView img         = row.findViewById(R.id.apps_icon);
        TextView appname      = row.findViewById(R.id.apps_name);
        TextView apppackage  = row.findViewById(R.id.apps_package);
        TextView appenabled   = row.findViewById(R.id.apps_enabled);
        TextView applastused   = row.findViewById(R.id.apps_lastused);

        //Get the app..
        JSONObject app = (JSONObject) mCurrentValues.get(position);

        String packageclass = (String)app.get("package");
        apppackage.setText(packageclass);

        //Get the ICON
        try{
            PackageManager pkgmanager = mContext.getPackageManager();

            ApplicationInfo appinfo = pkgmanager.getApplicationInfo(packageclass, 0);

            String name     = pkgmanager.getApplicationLabel(appinfo).toString();
            appname.setText(name);

            Drawable icon   = pkgmanager.getApplicationIcon(appinfo);
            img.setImageDrawable(icon);

        }catch (PackageManager.NameNotFoundException e){
            logger.log("Package not found.. "+packageclass+" "+e);
        }

        String admin = "";
        if((int)app.get("admin")==1){
            admin = " [ADMIN]";
        }

        if((int)app.get("penabled")==0){
            appenabled.setText("Disabled"+admin);
        }else{
            appenabled.setText("Enabled"+admin);
        }

        //The date
        long timemilli  = (long)app.get("lastused");
        String date     = MinimaService.DATEFORMAT.format(new Date(timemilli));
        applastused.setText(date);

        return row;
    }
}
