package com.example.proyectopresionarterial;

import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageHelper {
    // Clave pública de Unsplash (puedes obtener una gratuita en https://unsplash.com/developers)
    private static final String UNSPLASH_ACCESS_KEY = "uzemulN86NyvRvxMPiwNvyrDNf9nkDu7KK3quFpkijQ";
    private static final String UNSPLASH_URL = "https://api.unsplash.com/search/photos?page=1&per_page=1&query=%s&client_id=%s";

    public interface ImageCallback {
        void onImageUrl(String url);
        void onError();
    }

    public static void fetchImage(String keyword, ImageCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String urlStr = String.format(UNSPLASH_URL, keyword, UNSPLASH_ACCESS_KEY);
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();
                        JSONObject json = new JSONObject(sb.toString());
                        JSONArray results = json.getJSONArray("results");
                        if (results.length() > 0) {
                            JSONObject first = results.getJSONObject(0);
                            JSONObject urls = first.getJSONObject("urls");
                            return urls.getString("regular");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
            @Override
            protected void onPostExecute(String url) {
                if (url != null) callback.onImageUrl(url);
                else callback.onError();
            }
        }.execute();
    }
}
