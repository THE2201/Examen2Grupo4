package com.example.examen2grupo4;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ActualizarContacto extends AppCompatActivity {

    private EditText nombreEditText, telefonoEditText, latitudEditText, longitudEditText;
    private Button reproducirAudioButton, capturarAudioButton, detenerAudioButton, salvarButton, contactosButton;
    private MediaPlayer mediaPlayer;
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private RequestQueue requestQueue;
    private static final String TAG = "ActualizarContacto";
    private int id; // ID del contacto que se está actualizando

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_actualizar_contacto);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        nombreEditText = findViewById(R.id.nombre);
        telefonoEditText = findViewById(R.id.telefono);
        latitudEditText = findViewById(R.id.latitud);
        longitudEditText = findViewById(R.id.longitud);
        reproducirAudioButton = findViewById(R.id.bt_reproducir_audio);
        capturarAudioButton = findViewById(R.id.bt_capturar_audio);
        detenerAudioButton = findViewById(R.id.bt_detener_audio);
        salvarButton = findViewById(R.id.bt_salvar);
        contactosButton = findViewById(R.id.bt_contactos);

        requestQueue = Volley.newRequestQueue(this);

        id = getIntent().getIntExtra("contacto_id", -1);
        if (id != -1) {
            cargarContacto(id);
        }

        reproducirAudioButton.setOnClickListener(v -> {
            if (audioFilePath != null) {
                startPlaying(audioFilePath);
            } else {
                Toast.makeText(ActualizarContacto.this, "No hay audio para reproducir", Toast.LENGTH_SHORT).show();
            }
        });

        capturarAudioButton.setOnClickListener(v -> startRecording());

        detenerAudioButton.setOnClickListener(v -> stopRecording());

        salvarButton.setOnClickListener(v -> actualizarContacto());

        contactosButton.setOnClickListener(v -> finish());
    }

    private void cargarContacto(int id) {
        String url = "http://34.125.8.146/getContactos.php?id=" + id;

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            if (response.length() > 0) {
                                // Asume que solo hay un objeto en el array
                                JSONObject contacto = response.getJSONObject(0);
                                String nombre = contacto.getString("nombre");
                                String telefono = contacto.getString("telefono");
                                double latitud = contacto.getDouble("latitud");
                                double longitud = contacto.getDouble("longitud");
                                String audioBase64 = contacto.getString("audio");

                                nombreEditText.setText(nombre);
                                telefonoEditText.setText(telefono);
                                latitudEditText.setText(String.valueOf(latitud));
                                longitudEditText.setText(String.valueOf(longitud));

                                // Decodifica el audio y guarda en el archivo
                                if (audioBase64 != null && !audioBase64.isEmpty()) {
                                    audioFilePath = getExternalCacheDir().getAbsolutePath() + "/audio.3gp";
                                    decodeAudioFileFromBase64(audioBase64, audioFilePath);
                                }
                            } else {
                                Toast.makeText(ActualizarContacto.this, "No se encontraron datos", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(ActualizarContacto.this, "Error al analizar datos", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error al cargar datos: " + error.toString());
                        Toast.makeText(ActualizarContacto.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(jsonArrayRequest);
    }



    private void decodeAudioFileFromBase64(String base64Audio, String filePath) {
        byte[] decodedBytes = Base64.decode(base64Audio, Base64.DEFAULT);
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(decodedBytes);
            Log.d(TAG, "Audio guardado en: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startRecording() {
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/audio_record.3gp";

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(audioFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            detenerAudioButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Grabando audio...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al iniciar grabación", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            detenerAudioButton.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "Grabación detenida", Toast.LENGTH_SHORT).show();
        }
    }

    private void startPlaying(String filePath) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "startPlaying: " + e.toString());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void actualizarContacto() {
        String nombre = nombreEditText.getText().toString();
        String telefono = telefonoEditText.getText().toString();
        double latitud = Double.parseDouble(latitudEditText.getText().toString());
        double longitud = Double.parseDouble(longitudEditText.getText().toString());

        JSONObject postData = new JSONObject();
        try {
            postData.put("id", id);
            postData.put("nombre", nombre);
            postData.put("telefono", telefono);
            postData.put("latitud", latitud);
            postData.put("longitud", longitud);
            // Codifica el archivo de audio a base64
            if (audioFilePath != null) {
                postData.put("audio", encodeAudioFileToBase64());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "http://34.125.8.146/updateContactos.php";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.PUT,  // Cambiado a PUT para actualizar los datos
                url,
                postData,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(ActualizarContacto.this, "Contacto actualizado", Toast.LENGTH_SHORT).show();
                        finish(); // Regresa a la actividad anterior
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ActualizarContacto.this, "Error al actualizar el contacto", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(jsonObjectRequest);
    }

    private String encodeAudioFileToBase64() {
        File audioFile = new File(audioFilePath);
        byte[] audioBytes = new byte[(int) audioFile.length()];
        try (FileInputStream fis = new FileInputStream(audioFile)) {
            int bytesRead = fis.read(audioBytes);
            if (bytesRead != audioFile.length()) {
                throw new IOException("Error al leer el archivo.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return Base64.encodeToString(audioBytes, Base64.DEFAULT);
    }
}