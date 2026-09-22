package org.minimarex.minimacore.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.caverock.androidsvg.SVG;

import java.io.InputStream;
import java.util.Base64;
import java.util.Hashtable;

public class ImageDownloader {

    static ImageDownloader IMAGE_DOWNLOADER = null;
    public static void createImageDownloader(Context zContext){
        IMAGE_DOWNLOADER = new ImageDownloader(zContext);
    }
    public static ImageDownloader getimageDownloader(){
        return IMAGE_DOWNLOADER;
    }

    public static Hashtable<String, Bitmap> PREVIOUS_DOWNLOADS;

    Context mContext;

    public ImageDownloader(Context zContext){
        PREVIOUS_DOWNLOADS = new Hashtable<>();

        mContext = zContext;
    }

    public void downloadImage(String zUrl, ImageView zImageView){

        if(!zUrl.endsWith(".svg") && !zUrl.startsWith("<artimage>")){

            //It's a regular image - Load on this thread
            Glide.with(mContext)
                    .load(zUrl)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    //.placeholder(R.drawable.ic_launcher_background)
                    //.error(R.drawable.broken_image)
                    .into(zImageView);

            return;
        }

        //Run in synchronised separate thread
        Runnable download = new Runnable() {
            @Override
            public void run() {
                loadBackground(zUrl,zImageView);
            }
        };

        Thread tt = new Thread(download);
        tt.start();
    }

    private synchronized void loadBackground(String zURL, ImageView zImage) {

        //First check if we have it..
        if(PREVIOUS_DOWNLOADS.containsKey(zURL)){

            //Get it..
            Bitmap bmp = PREVIOUS_DOWNLOADS.get(zURL);

            //Set it..
            setImage(zImage,bmp);

            return;
        }

        logger.log("Load Image : "+zURL);

        //Is it embedded
        if (zURL.startsWith("<artimage>")){

            //Remove front and back..
            String img = zURL.substring(10,zURL.length()-11);

            //Convert to
            byte[] b64 = Base64.getDecoder().decode(img);

            Bitmap bmp = BitmapFactory.decodeByteArray(b64,0,b64.length);

            //Store it..
            PREVIOUS_DOWNLOADS.put(zURL, bmp);

            setImage(zImage,bmp);

            return;
        }

        //Open stream
        try {

            InputStream in = new java.net.URL(zURL).openStream();

            // Read an SVG from the assets folder
            SVG svg = SVG.getFromInputStream(in);

            Bitmap newBM = Bitmap.createBitmap( (int) Math.ceil(128),
                                                (int) Math.ceil(128),
                                                Bitmap.Config.ARGB_8888);

            Canvas bmcanvas = new Canvas(newBM);

            // Clear background to white
            bmcanvas.drawRGB(255, 255, 255);

            // Render our document onto our canvas
            svg.renderToCanvas(bmcanvas);

            //Store it..
            PREVIOUS_DOWNLOADS.put(zURL, newBM);

            //Set it..
            setImage(zImage,newBM);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setImage(ImageView zImageView, Bitmap zBitmap){
        zImageView.post(new Runnable() {
            @Override
            public void run() {
                zImageView.setImageBitmap(zBitmap);
            }
        });
    }
}
