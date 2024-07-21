package com.example.examen2grupo4;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.Manifest;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.media.MediaRecorder;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.examen2grupo4.Models.Contacto;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private final String TAG = "Error";

    private MediaRecorder recorder = null;
    private MediaPlayer player = null;
    private String fileName = null;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    boolean estaGrabando = false;

    Button detenerAudio;
    Button capturarAudio;
    Button reproducirAudio;
    Button salvar;
    Button contactos;
    EditText nombre;
    EditText telefono;
    EditText latitud;
    EditText longitud;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        nombre = findViewById(R.id.nombre);
        telefono = findViewById(R.id.telefono);
        latitud = findViewById(R.id.latitud);
        longitud = findViewById(R.id.longitud);

        capturarAudio = findViewById(R.id.bt_capturar_audio);
        reproducirAudio = findViewById(R.id.bt_reproducir_audio);
        salvar = findViewById(R.id.bt_salvar);
        contactos = findViewById(R.id.bt_contactos);
        detenerAudio = findViewById(R.id.bt_detener_audio);

        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/grabacion.3gp";

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        capturarAudio.setOnClickListener(v -> startRecording());
        detenerAudio.setOnClickListener(v -> stopRecording());
        reproducirAudio.setOnClickListener(v -> startPlaying());
        contactos.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ContactosActivity.class)));
        salvar.setOnClickListener(v -> guardarContacto());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!permissionToRecordAccepted) finish();
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                //Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestLocationPermission();
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                double lat = location.getLatitude();
                                double lon = location.getLongitude();
                                latitud.setText(String.valueOf(lat));
                                longitud.setText(String.valueOf(lon));
                            } else {
                                Toast.makeText(MainActivity.this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void guardarContacto() {
        String url = "http://34.125.8.146/postContactos.php";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("nombre", nombre.getText().toString());
            jsonBody.put("telefono", telefono.getText().toString());
            jsonBody.put("latitud", latitud.getText().toString());
            jsonBody.put("longitud", longitud.getText().toString());
            jsonBody.put("audio", encodeAudioFileToBase64());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    Log.d("Response", response.toString());
                    startActivity(new Intent(MainActivity.this, ContactosActivity.class));
                },
                error -> Log.e("Error", error.toString())) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    private String encodeAudioFileToBase64() {
        File audioFile = new File(fileName);
        byte[] audioBytes = new byte[(int) audioFile.length()];
        try (InputStream is = new FileInputStream(audioFile)) {
            int bytesRead = is.read(audioBytes);
            if (bytesRead != audioFile.length()) {
                throw new IOException("Error al leer el archivo.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        String encodedString = Base64.encodeToString(audioBytes, Base64.DEFAULT);
        Log.d("Base64Audio", encodedString);
        return encodedString;
    }

    private void limpiarCampos() {
        nombre.setText("");
        telefono.setText("");
        latitud.setText("");
        longitud.setText("");
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        detenerAudio.setVisibility(View.VISIBLE);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.i(TAG, "startRecording: " + e.toString());
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;

        detenerAudio.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.i(TAG, "startPlaying: " + e.toString());
        }
    }
}
