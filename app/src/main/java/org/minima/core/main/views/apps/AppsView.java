package org.minima.core.main.views.apps;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import org.minima.utils.json.JSONObject;
import org.minima.core.R;
import org.minima.core.main.BaseView;
import org.minima.core.receiver.ReceiverDB;
import org.minima.core.utils.logger;

public class AppsView extends BaseView {

    ListView mAppsList;
    AppsAdapter mAppsAdapter;

    public AppsView(Activity zActivity){
        super(zActivity, R.layout.view_apps);

        mAppsAdapter = new AppsAdapter(zActivity);

        mAppsList = getMainView().findViewById(R.id.wallet_apps_list);
        mAppsList.setAdapter(mAppsAdapter);

        mAppsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Get the selecetd app
                JSONObject app = (JSONObject) mAppsAdapter.getItem(position);

                showInfoDialog(app);
            }
        });
    }

    public void showInfoDialog(JSONObject zApp){

        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(mActivity.LAYOUT_INFLATER_SERVICE);
        View dialogview = inflater.inflate(R.layout.dialog_apps_info, null);

        final String packageclass   = (String)zApp.get("package");
        final String packageid      = (String)zApp.get("packageid");
        TextView packagec   = dialogview.findViewById(R.id.app_info_package);
        packagec.setText(packageclass);

        //TextView name       = dialogview.findViewById(R.id.app_info_name);
        //name.setText("not found..");

        final CheckBox cb = dialogview.findViewById(R.id.app_info_enabled);
        if((int)zApp.get("penabled") == 1){
            cb.setChecked(true);
        }

        cb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAppsAdapter.getDatabase().setEnabled(packageclass, packageid, cb.isChecked());
            }
        });

        final CheckBox admincb = dialogview.findViewById(R.id.app_info_admin);
        if((int)zApp.get("admin") == 1){
            admincb.setChecked(true);
        }

        admincb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAppsAdapter.getDatabase().setAdmin(packageclass, packageid, admincb.isChecked());
            }
        });

        //Get the ICON
        Drawable icon = null;
        String   pname = "";
        try{
            PackageManager pkgmanager = mActivity.getPackageManager();

            ApplicationInfo appinfo = pkgmanager.getApplicationInfo(packageclass, 0);

            pname     = pkgmanager.getApplicationLabel(appinfo).toString();
            icon = pkgmanager.getApplicationIcon(appinfo);

        }catch (PackageManager.NameNotFoundException e){
            logger.log("Package not found.. "+packageclass+" "+e);
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(pname)
                .setView(dialogview)
                .setIcon(icon)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int whichButton) {
                        AppsView.this.refreshView();
                    }})
                .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAppsAdapter.getDatabase().delete(packageclass, packageid);
                        AppsView.this.refreshView();
                    }
                }).show();

    }

    public void setDatabase(ReceiverDB zDB){
        mAppsAdapter.setDatabase(zDB);
    }

    @Override
    public void refreshView() {
        mAppsList.post(new Runnable() {
            @Override
            public void run() {
                mAppsAdapter.updateValues();
                mAppsList.invalidate();
            }
        });
    }
}
