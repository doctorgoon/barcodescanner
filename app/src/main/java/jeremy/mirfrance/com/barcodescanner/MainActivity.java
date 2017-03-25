package jeremy.mirfrance.com.barcodescanner;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private Button btnScan = null;
    private Button btnStock = null;
    private Button btnExp = null;
    private Button btnDel = null;

    private TextView textView = null;
    private TextView textView2 = null;
    private TextView textView3 = null;


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
        serialText = (EditText) findViewById(R.id.serialText);
        statusText = (EditText) findViewById(R.id.statusText);

        /*textView.setVisibility(View.GONE);
        textView2.setVisibility(View.GONE);
        textView3.setVisibility(View.GONE);

        serialText.setVisibility(View.GONE);
        statusText.setVisibility(View.GONE);*/

        btnStock.setVisibility(View.VISIBLE);
        btnExp.setVisibility(View.VISIBLE);
        btnDel.setVisibility(View.VISIBLE);

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBar();
            }
        });
        btnStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendInfoToServer(serial_num, 0);
            }
        });
        btnExp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendInfoToServer(serial_num, 1);

            }
        });
        btnDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendInfoToServer(serial_num, 2);
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
            URL url = new URL("http://192.168.1.97/app/get");

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

            if (responseCode == HttpsURLConnection.HTTP_OK) {

                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }

                try {
                    JSONObject json = new JSONObject(response);

                    if (json.has("status") && json.optInt("status") >= 0) {
                        int status = json.getInt("status");
                        switch (status) {
                            case 0:
                                Log.e("Status", "0");
                                serialText.setText(serial_num);
                                statusText.setText("Inconnu");
                                status = 0;
                                btnStock.setVisibility(View.VISIBLE);
                                btnExp.setVisibility(View.VISIBLE);
                                btnDel.setVisibility(View.GONE);
                                break;
                            case 1:
                                Log.e("Status", "1");
                                serialText.setText(serial_num);
                                statusText.setText("En stock");
                                btnStock.setVisibility(View.GONE);
                                btnExp.setVisibility(View.VISIBLE);
                                btnDel.setVisibility(View.VISIBLE);
                                status = 1;
                                break;
                            case 2:
                                Log.e("Status", "2");
                                serialText.setText(serial_num);
                                statusText.setText("Expédié");
                                btnStock.setVisibility(View.VISIBLE);
                                btnExp.setVisibility(View.GONE);
                                btnDel.setVisibility(View.VISIBLE);
                                status = 2;
                                break;
                            default:
                                serialText.setText(serial_num);
                                statusText.setText("Erreur");
                                break;
                        }
                    } else {
                        if (json.has("error")) {
                            Log.e("Erreur", json.getString("error"));
                            Toast toast5 = Toast.makeText(this, "Erreur lors du chargement des infos", Toast.LENGTH_LONG);
                            toast5.show();
                        } else {
                            Toast toast6 = Toast.makeText(this, "Erreur", Toast.LENGTH_LONG);
                            toast6.show();
                        }
                    }
                }
                catch(Exception jsonExecp) {
                    Toast toast7 = Toast.makeText(this, "Erreur lors de la lecture", Toast.LENGTH_LONG);
                    toast7.show();
                }

            } else {
                Toast toast8 = Toast.makeText(this, "Problèmes réseaux", Toast.LENGTH_LONG);
                toast8.show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void sendInfoToServer(String serial_num, int i) {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String response = "";
        try {
            URL url = new URL("http://192.168.1.97/app/set");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            JSONObject root = new JSONObject();
            root.put("serial_number", serial_num);
            root.put("action", i);
            String str = root.toString();

            byte[] outputBytes = str.getBytes("UTF-8");
            OutputStream os = conn.getOutputStream();
            os.write(outputBytes);

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {

                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }

                try {
                    JSONObject json = new JSONObject(response);

                    if (json.has("success") && json.optInt("success") > 0) {
                        int success = json.getInt("success");
                        switch (success) {
                            case 1:
                                Log.e("Success", "1");
                                Toast toast1 = Toast.makeText(this, "Article ajouté avec succès", Toast.LENGTH_LONG);
                                toast1.show();
                                break;
                            case 2:
                                Log.e("Success", "2");
                                Toast toast2 = Toast.makeText(this, "Article marqué en stock", Toast.LENGTH_LONG);
                                toast2.show();
                                break;
                            case 3:
                                Log.e("Success", "3");
                                Toast toast3 = Toast.makeText(this, "Article marqué expédié", Toast.LENGTH_LONG);
                                toast3.show();
                                break;
                            case 4:
                                Log.e("Success", "4");
                                Toast toast4 = Toast.makeText(this, "Article supprimé", Toast.LENGTH_LONG);
                                toast4.show();
                                break;
                            default:
                                Toast toast5 = Toast.makeText(this, "Echec", Toast.LENGTH_LONG);
                                toast5.show();
                                break;
                        }
                    } else {
                        if (json.has("error")) {
                            Log.e("Erreur", json.getString("error"));
                            Toast toast5 = Toast.makeText(this, "Erreur lors du chargement des infos", Toast.LENGTH_LONG);
                            toast5.show();
                        } else {
                            Toast toast6 = Toast.makeText(this, "Erreur", Toast.LENGTH_LONG);
                            toast6.show();
                        }
                    }
                }
                catch(Exception jsonExecp) {
                    Toast toast7 = Toast.makeText(this, "Erreur lors de la lecture", Toast.LENGTH_LONG);
                    toast7.show();
                }

            } else {
                Toast toast8 = Toast.makeText(this, "Problèmes réseaux", Toast.LENGTH_LONG);
                toast8.show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
