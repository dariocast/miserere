package it.dariocast.miserere.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import it.dariocast.miserere.BuildConfig;
import it.dariocast.miserere.R;
import it.dariocast.miserere.classi.Confraternita;
import it.dariocast.miserere.classi.Constants;
import it.dariocast.miserere.services.LocationService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LocationActivity extends AppCompatActivity implements OnClickListener {

    public boolean mTracking = false;
    Button startButton;
    Button stopButton;
    TextView txtStatus;
    Spinner spinnerConfraternite;
    List<Confraternita> confraternite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        try {
            confraternite = new ConfraterniteTask().execute().get();
            spinnerConfraternite = findViewById(R.id.confrat_spinner);
            ArrayAdapter spinnerArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, confraternite);
            spinnerConfraternite.setAdapter(spinnerArrayAdapter);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        startButton = findViewById(R.id.btn_start);
        stopButton = findViewById(R.id.btn_stop);
        txtStatus = findViewById(R.id.txt_status);
        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        toggleButtons();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                startLocationButtonClick();
                break;
            case R.id.btn_stop:
                stopLocationButtonClicked();
                break;

            default:
                break;
        }
    }

    public void stopLocationButtonClicked() {
        mTracking = false;
        toggleButtons();
        Intent stopIntent = new Intent(LocationActivity.this, LocationService.class);
        stopIntent.setAction(Constants.ACTION.STOPTRACKING_ACTION);
        startService(stopIntent);
    }

    public void startLocationButtonClick() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        AlertDialog.Builder passcodeDialog = new AlertDialog.Builder(LocationActivity.this);
                    LayoutInflater inflater = LayoutInflater.from(LocationActivity.this);
                    View view = inflater.inflate(R.layout.dialog_passcode, null,false);

                        passcodeDialog.setTitle("Passcode necessaria.");
                        passcodeDialog.setCancelable(true);
                        passcodeDialog.setView(view);
                        final EditText pin = (EditText) view.findViewById(R.id.passcode);
                        pin.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                        passcodeDialog.setPositiveButton("Trasmetti", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (allowTransmission(pin.getText().toString())) {
                                dialog.dismiss();
                                mTracking = true;
                                toggleButtons();
                                Confraternita selected = (Confraternita) spinnerConfraternite.getSelectedItem();
                                Intent startIntent = new Intent(LocationActivity.this, LocationService.class);
                                startIntent.setAction(Constants.ACTION.STARTTRACKING_ACTION);
                                startIntent.putExtra("id", selected.id);
                                startService(startIntent);
                                Toast.makeText(LocationActivity.this, "Inizio a trasmettere", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(LocationActivity.this, "Passcode Errato!", Toast.LENGTH_LONG).show();
                                pin.findFocus();
                            }
                        }
                    });
                        passcodeDialog.show();
                }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            openSettings();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction( Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void toggleButtons() {
        startButton.setEnabled(!mTracking);
        stopButton.setEnabled(mTracking);
        txtStatus.setText( (mTracking) ? "In trasmissione" : "Pronto a trasmettere" );
        txtStatus.setTextColor((mTracking) ? Color.RED : Color.GREEN);
    }

    private boolean allowTransmission(String passcode) {
        Confraternita selected = (Confraternita) spinnerConfraternite.getSelectedItem();
        if(passcode.equals(selected.passcode)) {
            return true;
        } else {
            return false;
        }
    }

}

class ConfraterniteTask extends AsyncTask<Void,Void, List<Confraternita>> {

    private static final String TAG = "ConfraternitaByIdTask";
    private static final String coordEndpoint = "https://dariocast.altervista.org/miserere/api/confraternita.php";
    private OkHttpClient client;
    private Request request;

    ConfraterniteTask() {
        client = new OkHttpClient();
        request = new Request.Builder()
                .url(coordEndpoint)
                .build();
    }

    @Override
    protected List<Confraternita> doInBackground(Void... voids) {
        List<Confraternita> confraternite = new ArrayList<>();
        try (Response response = client.newCall(request).execute()) {
            Log.d(TAG,"Ottengo response");
            JSONArray confraterniteArray = new JSONArray(response.body().string());
            if (confraterniteArray.length()>0) {
                for (int i = 0; i < confraterniteArray.length(); i++) {
                    JSONObject confraternita = confraterniteArray.getJSONObject(i);
                    int id = confraternita.getInt("id");
                    String nome = confraternita.getString("nome");
                    String colore = confraternita.getString("colore");
                    String passcode = confraternita.getString("passcode");

                    confraternite.add(new Confraternita(id,nome,colore,passcode));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG,"Confraternite trovata: "+ confraternite.toString());

        return confraternite;
    }
}