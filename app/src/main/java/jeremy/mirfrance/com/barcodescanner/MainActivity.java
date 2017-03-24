package jeremy.mirfrance.com.barcodescanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private Button btnScan = null;
    private Button btnStock = null;
    private Button btnExp = null;
    private Button btnDel = null;

    private EditText serialText = null;
    private EditText statusText = null;
    static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    private String serial_num = null;
    private String status = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnScan = (Button) findViewById(R.id.btnScan);
        btnStock = (Button) findViewById(R.id.btnStock);
        btnExp = (Button) findViewById(R.id.btnExp);
        btnDel = (Button) findViewById(R.id.btnDel);

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBar();
            }
        });
        btnStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        btnExp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        btnDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }


    private void scanBar() {
        try {
            //start the scanning activity from the com.google.zxing.client.android.SCAN intent
            Intent intent = new Intent(ACTION_SCAN);
            intent.putExtra("SCAN_MODE", "COD_128");
            startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException anfe) {

            Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException exDlFailed) {
                Log.e("Play store", "Can't open the PlayStore");
            }
        }
    }


    //on ActivityResult method
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                //get the extras that are returned from the intent
                String contents = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
                Toast toast = Toast.makeText(this, "Content:" + contents + " Format:" + format, Toast.LENGTH_LONG);
                toast.show();

                serial_num = contents;

                getInfoFromServer(serial_num);
            } else {
                System.out.print("ResultCode : " + resultCode);
            }
        } else {
            System.out.print("RequestCode : " + requestCode);
        }
    }

    //
    public void getInfoFromServer(String serial_num) {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String response = "";
        try {
            URL url = new URL("http://192.168.1.18/app");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            JSONObject root = new JSONObject();
            root.put("serial_number", serial_num);
            String str = root.toString();

            byte[] outputBytes = str.getBytes("UTF-8");
            OutputStream os = conn.getOutputStream();
            os.write(outputBytes);

            int responseCode = conn.getResponseCode();
            // Réponse d'un serveur HTTP :
            // 404 : Not found
            // 403 : Moved
            // 500 : Internal Server Error
            // 200 = HTTP OK

            if (responseCode == HttpsURLConnection.HTTP_OK) {

                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }

                try {
                    JSONObject json = new JSONObject(response);

                    if (json.has("status") && json.optInt("status") != 0) {
                        int status = json.getInt("status");
                        switch (status) {
                            case 1:
                                Log.e("Status", "1");
                                break;

                            case 2:
                                Log.e("Status", "2");
                                break;
                            default:
                                //
                                break;
                        }
                    } else {
                        if (json.has("error")) {
                            Log.e("Erreur", json.getString("error"));
                        } else {
                            // Erreur générique
                        }
                    }
                }
                catch(Exception jsonExecp) {
                    // Error in reading the JSON string
                }

            } else {
                // Network problem (server returns an error 500.. For example the PHP code is corrupted)
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
