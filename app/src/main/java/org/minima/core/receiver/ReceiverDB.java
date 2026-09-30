package org.minima.core.receiver;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;

public class ReceiverDB extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "receive.db";

    //The Handle to the DB
    SQLiteDatabase mDB;

    //All the Columns
    String[] ALL_COLUMNS = new String[] {"_id","package", "packageid", "minimaid", "penabled", "admin", "lastused"};

    // Database creation sql statement
    private static final String DATABASE_CREATE_APPLICATIONS = "create table if not exists applications ( " +
            "_id integer primary key," +
            "package text not null," +
            "packageid text not null," +
            "minimaid text not null," +
            "penabled integer not null," +
            "admin integer not null," +
            "lastused integer not null" +
            ");";

    public ReceiverDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        //Get handle..
        mDB = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE_APPLICATIONS);
    }

    public void wipeDB(){
        mDB.execSQL("DROP TABLE IF EXISTS applications");
        onCreate(mDB);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        wipeDB();
    }
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        wipeDB();
    }

    public void insertApp(String zPackageName, String zPackageID, String zMinimaID){

        ContentValues values = new ContentValues();
        values.put("package", zPackageName);
        values.put("packageid", zPackageID);
        values.put("minimaid", zMinimaID);
        values.put("penabled", 0);
        values.put("admin", 0);
        values.put("lastused", System.currentTimeMillis());

        mDB.insert("applications", null, values);
    }

    private JSONObject convertCursor(Cursor zCursor){
        JSONObject app = new JSONObject();

        app.put("_id", zCursor.getInt(0));
        app.put("package", zCursor.getString(1));
        app.put("packageid", zCursor.getString(2));
        app.put("minimaid", zCursor.getString(3));
        app.put("penabled", zCursor.getInt(4));
        app.put("admin", zCursor.getInt(5));
        app.put("lastused", zCursor.getLong(6));

        return app;
    }

    public JSONArray selectAllApps() {

        JSONArray results = new JSONArray();

        Cursor cursor = mDB.query(false, "applications", ALL_COLUMNS,null, null, null, null, null, null);
        if (cursor != null) {

            try {
                while (cursor.moveToNext()) {
                    results.add(convertCursor(cursor));
                }
            } finally {
                cursor.close();
            }
        }

        return results;
    }

    public JSONObject selectApp(String zPackageName, String zpackageID, String zMinimaID){

        String[] args = new String[] {zPackageName, zpackageID, zMinimaID};
        Cursor cursor = mDB.query(true, "applications", ALL_COLUMNS,"package=? AND packageid=? AND minimaid=?",
                args, null, null, null, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    return convertCursor(cursor);
                }
            } finally {
                cursor.close();
            }
        }

        return null;
    }

    public boolean exists(String zPackageName, String zPackageID, String zMinimaID){
        return selectApp(zPackageName,zPackageID,zMinimaID) != null;
    }

    public void setEnabled(String zPackageName, String zPackageID, boolean zEnabled){
        ContentValues cv = new ContentValues();
        if(zEnabled){
            cv.put("penabled",1);
        }else{
            cv.put("penabled",0);
        }

        mDB.update("applications", cv, "package=? AND packageid=?", new String[]{zPackageName, zPackageID});
    }

    public void setAdmin(String zPackageName, String zPackageID, boolean zAdmin){
        ContentValues cv = new ContentValues();
        if(zAdmin){
            cv.put("admin",1);
        }else{
            cv.put("admin",0);
        }

        mDB.update("applications", cv, "package=? AND packageid=?", new String[]{zPackageName, zPackageID});
    }

    public void updateLastUsed(String zPackageName, String zPackageID){
        ContentValues cv = new ContentValues();
        cv.put("lastused",System.currentTimeMillis());

        mDB.update("applications", cv, "package=? AND packageid=?", new String[]{zPackageName, zPackageID});
    }

    public void delete(String zPackageName, String zPackageID){
        mDB.delete("applications", "package=? AND packageid=?", new String[]{zPackageName, zPackageID});
    }
}
