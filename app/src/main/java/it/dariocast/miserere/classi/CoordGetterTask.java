package it.dariocast.miserere.classi;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CoordGetterTask extends AsyncTask<String, Void, List<Coordinate>> {
    static final String TAG = "CoordGetterTask";
    static final String coordEndpoint = "https://dariocast.altervista.org/miserere/api/coordinate.php";
    OkHttpClient client;
    Request request;

    public CoordGetterTask() {
        client = new OkHttpClient();
        request = new Request.Builder()
                .url(coordEndpoint)
                .build();
    }

    @Override
    protected List<Coordinate> doInBackground(String... strings) {
        JSONArray coordinate = null;
        List<Coordinate> coordTrovate = new ArrayList<>();
        try (Response response = client.newCall(request).execute()) {
            Log.d(TAG,"Ottengo response");
            coordinate = new JSONArray(response.body().string());

            if (coordinate.length()>0) {
                for (int i = 0; i < coordinate.length(); i++) {
                    JSONObject coordinata = coordinate.getJSONObject(i);
                    String confraternita = coordinata.getString("confraternita");
                    double lat = Double.parseDouble(coordinata.getJSONObject("coordinate").getString("lat"));
                    double lon = Double.parseDouble(coordinata.getJSONObject("coordinate").getString("lon"));
                    String estremo = coordinata.getJSONObject("coordinate").getString("estremo");

                    coordTrovate.add(new Coordinate(confraternita,lat,lon,estremo));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"Coordinate trovate: "+coordTrovate.toString());
        return coordTrovate;
    }
}
