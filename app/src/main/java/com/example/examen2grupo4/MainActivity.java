package com.example.examen2grupo4;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.examen2grupo4.Models.Contacto;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    //Concerniente a permisos de microfono
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};


    //Impresion en consola al fallar
    private final String TAG = "Error";

    //Archivo e instancia para grabar
    private MediaRecorder recorder = null;
    private String fileName = null;

    //booleano para definir y cambiar texto de: Capturar audio. -> Detener grabacion
    boolean estaGrabando = false;

    Button detenerAudio;
    Button capturarAudio;
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
        salvar = (Button) findViewById(R.id.bt_salvar);
        contactos = (Button) findViewById(R.id.bt_contactos);
        detenerAudio = (Button) findViewById(R.id.bt_detener_audio);

        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/grabacion.3gp";


        capturarAudio.setOnClickListener(v -> startRecording());
        detenerAudio.setOnClickListener(v -> stopRecording());
        contactos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ContactosActivity.class));
            }
        });

        salvar.setOnClickListener(v -> guardarContacto());


    }

    private void guardarContacto() {
        Contacto contact;
        //Insertar codigo para enviar
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
            Log.i(TAG, "startRecording: "+e.toString());
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