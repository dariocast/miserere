package it.dariocast.miserere.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import it.dariocast.miserere.R;
import it.dariocast.miserere.classi.Confraternita;
import it.dariocast.miserere.classi.Coordinate;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    public List<Coordinate> lista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (isGooglePlayServicesAvailable(MainActivity.this)) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }

        FloatingActionButton fab = findViewById(R.id.map_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshMap();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent();
            i.setClass(MainActivity.this,LocationActivity.class);
            startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        refreshMap();
    }

    private void refreshMap() {
        try {
            lista = new CoordTesteGetterTask().execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (lista.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.empty_list_title)
                    .setMessage(R.string.empty_list_msg)
                    .setIcon(android.R.drawable.stat_notify_error)
                    .setCancelable(false)
                    .setPositiveButton(R.string.refresh, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            refreshMap();
                        }
                    });
            builder.show();
        } else {
            LatLng nuovaPosizione = null;
            for (Coordinate coordinate: lista) {
                Confraternita confraternita = null;
                try {
                    confraternita = new ConfraternitaByIdTask().execute(coordinate.confraternitaId).get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LatLng coordTesta = new LatLng(coordinate.lat, coordinate.lon);
                mMap.addMarker(new MarkerOptions()
                        .position(coordTesta)
                        .title(confraternita!=null?confraternita.nome:"Confraternita con id "+coordinate.confraternitaId)
                        .icon(getMarkerIcon(confraternita!=null?confraternita.colore:"#000000"))
//                        .icon(BitmapDescriptorFactory.fromAsset(coordinate.colore.toLowerCase()+".bmp"))

                );
                if (nuovaPosizione==null) {
                    nuovaPosizione=coordTesta;
                }
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLng(nuovaPosizione));
        }
    }

    public boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if(status != ConnectionResult.SUCCESS) {
            if(googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 2404).show();
            }
            return false;
        }
        return true;
    }

    public BitmapDescriptor getMarkerIcon(String color) {
        float[] hsv = new float[3];
        Color.colorToHSV(Color.parseColor(color), hsv);
        return BitmapDescriptorFactory.defaultMarker(hsv[0]);
    }
}


class CoordTesteGetterTask extends AsyncTask<Void, Void, List<Coordinate>> {
    private static final String TAG = "CoordTesteGetterTask";
    private static final String coordEndpoint = "https://dariocast.altervista.org/miserere/api/coordinate.php";
    private OkHttpClient client;
    private Request request;

    CoordTesteGetterTask() {
        client = new OkHttpClient();
        request = new Request.Builder()
                .url(coordEndpoint)
                .build();
    }

    @Override
    protected List<Coordinate> doInBackground(Void... voids) {
        JSONArray coordinate = null;
        List<Coordinate> coordTeste = new ArrayList<>();
        try (Response response = client.newCall(request).execute()) {
            Log.d(TAG,"Ottengo response");
            coordinate = new JSONArray(response.body().string());

            if (coordinate.length()>0) {
                for (int i = 0; i < coordinate.length(); i++) {
                    JSONObject coordinata = coordinate.getJSONObject(i);
                    int confraternitaId = Integer.parseInt(coordinata.getString("confraternitaId"));
                    double lat = Double.parseDouble(coordinata.getJSONObject("coordinate").getString("lat"));
                    double lon = Double.parseDouble(coordinata.getJSONObject("coordinate").getString("lon"));
                    String estremo = coordinata.getJSONObject("coordinate").getString("estremo");

                    coordTeste.add(new Coordinate(confraternitaId,lat,lon,estremo));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"Coordinate trovate: "+coordTeste.toString());

        return coordTeste;
    }
}

class ConfraternitaByIdTask extends AsyncTask<Integer, Void, Confraternita> {
    private static final String TAG = "ConfraternitaByIdTask";
    private static final String coordEndpoint = "https://dariocast.altervista.org/miserere/api/confraternita.php";
    private OkHttpClient client;
    private Request request;

    ConfraternitaByIdTask() {
        client = new OkHttpClient();
    }

    @Override
    protected Confraternita doInBackground(Integer... ids) {
        Confraternita confraternita = null;
        String queryUrl = coordEndpoint + "?id="+ids[0];
        request = new Request.Builder()
                .url(queryUrl)
                .build();
        try (Response response = client.newCall(request).execute()) {
            Log.d(TAG,"Ottengo response");
            JSONObject confraternitaJson = new JSONObject(response.body().string());
            confraternita = new Confraternita(
                    confraternitaJson.getInt("id"),
                    confraternitaJson.getString("nome"),
                    confraternitaJson.getString("colore"),
                    confraternitaJson.getString("passcode")
            );
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG,"Confraternita trovata: "+confraternita.toString());

        return confraternita;
    }
}