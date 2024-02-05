package bd.stock.njoystick;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import bd.stock.njoystick.Models.Producto;
import bd.stock.njoystick.Models.Ventas;
import bd.stock.njoystick.databinding.ControlStockBinding;
import bd.stock.njoystick.databinding.VentaBinding;

public class ControlStock extends AppCompatActivity {
    private ArrayAdapter<String> listaProductosStock;
    private ArrayAdapter<Producto> autoCompleteAdapter;
    private String itemSeleccionado;
    private ArrayList<Producto> listaProductos = new ArrayList<>();
    private ArrayList<Producto> listaProductosFiltrados = new ArrayList<>();
    ControlStockBinding binding; // Utiliza la clase de binding correcta
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_stock);

        try {
            binding = ControlStockBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            // Inicializa el ArrayAdapter y asócialo con el ListView
            listaProductosStock = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            binding.ListaProductoStock.setAdapter(listaProductosStock);

        } catch (Exception e){
            Log.e("ControlStock", "Error en onCreate", e);
        }

        DatabaseReference productosRef = FirebaseDatabase.getInstance().getReference("productos");
        productosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                listaProductos.clear(); // Limpiamos la lista de productos existente
                listaProductosStock.clear(); // Limpiamos el ArrayAdapter

                for (DataSnapshot productoSnapshot : dataSnapshot.getChildren()) {
                    Producto producto = productoSnapshot.getValue(Producto.class);
                    listaProductos.add(producto);
                }

                // Llamamos al código para manejar la selección del Spinner cuando se cargan los productos por primera vez
                handleSpinnerSelection();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Log to check if onCancelled is being called and to show the error
                Toast.makeText(getApplicationContext(), "Error al leer datos de Firebase", Toast.LENGTH_SHORT).show();
            }
        });

        binding.SpinnerCategorias.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Esta función se ejecutará cada vez que se modifique la selección del Spinner
                handleSpinnerSelection();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Se llama cuando no hay ninguna selección
            }
        });

        // Configura el AutoCompleteTextView y el adaptador
        AutoCompleteTextView autoCompleteTextView = findViewById(R.id.autoCompleteTextView);
        autoCompleteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
        autoCompleteTextView.setAdapter(autoCompleteAdapter);

        // Agrega un TextChangedListener al AutoCompleteTextView para manejar las actualizaciones en tiempo real
        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filtrar los productos según el texto ingresado y la categoría seleccionada
                String searchText = s.toString();
                filterProducts(searchText);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

    }

    // Método para filtrar los productos según el texto de búsqueda y la categoría seleccionada
    private void filterProducts(String searchText) {
        String categoriaSeleccionada = binding.SpinnerCategorias.getSelectedItem().toString();

        // Limpiamos la lista de productos filtrados
        listaProductosFiltrados.clear();

        // Filtramos los productos según la categoría seleccionada y el texto de búsqueda
        for (Producto producto : listaProductos) {
            if (producto.getCategoria().equals(categoriaSeleccionada) &&
                    producto.getNombre().toLowerCase().contains(searchText.toLowerCase())) {
                listaProductosFiltrados.add(producto);
            }
        }

        // Llamamos al método para manejar la selección del Spinner y actualizar la lista debajo
        handleSpinnerSelectionBuscar();
    }


    private void handleSpinnerSelectionBuscar() {
        listaProductosStock.clear(); // Limpiamos el ArrayAdapter

        // Agregamos los productos filtrados a la listaProductosStock
        for (Producto producto : listaProductosFiltrados) {
            listaProductosStock.add(producto.getNombre() + " Stock: " + producto.getCantidad());
        }

        // Notificamos al ArrayAdapter sobre los cambios en los datos
        listaProductosStock.notifyDataSetChanged();
    }


    private void handleSpinnerSelection() {
        itemSeleccionado = binding.SpinnerCategorias.getSelectedItem().toString();
        listaProductosStock.clear(); // Limpiamos el ArrayAdapter
        // Obtenemos la categoría deseada (por ejemplo, "Electrónicos")

        // Creamos una lista temporal para almacenar los productos de la categoría deseada
        List<Producto> productosFiltrados = new ArrayList<>();

        for (Producto producto : listaProductos) {
            // Verificamos si el producto pertenece a la categoría deseada
            if (producto.getCategoria().equals(itemSeleccionado)) {
                productosFiltrados.add(producto);
            }
        }

        // Ordenamos la lista de productos filtrados por cantidad de stock de forma ascendente
        Collections.sort(productosFiltrados, new Comparator<Producto>() {
            @Override
            public int compare(Producto producto1, Producto producto2) {
                return Integer.compare(producto1.getCantidad(), producto2.getCantidad());
            }
        });

        // Agregamos los productos ordenados a la listaProductosStock
        for (Producto producto : productosFiltrados) {
            listaProductosStock.add(producto.getNombre() + " Stock: " + producto.getCantidad());
        }

        // Notificamos al ArrayAdapter sobre los cambios en los datos
        listaProductosStock.notifyDataSetChanged();
    }


}
