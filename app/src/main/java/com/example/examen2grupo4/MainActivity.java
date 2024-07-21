package com.example.examen2grupo4;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.Manifest;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.examen2grupo4.Models.Contacto;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    //Concerniente a permisos de microfono
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    //Impresion en consola al fallar
    private final String TAG = "Error";

    //Archivo e instancia para grabar
    private MediaRecorder recorder = null;
    private MediaPlayer player = null;
    private String fileName = null;

    //booleano para definir y cambiar texto de: Capturar audio. -> Detener grabacion
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        nombre = (EditText) findViewById(R.id.nombre);
        telefono = (EditText) findViewById(R.id.telefono);
        latitud = (EditText) findViewById(R.id.latitud);
        longitud = (EditText) findViewById(R.id.longitud);

        capturarAudio = (Button) findViewById(R.id.bt_capturar_audio);
        reproducirAudio = findViewById(R.id.bt_reproducir_audio);
        salvar = (Button) findViewById(R.id.bt_salvar);
        contactos = (Button) findViewById(R.id.bt_contactos);
        detenerAudio = (Button) findViewById(R.id.bt_detener_audio);

        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/grabacion.3gp";


        capturarAudio.setOnClickListener(v -> startRecording());
        detenerAudio.setOnClickListener(v -> stopRecording());
        reproducirAudio.setOnClickListener(v -> startPlaying());
        contactos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ContactosActivity.class));
            }
        });
        salvar.setOnClickListener(v -> guardarContacto());
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
                    limpiarCampos();
                },
                error -> {
                    Log.e("Error", error.toString());
                }) {
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
        Log.d("Base64Audio", encodedString); // Imprimir en Logcat
        return encodedString;
    }

    private void limpiarCampos() {
        nombre.setText("");
        telefono.setText("");
        latitud.setText("");
        longitud.setText("");
    }

    //funcion para comenzar grabacion
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

    //funcion para detener grabacion
    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;

        detenerAudio.setVisibility(View.INVISIBLE);
    }

    //Funcion al salir de app detenga grabacion
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();
    }


}