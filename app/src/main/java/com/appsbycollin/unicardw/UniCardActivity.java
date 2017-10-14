package com.appsbycollin.unicardw;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.EnumMap;
import java.util.Map;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class UniCardActivity extends AppCompatActivity {

    private static final String TAG = "UniCardActivityLog";

    String JSON_String = "";

    private static final String US_TAG = "users";
    private static final String ID_TAG = "ID";
    private static final String FN_TAG = "FirstName";
    private static final String MN_TAG = "MiddleName";
    private static final String LN_TAG = "LastName";
    private static final String ST_TAG = "Status";
    private static final String SD_TAG = "StartDate";
    private static final String LCN_TAG = "LibraryCardNumber"; //JSON Tags for respective fields.

    private static final int BARCODE_HEIGHT = 100;
    private static final int BARCODE_WIDTH = 500;

    private static String strID = "";
    private static String FN = "";
    private static String MN = "";
    private static String LN = "";
    private static String ST = "";
    private static String SD = "";
    private static String LCN = "";

    JSONArray userArray = null;

    private static final int REQUEST_IMAGE_CAPTURE = 11;
    private static final int REQUEST_IC_WES_PERMISSIONS = 44;

    private Intent takePictureIntent = null;
    private AlertDialog cAD, sAD, eAD;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unicard);

        Log.i(TAG, "onCreate Called!!");
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //forces activity to remain in landscape orientation.
        Bundle extraInfo = getIntent().getExtras();

        JSON_String = extraInfo.getString("userDetails");

        Log.i(TAG, "Data @ UniCardActivity: " + JSON_String);

        parseJSON();
        setAllCardText();

        if(!picCheck())
            showCameraDialog();
        else
            loadImage();

        //onClick Listener for (currently invisible) buttonOK
        Button buttonTakePic = (Button) findViewById(R.id.ID_buttonTakePic);
        buttonTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
    }

    //Intent to take picture
    private void dispatchTakePictureIntent() { //need to make this static to call it from a static context.
        takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if(takePictureIntent.resolveActivity(getPackageManager()) != null) { //checks that an activity can handle the intent - prevents crashes
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) { //If permission has not been granted, then...
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_IC_WES_PERMISSIONS); //...request the permission from user.
            }
            else if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) //Permission *already* granted, so...
            {
                Log.i(TAG, "Permissions previously granted.");
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE); //...start activity!
                Log.i(TAG, "Picture is being taken...");
            }
        }
    }

    @Override //this callback is invoked when a result from the above permission request is received. This is where the next step to take is specified.
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_IC_WES_PERMISSIONS)
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) //Permission granted!
            {
                Log.i(TAG, "Permissions just granted.");
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE); //...start activity!
                Log.i(TAG, "Picture is being taken...");
            }
        else showExitDialog();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//            Log.i(TAG, "Picture successfully taken!");

            Bundle extras = data.getExtras();
            Bitmap capturedPic = (Bitmap) extras.get("data");

            saveImage(capturedPic);
        }
    }

    private void saveImage(Bitmap image) {
        File path = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/UniCard");
        //path.mkdirs();

//        String timeStamp = new SimpleDateFormat("dd-MMM-yyyy_HH-mm-ss", Locale.UK).format(new Date());
//        String imageFileName = "ID_" + timeStamp;

        String imageFileName = "UniCardID.png";
        File imageFile  = new File(path, imageFileName);
        try {
            FileOutputStream fOut = new FileOutputStream(imageFile);
            image.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();

            Log.i(TAG, "Picture saved in " + path);
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadImage();
    }

    private void loadImage() {
        String loadPath = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/UniCard/UniCardID.png").toString();
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bmp = BitmapFactory.decodeFile(loadPath, opt);

        int imgWidth = 0;
        int imgHeight = 0;

        ImageView ID_iV = (ImageView) findViewById(R.id.ID_iV_Pic);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {//if in portrait orientation, apply the following settings
            imgWidth = 450;
            imgHeight = 600;
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) { //if in landscape orientation, apply the following settings
            imgWidth = 500;
            imgHeight =700;
        }

        RelativeLayout.LayoutParams ID_Params = new RelativeLayout.LayoutParams(imgWidth, imgHeight); //Image width & height

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {//if in portrait orientation, apply the following settings
            ID_Params.leftMargin = 275;
            ID_Params.topMargin = 550;
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) { //if in landscape orientation, apply the following settings
            ID_Params.leftMargin = 625;
            ID_Params.topMargin = 75;
        }

        ID_iV.setScaleType(ImageView.ScaleType.FIT_XY);
        ID_iV.setLayoutParams(ID_Params);
        ID_iV.setImageBitmap(bmp);

        Log.i(TAG, "Image loaded.");

        NFC_Check("Please visit Settings to enable NFC.");
    }

    protected void setAllCardText() {
        TextView titleID = (TextView) findViewById(R.id.ID_tV_ID);
        if(ST.contains("Staff"))
            titleID.setText(R.string.string_titleIDStaff);

        TextView ID = (TextView) findViewById(R.id.ID_tV_ID_txt);
        ID.setText(strID);

        TextView firstName = (TextView) findViewById(R.id.ID_tV_FirstName);
        firstName.setText(FN);

        TextView midName = (TextView) findViewById(R.id.ID_tV_MidName);
        midName.setText(MN);

        TextView lastName = (TextView) findViewById(R.id.ID_tV_LastName);
        lastName.setText(LN);

        TextView status = (TextView) findViewById(R.id.ID_tV_Status_txt);
        status.setText(ST);

        TextView startDate = (TextView) findViewById(R.id.ID_tv_StartDate_txt);
        startDate.setText(SD);

        TextView libCardNum = (TextView) findViewById(R.id.ID_tV_LCN_txt);
        libCardNum.setText(LCN);

        //Barcode Test
        RelativeLayout RL = (RelativeLayout) findViewById(R.id.activity_unicard);
        String barcodeData = "0" + strID;

        Bitmap bm;
        //ImageView IV = new ImageView(this);
        ImageView IV = (ImageView) findViewById(R.id.ID_IV_BCP);

        try {
            bm = encodeAsBitmap(barcodeData, BarcodeFormat.EAN_8, BARCODE_WIDTH, BARCODE_HEIGHT);
            IV.setImageBitmap(bm);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        RL.removeView(IV);

        RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(BARCODE_WIDTH, BARCODE_HEIGHT);

        layout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layout.addRule(RelativeLayout.CENTER_HORIZONTAL);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
        {//if in portrait orientation, apply the following settings
            layout.bottomMargin = 500;
            layout.rightMargin = 265;
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        { //if in landscape orientation, apply the following settings
            layout.rightMargin = 700;
        }

        IV.setLayoutParams(layout);

        RL.addView(IV);

        //barcode text
//        TextView TV = new TextView(this);
//        TV.setGravity(Gravity.CENTER_HORIZONTAL);
//        TV.setPadding(0, 200, 0, 0);
//        TV.setText(barcodeData);
//        RL.addView(TV);
    }

    protected void parseJSON() {
        try {

            //JSONObject JSON = new JSONObject("{\"users\":" + JSON_String.substring(4) + "}");
            JSONObject JSON = new JSONObject(JSON_String.substring(4)); //first four characters are "TRUE"
            userArray = JSON.getJSONArray(US_TAG);
            JSONObject obj = userArray.getJSONObject(0);

            strID = obj.getString(ID_TAG);
            FN = obj.getString(FN_TAG);
            MN = obj.getString(MN_TAG);
            LN = obj.getString(LN_TAG);
            ST = obj.getString(ST_TAG);
            SD = obj.getString(SD_TAG);
            LCN = obj.getString(LCN_TAG); //Objects in JSON String are stored here as individual strings.

            Account.setAccount(strID);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showSettingsDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Enable NFC")
                .setMessage(msg)
                .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Open settings
                        startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                        dialog.dismiss();
                    }
                });


        sAD = builder.show();
    }

    private void showCameraDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("ID Picture")
                .setMessage("Please take an ID photo. (Make it a good one - you can't take it again!")
                .setPositiveButton("OPEN CAMERA", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //Open camera application
                        dispatchTakePictureIntent();
                        dialog.dismiss();
                    }
                })
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if(keyCode == KeyEvent.KEYCODE_BACK) {
                            Toast.makeText(getApplicationContext(), "You must take a picture in " +
                                    "order to use this application.", Toast.LENGTH_SHORT).show();
                        }
                        return false;
                    }
                });

        cAD = builder.show();
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Exit")
                .setMessage("In order to use this app, permissions will need to be granted. " +
                        "Click exit to return to the login screen.")
                .setPositiveButton("EXIT", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //Return to the login screen
                        finish();
                        dialog.dismiss();
                    }
                });

        eAD = builder.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cAD != null)
            cAD.dismiss();

        if(sAD != null)
            sAD.dismiss();

        if(eAD != null)
            eAD.dismiss();

        //Prevents "leaked window" error messages.
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Account.setAccount(strID);

        //NFC_Check("The application will not function if NFC is not activated. Please visit Settings to enable NFC.");

        Log.i(TAG, "onPostResume Called!!");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Account.setAccount("0000000");
        Log.i(TAG, "onStop Called!!");
    }

    private boolean picCheck() {
        /*///////////////////////////PERMISSION REQUEST///////////////////////////// - does not work when in onCreate for whatever reason - return to if you can.
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) { //If permission has not been granted, then...
            Log.i(TAG, "Requesting WES permission...");
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WES_PERMISSION); //...request the permission from user.
        }*/

        String loadPath = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/UniCard/UniCardID.png").toString();
        Bitmap bmp = BitmapFactory.decodeFile(loadPath);

        if (bmp == null) {
            Log.i(TAG, "False returned");
            return false;
        }
        else {
            Log.i(TAG, "True returned");
            return true;
        }
    }

    private void NFC_Check(String msg) {
        if(!NfcAdapter.getDefaultAdapter(this).isEnabled())
            showSettingsDialog(msg);
    }

    ////////////////////////////Barcode Stuff///////////////////////////////////
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    Bitmap encodeAsBitmap(String contentsToEncode, BarcodeFormat format, int img_width, int img_height) throws WriterException {
        if (contentsToEncode == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = null;
        String encoding = guessAppropriateEncoding(contentsToEncode);
        if (encoding != null) {
            hints = new EnumMap</*EncodeHintType, Object*/>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result;
        try {
            result = writer.encode(contentsToEncode, format, img_width, img_height, hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            iae.printStackTrace();
            return null;
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private static String guessAppropriateEncoding(CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }
    ////////////////////////////////////////////////////////////////////////////
}

