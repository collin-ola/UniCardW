package com.appsbycollin.unicardw;

import android.content.DialogInterface;
import android.nfc.cardemulation.HostApduService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import java.sql.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class UniCardWService extends HostApduService {
    private static final String TAG = "AppLog"; //Tag for logging
    private static final String AID = "F7285E7F77070394ABCD";

    // ISO-DEP (Data Exchange Protocol) Header for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    private static final String RETURN_DATA_LENGTH = "08";

    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = Utils.HexStringToByteArray("9000");

    // "UNKNOWN" status word sent in response to invalid APDU command (0x0000)
    private static final byte[] UNKNOWN_CMD_SW = Utils.HexStringToByteArray("FFFF");

    private static final byte[] SELECT_APDU = BuildSelectApdu(AID);

    private static int token = 0;
    private static String acc = null;

    private AlertDialog rAD;

    private static final int IP_SEL = 0; //0 = Home, 1 = Campus

    private static String URL_SEARCH = null;


    private void IP_SELECT() {
        switch (IP_SEL) {
            case 0: {
                Log.i(TAG, "At Home");
                URL_SEARCH = "http://192.168.0.18:8080/phpandroid/search.php";
            } break;
            case 1: {
                Log.i(TAG, "On Campus");
                URL_SEARCH = "http://10.154.14.111:8080/phpandroid/search.php";
            } break;
        }
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        IP_SELECT();

        byte[] returnData;

        Log.i(TAG, "Received APDU: " + Utils.ByteArrayToHexString(commandApdu));
        //If the APDU received matches the SELECT AID command, send data.
        if(Arrays.equals(SELECT_APDU, commandApdu)) {
            //Do data stuff here, then return it
            token = new Random().nextInt(0xFF); //Random int between 0 and 255

            acc = Account.getAccount();
            byte[] accBytes = acc.getBytes();
            byte[] tokBytes = new byte[1];
            tokBytes[0] = (byte)token;

            Log.i(TAG, "Sending student account number and token...");
            Log.i(TAG, "Token value is (int|hex) " + token + "|" + Integer.toHexString(token));

//            Toast.makeText(getApplicationContext(), "Student account number " +
//                    acc + " has been sent.", Toast.LENGTH_LONG).show();

            returnData = Utils.ConcatArrays(accBytes, tokBytes, SELECT_OK_SW);


            //Eventually, I may do more than just send the account number - depends on what I want
            //to implement.
            //^Sending a randomly generated token too.
        } else {
            returnData = UNKNOWN_CMD_SW;
        }
        return returnData;
    }

    @Override
    public void onDeactivated(int reason) {
        Log.i(TAG, "onDeactivated called. Reason code: " + reason); //called when new (different) AID is received or link is broken.

        if(!acc.contains("0000000")) {
            Log.i(TAG, "Verifying user has been registered in db...");

            Date today = new Date(System.currentTimeMillis()); //Milliseconds since epoch (Jan 1 1970).
            Log.i(TAG, "Date is " +  today.toString());

            checkForRecord cFR = new checkForRecord();
            cFR.execute(acc, Integer.toString(token), today.toString());
        }
    }

    public static byte[] BuildSelectApdu(String AID) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | DATA | RETURN DATA LENGTH]
        return Utils.HexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X",
                AID.length() / 2) + AID + RETURN_DATA_LENGTH);
    }

    private class checkForRecord extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {

            HashMap<String, String> searchData  = new HashMap<>();
            searchData.put("studentid", params[0]);
            searchData.put("token", params[1]);
            searchData.put("date", params[2]);

            Log.i(TAG, "Creating connection with ID " + params[0] + ", token " + params[1] + " and date "  + params[2]);

            createConn cC = new createConn();
            return cC.sendPostRequest(URL_SEARCH, searchData);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            String message;

            Log.i(TAG, "S Contains " + s);

            if(s.contains("TRUE") && s.contains("On Time"))
                message = "Your attendance to lecture " + s.substring(5, 11) + " has been recorded. You're on time, keep it up.";
            else if(s.contains("TRUE") && s.contains("Late"))
                message = "Your attendance to lecture " + s.substring(5, 11) + " has been recorded. You're late!";
            else if (s.contains("FALSE"))
                message = "Your attendance to the lecture has NOT been recorded - Please try again.";
            else
                message = "Something has gone wrong with registration. Please try again.";

            //showRegDialog(message);
            //Won't work due to the fact that I am trying to open a dialog
            //from a service! I am in an "illegal state".
            //java.lang.IllegalStateException: You need to use a Theme.AppCompat
            //(or descendant) with this activity.

            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            //A toast will do for now!
        }
    }

    private void showRegDialog(String message) { //This won't work here because this class is a service, and doesn't have a UI.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Lecture Registration")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        rAD = builder.show();
    }
}
