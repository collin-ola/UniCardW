package com.appsbycollin.unicardw;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivityLog";
    private EditText et_Username;
    private EditText et_Password;
    private CheckBox cb_RememberMe;

    private static final int IP_SEL = 0; //0 = Home, 1 = Campus

    private static String URL_PARSE = null;

    private void IP_SELECT() {
        switch (IP_SEL) {
            case 0: {
                Log.i(TAG, "At Home");
                URL_PARSE = "http://192.168.0.18:8080/phpandroid/parse.php";
            } break;
            case 1: {
                Log.i(TAG, "On Campus");
                URL_PARSE = "http://10.154.14.111:8080/phpandroid/parse.php";
            } break;
        }
    }

    private static final String FILENAME = "LoginData";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate Called");

        IP_SELECT();

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //forces activity to remain in portrait orientation.

        String[] loginData = readFromFile();
        et_Username = (EditText) findViewById(R.id.ID_eT_Username);
        et_Password = (EditText) findViewById(R.id.ID_et_Password);

        //checkForData();
        if (loginData[0] == null && loginData[1] == null) {
            Log.i(TAG, "No prior login data exists, do nothing.");
        } else {
            et_Username.setText(loginData[0]);
            et_Password.setText(loginData[1]);
            //cb_RememberMe.setChecked(true);
        }

        Button buttonLogin = (Button) findViewById(R.id.ID_buttonLogin);

        //onClick Listener for buttonOK
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i(TAG, "onButtonLogin_Click Called.");

                cb_RememberMe = (CheckBox) findViewById(R.id.id_cB_rememberMe);
                String username = et_Username.getText().toString();
                String password = et_Password.getText().toString();

                if(cb_RememberMe.isChecked())
                    writeToFile(new String[]{username, password});
                //else if(!cb_RememberMe.isChecked())
                   // writeToFile(new String[]{null, null});

                login(username, password);
            }
        });
    }

    private int writeToFile(String[] dataToWrite) {
        FileOutputStream fos = null; //should reaaaally use FileWriter, which is designed for text

        String username = dataToWrite[0];
        String password = dataToWrite[1];

        try {
            fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            Log.i(TAG, "Opening file for writing...");
        } catch (FileNotFoundException FNFE) {
            FNFE.printStackTrace();
        }

        try {
            /**IMPORTANT
             * The highest value you can typecast is 127 - after this, the MSB of a byte is set
             * to 1. This value is taken as negative, due to Java bytes/ints/longs being signed.
             * An int of 127 is still a byte of 127. An int of 128 is -128 in byte represenation.
             * 129 is -127, and so on.
             * **/
            Log.i(TAG, "Writing login data to file.");
            ByteBuffer bb = ByteBuffer.allocate(username.length() + password.length() + 2);

            bb.position(0);
            bb.put((byte)username.length());

            bb.position(1);
            bb.put((byte)password.length());

            bb.position(2);
            bb.put(username.getBytes());

            bb.position(username.length() + 2);
            bb.put(password.getBytes());

            byte[] array = bb.array();

            assert fos != null;
            fos.write(array);

            Log.i(TAG, "Closing file...");
            fos.close();

        } catch (IOException IOEx) {
            IOEx.printStackTrace();
        }
        return 0;
    }

    private String[] readFromFile() {
        FileInputStream fis; //should reaaaally use FileReader, which is designed for text
        String storedUN = null, storedPW = null;


        try {
            fis = openFileInput(FILENAME);
            Log.i(TAG, "Opening file for reading...");
        } catch (FileNotFoundException FNFE) {
            FNFE.printStackTrace();
            return new String[] {null, null};
        }

        try {
            Log.i(TAG, "Reading from file...");

            int unLength = fis.read();
            int pwLength = fis.read();

            byte[] unFromFile = new byte[unLength];
            byte[] pwFromFile = new byte[pwLength];

            //noinspection ResultOfMethodCallIgnored
            fis.read(unFromFile);
            //noinspection ResultOfMethodCallIgnored
            fis.read(pwFromFile);

            Log.i(TAG, "Closing file...");
            fis.close();

            storedUN = new String(unFromFile, "US-ASCII");
            storedPW = new String(pwFromFile, "UTF-8");
            /**
             * Basically, unicode == ASCII for first 128 characters, covering the entire
             * alphanumeric set in both lower and upper case, so they can be used
             * interchangeably.
             */

        } catch (IOException IOEx) {
            IOEx.printStackTrace();
        }

        return new String[]{storedUN, storedPW};
    }

    private void login(final String un, final String pw) {
        class LoginAsync extends AsyncTask<String, Void, String>{ //==getJSON

            private ProgressDialog loadingDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loadingDialog = ProgressDialog.show(MainActivity.this, "Logging In", "Please wait...", true, true);
            }

            @Override
            protected String doInBackground(String... params) {
                HashMap<String, String> data = new HashMap<>();
                data.put("username", params[0]);
                data.put("password", params[1]);

                createConn cC = new createConn();

                Log.i(TAG, "Returning result from " + params[0] + ", " + params[1]);
                return cC.sendPostRequest(URL_PARSE, data); //will I get data back from here?
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loadingDialog.dismiss();

                if(s.contains("TRUE")) { //If the JSON String contains "TRUE", good data has been returned.
                    Log.i(TAG, "Logged in!");
                    Toast.makeText(MainActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                    //Launch new activity
                    Intent i = new Intent(getApplicationContext(), UniCardActivity.class);
                    i.putExtra("userDetails", s);
                    startActivity(i);
                } else {
                    Toast.makeText(MainActivity.this, "Login Failed. Please ensure you have " +
                            "entered the correct credentials.", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "s Contains: " + s);
                }
            }
        }

        LoginAsync LA = new LoginAsync();
        LA.execute(un, pw);
    }

    @Override
    protected void onResume() {
        new Intent(this, MainActivity.class).
                addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        Log.i(TAG, "onResume Called");

        Account.setAccount("0000000"); //when a user presses "back" and is logged out, the
                                        //account number is reset to its default value.
        super.onResume();
    }


}
