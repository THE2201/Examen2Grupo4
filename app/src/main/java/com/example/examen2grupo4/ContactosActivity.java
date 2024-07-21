package com.example.examen2grupo4;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ContactosActivity extends AppCompatActivity {

    private ListView listViewContactos;
    private Button botonAtras, eliminarButton, actualizarButton;
    private SearchView searchView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> contactos;
    private ArrayList<String> contactosOriginales;
    private String contactoSeleccionado;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contactos);

        listViewContactos = findViewById(R.id.listadecontactos);
        botonAtras = findViewById(R.id.botonatras);
        eliminarButton = findViewById(R.id.eliminarc);
        actualizarButton = findViewById(R.id.actualizarc);
        searchView = findViewById(R.id.searchView);

        contactos = new ArrayList<>();
        contactosOriginales = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, contactos);
        listViewContactos.setAdapter(adapter);

        requestQueue = Volley.newRequestQueue(this);

        fetchContactos();

        listViewContactos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                contactoSeleccionado = (String) parent.getItemAtPosition(position);
                showActionDialog();
            }
        });

        botonAtras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        eliminarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eliminarContacto();
            }
        });

        actualizarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actualizarContacto();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }

    private void showActionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Acción")
                .setMessage("¿Desea ir a la ubicación de "+contactoSeleccionado+"?")
                .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(ContactosActivity.this, "Ir a la ubicacion de " + contactoSeleccionado, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void eliminarContacto() {
        if (contactoSeleccionado != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirmar eliminación")
                    .setMessage("¿Está seguro de que desea eliminar este contacto?")
                    .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            contactos.remove(contactoSeleccionado);
                            adapter.notifyDataSetChanged();
                            contactoSeleccionado = null;
                            Toast.makeText(ContactosActivity.this, "Contacto eliminado", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else {
            Toast.makeText(this, "Seleccione un contacto para eliminar", Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarContacto() {
        if (contactoSeleccionado != null) {
            Toast.makeText(this, "Actualizar el contacto: " + contactoSeleccionado, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Seleccione un contacto para actualizar", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchContactos() {
        String url = "http://34.125.8.146/getContactos.php";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        contactos.clear();
                        contactosOriginales.clear();
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject contacto = response.getJSONObject(i);
                                String nombre = contacto.getString("nombre");
                                contactos.add(nombre);
                                contactosOriginales.add(nombre);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ContactosActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(jsonArrayRequest);
    }

    private void filter(String text) {
        List<String> filteredList = contactosOriginales.stream()
                .filter(item -> item.toLowerCase().contains(text.toLowerCase()))
                .collect(Collectors.toList());
        contactos.clear();
        contactos.addAll(filteredList);
        adapter.notifyDataSetChanged();
    }

}