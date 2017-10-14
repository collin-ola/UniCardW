package com.appsbycollin.unicardw;

import android.util.Log;

public class Account {

    private static final String TAG = "AppLog";
    private static String accountNumber = "0000000";


    public static String getAccount() {
        Log.i(TAG, "Account number getter called.");
        return accountNumber;
    }

    public static void setAccount(String accNum) {
        Log.i(TAG, "Account number setter called. ID is " + accNum  );
        accountNumber = accNum;
    }
}


