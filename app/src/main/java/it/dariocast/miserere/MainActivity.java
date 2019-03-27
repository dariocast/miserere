package it.dariocast.miserere;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.R.drawable.*;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.ButtCap;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import it.dariocast.miserere.classi.Coordinate;
import it.dariocast.miserere.classi.CoppiaCoordinate;
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
            return true;
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
                LatLng coordTesta = new LatLng(coordinate.lat, coordinate.lon);
                mMap.addMarker(new MarkerOptions()
                        .position(coordTesta)
                        .title(coordinate.confraternita)
                        .icon(getMarkerIcon(coordinate.colore))
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

class CoordGetterTask extends AsyncTask<Void, Void, List<CoppiaCoordinate>> {
    private static final String TAG = "CoordGetterTask";
    private static final String coordEndpoint = "https://dariocast.altervista.org/miserere/api/coordinate.php";
    private OkHttpClient client;
    private Request request;

    CoordGetterTask() {
        client = new OkHttpClient();
        request = new Request.Builder()
                .url(coordEndpoint)
                .build();
    }

    @Override
    protected List<CoppiaCoordinate> doInBackground(Void... voids) {
        JSONArray coordinate = null;
        List<Coordinate> coordTrovate = new ArrayList<>();
        try (Response response = client.newCall(request).execute()) {
            Log.d(TAG,"Ottengo response");
            coordinate = new JSONArray(response.body().string());

            if (coordinate.length()>0) {
                for (int i = 0; i < coordinate.length(); i++) {
                    JSONObject coordinata = coordinate.getJSONObject(i);
                    String confraternita = coordinata.getString("confraternita");
                    String colore = coordinata.getString("colore");
                    double lat = Double.parseDouble(coordinata.getJSONObject("coordinate").getString("lat"));
                    double lon = Double.parseDouble(coordinata.getJSONObject("coordinate").getString("lon"));
                    String estremo = coordinata.getJSONObject("coordinate").getString("estremo");

                    coordTrovate.add(new Coordinate(confraternita, colore, lat,lon,estremo));
                }
            }
        } catch (IOException e) {
            Log.e(TAG,e.getMessage());
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e(TAG,e.getMessage());
            e.printStackTrace();
        }

        Log.d(TAG,"Coordinate trovate: "+coordTrovate.toString());
        List<CoppiaCoordinate> coppie = new ArrayList<>();
        Map<String,Coordinate> teste = new HashMap<>();
        Map<String,Coordinate> code = new HashMap<>();
        for (Coordinate coordTrovata: coordTrovate) {
            if(coordTrovata.estremo.equals("testa")) {
                teste.put(coordTrovata.confraternita, coordTrovata);
            } else {
               code.put(coordTrovata.confraternita, coordTrovata);
            }
        }
        for (Map.Entry<String,Coordinate> testa: teste.entrySet()) {
            coppie.add(new CoppiaCoordinate(testa.getValue(),code.get(testa.getKey())));
        }
        return coppie;
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
                    String confraternita = coordinata.getString("confraternita");
                    String colore = coordinata.getString("colore");
                    double lat = Double.parseDouble(coordinata.getJSONObject("coordinate").getString("lat"));
                    double lon = Double.parseDouble(coordinata.getJSONObject("coordinate").getString("lon"));
                    String estremo = coordinata.getJSONObject("coordinate").getString("estremo");

                    coordTeste.add(new Coordinate(confraternita, colore,lat,lon,estremo));
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