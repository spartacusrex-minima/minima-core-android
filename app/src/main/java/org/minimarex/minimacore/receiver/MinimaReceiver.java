package org.minimarex.minimacore.receiver;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import org.minima.Minima;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minimarex.minimaapi.MinimaAPI;
import org.minimarex.minimaapi.MinimaAPILogger;
import org.minimarex.minimaapi.MinimaAPIMessages;
import org.minimarex.minimacore.utils.logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MinimaReceiver extends BroadcastReceiver {

    public static final int MAX_MESSAGE_LEN = 256000;

    //Large responses are written here and handed over as a content:// URI
    public static final String FILE_RESPONSE_AUTHORITY  = "org.minimarex.minimacore.ipcresponses";
    public static final String FILE_RESPONSE_DIR        = "ipcresponses";
    public static final long   FILE_RESPONSE_MAX_AGE_MS = 5 * 60 * 1000;

    private Minima mMinima;

    ReceiverDB mDatabase;

    //Commands run on a single background thread - Android forbids network on the main thread
    //(megammrsync / archive / restoresync open sockets inline) and a slow command must not
    //freeze the UI or the broadcast queue. One thread keeps commands serialised as before.
    ExecutorService mCmdExecutor = Executors.newSingleThreadExecutor();

    public MinimaReceiver(Minima zMinima, Context zContext){
        super();

        //Store for later
        mMinima = zMinima;

        mDatabase = new ReceiverDB(zContext);
        //mDatabase.wipeDB();

        //Clear out any leftover large-response files from a previous run
        pruneResponseFiles(zContext.getApplicationContext(), true);

        MinimaAPILogger.log("MAIN - Started logging:"+MinimaAPI.LOGGING_ENABLED);
    }

    public ReceiverDB getDatabase(){
        return mDatabase;
    }

    public void onDestroy(){
        mCmdExecutor.shutdown();
        mDatabase.close();
    }

    @Override
    public void onReceive(Context zContext, Intent zIntent) {

        //Put the WHOLE thing in a try catch..
        try{
            String action = zIntent.getAction();

            //Get the Extra data - that is ALWAYS Sent
            String frompackage      = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_PACKAGE_CLASS);
            String frompackageuid   = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_APP_UID);
            String minimauid        = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_REGISTER_MINIMAID);
            String responseid       = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_ID);

            if(MinimaAPI.LOGGING_ENABLED){
                MinimaAPILogger.log("MAIN - RECEIVED BROADCAST respID:"+responseid+" frompackage:"+frompackage+" action:"+action);
            }

            //Get the App
            JSONObject app = mDatabase.selectApp(frompackage, frompackageuid, minimauid);

            //Check the message type
            if (Objects.equals(zIntent.getAction(), MinimaAPIMessages.MINIMA_API_REGISTER)) {

                //CHECK AND ADD to DB
                if(app == null){

                    //Add to the database..
                    mDatabase.insertApp(frompackage, frompackageuid, minimauid);

                    //Send response..
                    String basicmessage = getBasicMessage(true, false, false,"Registered OK!");
                    sendResponse(zContext, frompackage, responseid, minimauid, basicmessage);

                }else{

                    boolean enabled = (int)app.get("penabled")==1;
                    boolean admin   = (int)app.get("admin")==1;

                    //Send response..
                    String basicmessage = getBasicMessage(true, enabled, admin, "Already registered..");
                    sendResponse(zContext, frompackage, responseid, minimauid, basicmessage);
                }

            } else if (Objects.equals(zIntent.getAction(), MinimaAPIMessages.MINIMA_API_CMD)) {

                //Does the App exist
                if(app == null){
                    logger.log("UNKNOWN Package for request : " + frompackage);
                    return;
                }

                boolean enabled = (int)app.get("penabled")==1;
                boolean admin   = (int)app.get("admin")==1;

                //Is it enabled..!
                if(!enabled){
                    String basicmessage = getBasicMessage(false, enabled, admin, "Package NOT enabled in Minima-Core!");
                    sendResponse(zContext, frompackage, responseid, minimauid, basicmessage);
                    return;
                }

                //Is it an ADMIN user
                String Userid = "0xFF";
                if(admin){
                    Userid = "0x00";
                }

                //Update last used
                mDatabase.updateLastUsed(frompackage, frompackageuid);

                String cmd      = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_CMD_ACTION);

                //Can the caller consume a content:// file for an oversized result..
                boolean fileresp = zIntent.getBooleanExtra(MinimaAPIMessages.MINIMA_API_CMD_FILERESP, false);

                //Check size.. is JSON format so will be longer than normal HEX
                //Need to be able to sign transactions etc..
                if(cmd.length() > MAX_MESSAGE_LEN){
                    String basicmessage = getBasicMessage(false, enabled, admin, "Command too long! MAX("+MAX_MESSAGE_LEN+")");
                    sendResponse(zContext, frompackage, responseid, minimauid, basicmessage);
                    return;
                }

                //Run the command OFF the broadcast (main) thread and respond from there
                final Context appcontext = zContext.getApplicationContext();
                final String  userid     = Userid;
                final boolean fenabled   = enabled;
                final boolean fadmin     = admin;
                final String  fpackage   = frompackage;
                final String  fresponse  = responseid;
                final String  fminimauid = minimauid;
                final boolean ffileresp  = fileresp;

                mCmdExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            //Run the command
                            String result = mMinima.runMinimaCMD(cmd, false, userid);

                            //Check the result is within acceptable parameters
                            if(result.length() > MAX_MESSAGE_LEN){

                                //New clients get the payload as a content:// file - old clients get the stub
                                if(ffileresp && sendFileResponse(appcontext, fpackage, fresponse, fminimauid, result)){
                                    return;
                                }

                                String basicmessage = getBasicMessage(false, fenabled, fadmin, "Result too long! MAX("+MAX_MESSAGE_LEN+")");
                                sendResponse(appcontext, fpackage, fresponse, fminimauid, basicmessage);
                                return;
                            }

                            //Send it back..
                            sendResponse(appcontext, fpackage, fresponse, fminimauid, result);

                        }catch(Exception exc){
                            MinimaAPILogger.log("ERROR MinimaReceive CMD :"+exc.toString());

                            //Tell the caller rather than go silent
                            try{
                                String basicmessage = getBasicMessage(false, fenabled, fadmin, "Command failed : "+exc);
                                sendResponse(appcontext, fpackage, fresponse, fminimauid, basicmessage);
                            }catch(Exception ignore){}
                        }
                    }
                });

            } else if (Objects.equals(zIntent.getAction(), MinimaAPIMessages.MINIMA_API_FILE)) {

                //Does the App exist
                if(app == null){
                    logger.log("UNKNOWN Package for FILE request : " + frompackage);
                    return;
                }

                boolean enabled = (int)app.get("penabled")==1;
                boolean admin   = (int)app.get("admin")==1;

                //Is it enabled..!
                if(!enabled){
                    String basicmessage = getBasicMessage(false, enabled, admin, "Package NOT enabled in Minima-Core!");
                    sendResponse(zContext, frompackage, responseid, minimauid, basicmessage);
                    return;
                }

                //File access can exfiltrate a backup - ADMIN only
                if(!admin){
                    String basicmessage = getBasicMessage(false, enabled, admin, "File access needs ADMIN - enable it for this app in Minima Core -> Apps");
                    sendResponse(zContext, frompackage, responseid, minimauid, basicmessage);
                    return;
                }

                //Update last used
                mDatabase.updateLastUsed(frompackage, frompackageuid);

                final Context appcontext    = zContext.getApplicationContext();
                final String  fpackage      = frompackage;
                final String  fresponse     = responseid;
                final String  fminimauid    = minimauid;
                final String  faction       = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_FILE_ACTION);
                final String  fpath         = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_FILE_PATH);
                final String  fnewpath      = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_FILE_NEWPATH);
                final String  furi          = zIntent.getStringExtra(MinimaAPIMessages.MINIMA_API_FILE_URI);
                final boolean ffileresp     = zIntent.getBooleanExtra(MinimaAPIMessages.MINIMA_API_CMD_FILERESP, false);

                mCmdExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        String result;
                        try{
                            result = runFileAction(appcontext, fpackage, faction, fpath, fnewpath, furi);
                        }catch(Exception exc){
                            MinimaAPILogger.log("ERROR MinimaReceive FILE :"+exc);
                            result = fileError("File action failed : "+exc.getMessage());
                        }

                        try{
                            if(result.length() > MAX_MESSAGE_LEN){
                                if(ffileresp && sendFileResponse(appcontext, fpackage, fresponse, fminimauid, result)){
                                    return;
                                }
                                result = fileError("Result too long! MAX("+MAX_MESSAGE_LEN+")");
                            }
                            sendResponse(appcontext, fpackage, fresponse, fminimauid, result);
                        }catch(Exception ignore){}
                    }
                });
            }

        }catch(Exception exc){
            MinimaAPILogger.log("ERROR MinimaReceive :"+exc.toString());
        }
    }

    /**
     * FILE BRIDGE
     *
     * Import/export files between the node's base folder (getFilesDir - where backup /
     * archive export / txnexport land) and an ADMIN-approved companion app.
     *
     * Every path is resolved canonically and MUST stay inside the base folder. Writes
     * (put/mkdir/move/delete) additionally refuse the live "databases" folder - a file
     * manager must never be able to corrupt the running wallet/chain DBs. Reads are
     * allowed everywhere (an admin app can run `backup` and read everything anyway).
     */
    private static final String[] WRITE_PROTECTED_DIRS = {"databases"};

    private String runFileAction(Context zContext, String zPackage, String zAction, String zPath, String zNewPath, String zUriStr) throws IOException {

        if(zAction == null){
            return fileError("Missing file action");
        }

        File base = zContext.getFilesDir();

        switch(zAction){

            case "list" : {
                File dir = resolveInBase(base, zPath);
                if(!dir.isDirectory()){
                    return fileError("Not a directory : "+zPath);
                }

                JSONArray arr = new JSONArray();
                File[] files = dir.listFiles();
                if(files != null){
                    for(File f : files){
                        JSONObject entry = new JSONObject();
                        entry.put("name", f.getName());
                        entry.put("isdir", f.isDirectory());
                        entry.put("size", f.isDirectory() ? 0L : f.length());
                        entry.put("modified", f.lastModified());
                        arr.add(entry);
                    }
                }

                JSONObject resp = fileOK("list");
                resp.put("path", relPath(base, dir));
                //The node's OWN canonical base path - clients must build terminal paths from
                //this, never reconstruct it from other commands
                resp.put("base", base.getCanonicalPath());
                resp.put("list", arr);
                return resp.toString();
            }

            //Resolve a RAW terminal-style path exactly like MiniFile.createBaseFile does and
            //report what the node actually sees - the ground truth for "file doesn't exist"
            case "stat" : {
                String raw = zPath == null ? "" : zPath;
                File f;
                if(raw.contains(File.separator) || raw.contains("\\") || raw.contains("/")){
                    f = new File(raw);
                }else{
                    f = new File(base, raw);
                }
                JSONObject resp = fileOK("stat");
                resp.put("query", raw);
                resp.put("exists", f.exists());
                resp.put("isdir", f.isDirectory());
                resp.put("size", f.isFile() ? f.length() : 0L);
                resp.put("abspath", f.getAbsolutePath());
                try{ resp.put("canonical", f.getCanonicalPath()); }catch(Exception ignore){}
                resp.put("base", base.getCanonicalPath());

                //When the file is missing, show what IS there - catches invisible
                //name mismatches that are impossible to spot by eye
                if(!f.exists()){
                    File parent = f.getParentFile();
                    resp.put("parentexists", parent != null && parent.isDirectory());
                    if(parent != null && parent.isDirectory()){
                        JSONArray kids = new JSONArray();
                        File[] children = parent.listFiles();
                        if(children != null){
                            int n = 0;
                            for(File k : children){
                                kids.add(k.getName());
                                if(++n >= 50) break;
                            }
                        }
                        resp.put("parentlist", kids);
                    }
                }
                return resp.toString();
            }

            case "get" : {
                File file = resolveInBase(base, zPath);
                if(!file.isFile()){
                    return fileError("Not a file : "+zPath);
                }

                //Serve the actual file via the FileProvider files-path root
                Uri uri = FileProvider.getUriForFile(zContext, FILE_RESPONSE_AUTHORITY, file);

                //ONLY the requesting package may read it
                zContext.grantUriPermission(zPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                JSONObject resp = fileOK("get");
                resp.put("path", relPath(base, file));
                resp.put("abspath", file.getCanonicalPath());
                resp.put("name", file.getName());
                resp.put("size", file.length());
                resp.put("modified", file.lastModified());
                resp.put("uri", uri.toString());
                return resp.toString();
            }

            case "put" : {
                if(zUriStr == null){
                    return fileError("put needs a source uri");
                }
                File dest = resolveInBase(base, zPath);
                checkWriteAllowed(base, dest);
                if(dest.isDirectory()){
                    return fileError("Destination is a directory : "+zPath);
                }

                File parent = dest.getParentFile();
                if(parent != null && !parent.exists() && !parent.mkdirs()){
                    return fileError("Could not create parent folder");
                }

                //Stream to a temp file first - never leave a half-written file behind
                File part = new File(parent, dest.getName()+".part");
                long total = 0;
                InputStream is = zContext.getContentResolver().openInputStream(Uri.parse(zUriStr));
                if(is == null){
                    return fileError("Could not open source uri");
                }
                try{
                    FileOutputStream fos = new FileOutputStream(part);
                    try{
                        byte[] buf = new byte[65536];
                        int read;
                        while((read = is.read(buf)) != -1){
                            fos.write(buf, 0, read);
                            total += read;
                        }
                    }finally{
                        fos.close();
                    }
                }finally{
                    is.close();
                }

                if(dest.exists() && !dest.delete()){
                    part.delete();
                    return fileError("Could not replace existing file");
                }
                if(!part.renameTo(dest)){
                    part.delete();
                    return fileError("Could not finalise file");
                }

                JSONObject resp = fileOK("put");
                resp.put("path", relPath(base, dest));
                resp.put("size", total);
                return resp.toString();
            }

            case "mkdir" : {
                File dir = resolveInBase(base, zPath);
                checkWriteAllowed(base, dir);
                if(dir.exists()){
                    return fileError("Already exists : "+zPath);
                }
                if(!dir.mkdirs()){
                    return fileError("Could not create folder");
                }
                JSONObject resp = fileOK("mkdir");
                resp.put("path", relPath(base, dir));
                return resp.toString();
            }

            case "move" : {
                File from = resolveInBase(base, zPath);
                File to   = resolveInBase(base, zNewPath);
                checkWriteAllowed(base, from);
                checkWriteAllowed(base, to);
                if(!from.exists()){
                    return fileError("Not found : "+zPath);
                }
                if(to.exists()){
                    return fileError("Destination already exists : "+zNewPath);
                }
                File parent = to.getParentFile();
                if(parent != null && !parent.exists() && !parent.mkdirs()){
                    return fileError("Could not create parent folder");
                }
                if(!from.renameTo(to)){
                    return fileError("Could not move");
                }
                JSONObject resp = fileOK("move");
                resp.put("path", relPath(base, to));
                return resp.toString();
            }

            case "delete" : {
                File file = resolveInBase(base, zPath);
                checkWriteAllowed(base, file);
                if(file.getCanonicalPath().equals(base.getCanonicalPath())){
                    return fileError("Cannot delete the base folder");
                }
                if(!file.exists()){
                    return fileError("Not found : "+zPath);
                }
                deleteRecursive(file);
                if(file.exists()){
                    return fileError("Could not delete");
                }
                JSONObject resp = fileOK("delete");
                resp.put("path", zPath);
                return resp.toString();
            }

            default :
                return fileError("Unknown file action : "+zAction);
        }
    }

    /** Resolve a caller-supplied relative path and REFUSE anything outside the base folder. */
    private File resolveInBase(File zBase, String zRel) throws IOException {
        String rel = zRel == null ? "/" : zRel;
        File f = new File(zBase, rel).getCanonicalFile();
        String basepath = zBase.getCanonicalPath();
        if(!f.getPath().equals(basepath) && !f.getPath().startsWith(basepath + File.separator)){
            throw new IOException("Path outside base folder");
        }
        return f;
    }

    /** Writes must never touch the node's live databases. */
    private void checkWriteAllowed(File zBase, File zTarget) throws IOException {
        String basepath = zBase.getCanonicalPath();
        String rel = zTarget.getCanonicalPath().substring(Math.min(basepath.length(), zTarget.getCanonicalPath().length()));
        while(rel.startsWith(File.separator)){
            rel = rel.substring(1);
        }
        String first = rel.contains(File.separator) ? rel.substring(0, rel.indexOf(File.separator)) : rel;
        for(String prot : WRITE_PROTECTED_DIRS){
            if(first.equals(prot)){
                throw new IOException("'"+prot+"' is protected - the node's live data cannot be modified");
            }
        }
    }

    private void deleteRecursive(File zFile){
        if(zFile.isDirectory()){
            File[] children = zFile.listFiles();
            if(children != null){
                for(File c : children){
                    deleteRecursive(c);
                }
            }
        }
        zFile.delete();
    }

    private String relPath(File zBase, File zFile) throws IOException {
        String basepath = zBase.getCanonicalPath();
        String path = zFile.getCanonicalPath();
        if(path.equals(basepath)){
            return "/";
        }
        return path.substring(basepath.length()).replace(File.separatorChar, '/');
    }

    private JSONObject fileOK(String zAction){
        JSONObject ret = new JSONObject();
        ret.put("status", true);
        ret.put("action", zAction);
        return ret;
    }

    private String fileError(String zMessage){
        JSONObject ret = new JSONObject();
        ret.put("status", false);
        ret.put("error", zMessage);
        return ret.toString();
    }

    public void sendResponse(Context zContext, String zPackage, String zResponseID, String zMinimaID, String zResponse){

        if(MinimaAPI.LOGGING_ENABLED){
            MinimaAPILogger.log("MAIN - SEND BROADCAST respID:"+zResponseID+" resp:"+zResponse);
        }

        //Create the Response intent
        Intent intent = new Intent(MinimaAPIMessages.MINIMA_API_RESPONSE);

        //The MinimaID they expect
        intent.putExtra(MinimaAPIMessages.MINIMA_API_REGISTER_MINIMAID, zMinimaID);

        //The ResponseID they expect
        intent.putExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_ID, zResponseID);

        //The REsponse Data
        intent.putExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_RESULT, zResponse);

        //Set to send ONLY back to original sender
        intent.setPackage(zPackage);

        //And broadcast
        zContext.sendBroadcast(intent);
    }

    /**
     * Hand an oversized result to the caller as a content:// file.
     *
     * The payload cannot travel as an Intent extra (Android Binder ~1MB transaction limit,
     * exceeding it kills the receiving process with an uncatchable TransactionTooLargeException)
     * so it is written to cache and ONLY the requesting package is granted read on the URI.
     *
     * @return true if the file response was sent - false means fall back to the stub
     */
    public boolean sendFileResponse(Context zContext, String zPackage, String zResponseID, String zMinimaID, String zResponse){

        try{
            //Housekeeping first - never let the cache grow
            pruneResponseFiles(zContext, false);

            File dir = new File(zContext.getCacheDir(), FILE_RESPONSE_DIR);
            if(!dir.exists() && !dir.mkdirs()){
                MinimaAPILogger.log("ERROR sendFileResponse : could not create "+dir);
                return false;
            }

            //Response id is client-supplied - sanitise it before using as a filename
            String safeid = zResponseID == null ? "" : zResponseID.replaceAll("[^0-9a-zA-Zx]", "");
            File respfile = new File(dir, "resp_"+safeid+"_"+System.nanoTime()+".json");

            FileOutputStream fos = new FileOutputStream(respfile);
            try{
                fos.write(zResponse.getBytes(StandardCharsets.UTF_8));
            }finally{
                fos.close();
            }

            //content:// URI via the (non-exported) FileProvider
            Uri uri = FileProvider.getUriForFile(zContext, FILE_RESPONSE_AUTHORITY, respfile);

            //Explicit per-package grant - extras do NOT auto-grant
            zContext.grantUriPermission(zPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            //Create the Response intent - same shape as sendResponse but with a URI payload
            Intent intent = new Intent(MinimaAPIMessages.MINIMA_API_RESPONSE);
            intent.putExtra(MinimaAPIMessages.MINIMA_API_REGISTER_MINIMAID, zMinimaID);
            intent.putExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_ID, zResponseID);
            intent.putExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_URI, uri.toString());
            intent.putExtra(MinimaAPIMessages.MINIMA_API_RESPONSE_LEN, (long)zResponse.length());

            //Belt and braces - ClipData grant travels with the Intent on newer Android
            intent.setClipData(ClipData.newRawUri("minima_response", uri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            //Set to send ONLY back to original sender
            intent.setPackage(zPackage);

            if(MinimaAPI.LOGGING_ENABLED){
                MinimaAPILogger.log("MAIN - SEND FILE BROADCAST respID:"+zResponseID+" len:"+zResponse.length()+" uri:"+uri);
            }

            //And broadcast
            zContext.sendBroadcast(intent);

            return true;

        }catch(Exception exc){
            MinimaAPILogger.log("ERROR sendFileResponse : "+exc);
            return false;
        }
    }

    /**
     * Delete old large-response files (and revoke their URI grants).
     * @param zAll true wipes everything (startup), false only files past FILE_RESPONSE_MAX_AGE_MS
     */
    private void pruneResponseFiles(Context zContext, boolean zAll){
        try{
            File dir = new File(zContext.getCacheDir(), FILE_RESPONSE_DIR);
            File[] files = dir.listFiles();
            if(files == null){
                return;
            }

            long now = System.currentTimeMillis();
            for(File f : files){
                if(zAll || (now - f.lastModified()) > FILE_RESPONSE_MAX_AGE_MS){
                    try{
                        Uri uri = FileProvider.getUriForFile(zContext, FILE_RESPONSE_AUTHORITY, f);
                        zContext.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }catch(Exception ignore){}
                    f.delete();
                }
            }
        }catch(Exception exc){
            MinimaAPILogger.log("ERROR pruneResponseFiles : "+exc);
        }
    }

    public void sendNotify(Context zContext, String zPackage, String zMinimaID, String zNotifyMessage){

        //Create the Response intent
        Intent intent = new Intent(MinimaAPIMessages.MINIMA_API_NOTIFY);

        //The MinimaID they expect
        intent.putExtra(MinimaAPIMessages.MINIMA_API_REGISTER_MINIMAID, zMinimaID);

        //The Notify Data
        intent.putExtra(MinimaAPIMessages.MINIMA_API_NOTIFY_DATA, zNotifyMessage);

        //Set to send ONLY back to original sender
        intent.setPackage(zPackage);

        //And broadcast
        zContext.sendBroadcast(intent);
    }

    private String getBasicMessage(boolean zStatus, boolean zEnabled, boolean zAdmin, String zMessage){
        JSONObject ret = new JSONObject();
        ret.put("status",zStatus);
        ret.put("enabled",zEnabled);
        ret.put("admin",zAdmin);
        ret.put("response",zMessage);
        return ret.toString();
    }
}
