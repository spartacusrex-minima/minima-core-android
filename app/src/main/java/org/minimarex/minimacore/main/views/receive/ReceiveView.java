package org.minimarex.minimacore.main.views.receive;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.minima.utils.json.JSONObject;
import org.minimarex.minimacore.R;
import org.minimarex.minimacore.utils.MinimaCMD;
import org.minimarex.minimacore.utils.MinimaCMDListener;
import org.minimarex.minimacore.main.BaseView;

public class ReceiveView extends BaseView {

    TextView mAddressText;

    ImageView mQRCodeAddress;

    Button mChangeButton;

    boolean mChangeInit = false;
    public ReceiveView(Activity zActivity){
        super(zActivity, R.layout.view_wallet_receive);

        mQRCodeAddress = getMainView().findViewById(R.id.wallet_receive_qrcode);

        mAddressText = getMainView().findViewById(R.id.wallet_receive_address);

        mChangeButton = getMainView().findViewById(R.id.wallet_receive_changeaddress);
        mChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeAddress();
            }
        });

    }

    public void changeAddress(){

        mChangeButton.post(new Runnable() {
            @Override
            public void run() {
                mChangeButton.setEnabled(false);
            }
        });

        MinimaCMD.runMinima("getAddress", new MinimaCMDListener() {
            @Override
            public void cmdResult(JSONObject zResult) {
                JSONObject response = (JSONObject)zResult.get("response");
                String address      = response.get("miniaddress").toString();
                updateAddress(address);
            }
        });
    }

    protected void updateAddress(String zAddress){

        mAddressText.post(new Runnable() {
            @Override
            public void run() {
                try {
                    BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                    Bitmap bitmap = barcodeEncoder.encodeBitmap(zAddress, BarcodeFormat.QR_CODE, 400, 400);
                    mQRCodeAddress.setImageBitmap(bitmap);

                    mChangeInit = true;

                } catch(Exception e) {

                }

                mAddressText.setText(zAddress);
            }
        });

        mChangeButton.post(new Runnable() {
            @Override
            public void run() {
                mChangeButton.setEnabled(true);
            }
        });
    }

    @Override
    public void refreshView() {
        //Have we started
        if(!MinimaCMD.checkMinimaStarted()){
            return;
        }

        if(!mChangeInit){
            changeAddress();
        }
    }
}
