package bd.stock.njoystick;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.reflect.TypeToken;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import bd.stock.njoystick.Models.ProductAdapter;
import bd.stock.njoystick.Models.Producto;
import bd.stock.njoystick.Models.ProductoVenta;
import bd.stock.njoystick.Models.Ventas;
import bd.stock.njoystick.Services.CaptureActivityPortrait;
import bd.stock.njoystick.Services.InputCodigoDialog;
import bd.stock.njoystick.databinding.VentaBinding; // Asegúrate de importar la clase correcta

public class Venta extends AppCompatActivity implements InputCodigoDialog.OnInputListener {
    private static final String PREFS_NAME = "VentaPrefs";
    private static final String PREF_VENTA_EN_CURSO = "venta_en_curso";
    VentaBinding binding; // Utiliza la clase de binding correcta
    private ArrayAdapter<String> listaProductosAdapter;
    private ArrayList<ProductoVenta> ControlStock = new ArrayList<>();
    private Ventas VentasReport;
    private Producto productoStored = null;
    private AlertDialog alertDialog;
    private EditText editTextBuscar;
    private RecyclerView recyclerView;
    private ProductAdapter adapterSearch;
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Toast.makeText(this, "CANCELADO", Toast.LENGTH_SHORT).show();
        } else {
            obtenerInformacionProducto(result.getContents());
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Utiliza la clase de binding correcta y asegúrate de inflarla antes de usarla
        binding = VentaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.imageViewProducto.setImageResource(R.drawable.placeholder_image);
        // Inicializa el ArrayAdapter y asócialo con el ListView
        listaProductosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        binding.listaProductos.setAdapter(listaProductosAdapter);
        adapterSearch =  new ProductAdapter(this, new ArrayList<>());
        VentasReport = new Ventas();
        binding.btnLeerCodigo.setOnClickListener(view -> escanear());
        binding.btnAddALista.setOnClickListener(view -> enviarALista());
        binding.btnRealizarVenta.setOnClickListener(view -> aceptarCompra());
        binding.btnCancelarVenta.setOnClickListener(view -> cancelarCompra());
        binding.btnIngresoManual.setOnClickListener(view -> mostrarDialogoBusqueda());
        if (ventaEnCurso()) {
            restaurarControlStock();
        }
    }

    private void abrirDialog() {
        InputCodigoDialog dialogFragment = new InputCodigoDialog();
        dialogFragment.show(getSupportFragmentManager(), "inputDialog");
    }
    @Override
    public void onInputReceived(String userInput) {
        obtenerInformacionProducto(userInput);
    }
    public void escanear() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        options.setPrompt("ESCANEAR CODIGO");
        options.setCameraId(0);
        options.setOrientationLocked(false);
        options.setBeepEnabled(false);
        options.setCaptureActivity(CaptureActivityPortrait.class);
        options.setBarcodeImageEnabled(false);

        barcodeLauncher.launch(options);
    }

    private void obtenerInformacionProducto(String codigoProducto) {
        toggleProgressBar(true);
        DatabaseReference productosRef = FirebaseDatabase.getInstance().getReference("productos").child(codigoProducto);
        productosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Producto producto = dataSnapshot.getValue(Producto.class);
                    if (producto != null) {
                        // Muestra la imagen del producto en el ImageView correspondiente
                        Picasso.get().load(producto.getUrlImagen()).into(binding.imageViewProducto);
                        binding.textViewNombreProducto.setText(producto.getNombre());
                        binding.editTextCantidad.setText("1");
                        binding.textViewPrecio.setText(getString(R.string.precio_producto, String.valueOf(producto.getPrecio())));
                        productoStored = producto;
                        toggleProgressBar(false);
                    }
                } else {
                    toggleProgressBar(false);
                    Toast.makeText(getApplicationContext(), "Producto no encontrado", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                toggleProgressBar(false);
                Toast.makeText(getApplicationContext(), "Error al leer datos de Firebase", Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void aceptarCompra() {
        toggleProgressBar(true);
        ListView listView = findViewById(R.id.listaProductos);
        ListAdapter adaptadorUnico = listView.getAdapter();

        if (adaptadorUnico != null) {
            int itemCount = adaptadorUnico.getCount();

            if (itemCount == 0) {
                if (productoStored != null) {
                    int cantidadVenta = Integer.parseInt(binding.editTextCantidad.getText().toString());

                    // Asegurar de que haya suficiente stock disponible
                    if (productoStored.getCantidad() >= cantidadVenta) {
                        // actualización bdd
                        DatabaseReference productoRef = FirebaseDatabase.getInstance().getReference("productos").child(productoStored.getCodigo());
                        int nuevaCantidad = productoStored.getCantidad() - cantidadVenta;
                        productoRef.child("cantidad").setValue(nuevaCantidad);
                        productoStored.setCantidad(nuevaCantidad);

                        // Limpiar campos y crear una nueva venta
                        limpiarCampos();
                        Ventas nuevaVenta = new Ventas();
                        ProductoVenta aux = new ProductoVenta();
                        aux.producto = productoStored;
                        aux.cantidad = cantidadVenta;
                        nuevaVenta.setListaProductosVenta(new ArrayList<>(Collections.singletonList(aux)));
                        nuevaVenta.setMontoTotal(cantidadVenta * productoStored.getPrecio());
                        nuevaVenta.setFecha(new Date());

                        // Actualizar la lista de ventas
                        DatabaseReference ventasRef = FirebaseDatabase.getInstance().getReference("ventas");
                        String nuevaVentaKey = ventasRef.push().getKey();
                        ventasRef.child(nuevaVentaKey).setValue(nuevaVenta);

                        if(nuevaCantidad==0){
                            // Inflar la vista del Toast personalizado
                            LayoutInflater inflater = getLayoutInflater();
                            View layout = inflater.inflate(R.layout.fondo_toast_alert, null);

// Configurar el texto del TextView en la vista inflada
                            TextView text = layout.findViewById(R.id.textViewToastMessage);
                            text.setText("HA QUEDADO SIN STOCK DE: " + productoStored.getNombre());

// Crear y mostrar el Toast personalizado
                            Toast toast = new Toast(getApplicationContext());
                            toast.setDuration(Toast.LENGTH_SHORT);
                            toast.setView(layout);
                            toast.show();
                        }
                        // Limpiar producto almacenado
                        productoStored = null;
                        Toast.makeText(getApplicationContext(), "Compra confirmada con éxito", Toast.LENGTH_SHORT).show();
                        VentasReport.setMontoTotal(0);
                        binding.totalVenta.setText("TOTAL VENTA: $");

                    } else {

                        Toast.makeText(getApplicationContext(), "Stock insuficiente", Toast.LENGTH_SHORT).show();
                        VentasReport.setMontoTotal(0);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Debe buscar un nuevo producto", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Si hay elementos en la lista, maneja la lógica aquí
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) binding.listaProductos.getAdapter();
                adapter.clear();
                adapter.notifyDataSetChanged();
                limpiarCampos();

                // Crear una nueva venta con la lista actual de productos vendidos
                Ventas nuevaVenta = new Ventas();
                nuevaVenta.setListaProductosVenta(ControlStock);
                nuevaVenta.setMontoTotal(calcularMontoTotal(ControlStock));
                nuevaVenta.setFecha(new Date());

                // Actualizar la lista de ventas
                DatabaseReference ventasRef = FirebaseDatabase.getInstance().getReference("ventas");
                String nuevaVentaKey = ventasRef.push().getKey();
                ventasRef.child(nuevaVentaKey).setValue(nuevaVenta);

                // Limpiar la lista de productos vendidos
                ControlStock.clear();
                guardarEstadoVentaEnCurso(false);

                Toast.makeText(getApplicationContext(), "Compra confirmada con éxito", Toast.LENGTH_SHORT).show();
                VentasReport.setMontoTotal(0);
                binding.totalVenta.setText("TOTAL VENTA: $");
            }

        } else {
            Toast.makeText(getApplicationContext(), "Adaptador nulo", Toast.LENGTH_SHORT).show();
            VentasReport.setMontoTotal(0);
        }

        toggleProgressBar(false);
    }

    // Método para calcular el monto total de la venta
    private int calcularMontoTotal(List<ProductoVenta> productosVenta) {
        int montoTotal = 0;
        for (ProductoVenta pv : productosVenta) {
            montoTotal += pv.cantidad * pv.producto.getPrecio();
        }
        return montoTotal;
    }

    public void cancelarCompra() {
        toggleProgressBar(true);
        try {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("productos");

            for (ProductoVenta productoVenta : ControlStock) {
                Producto producto = productoVenta.getProducto();
                int cantidadVenta = productoVenta.getCantidad();

                // Calcula la nueva cantidad después de cancelar la venta
                int nuevaCantidad = producto.getCantidad() + cantidadVenta;

                // Añade la actualización al lote
                databaseReference.child(producto.getCodigo()).child("cantidad").setValue(nuevaCantidad);
            }

            // Limpiar la lista de productos vendidos
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) binding.listaProductos.getAdapter();
            adapter.clear();
            adapter.notifyDataSetChanged();


            ControlStock.clear();

            // Limpiar la interfaz de usuario
            limpiarCampos();
            guardarEstadoVentaEnCurso(false);
            Toast.makeText(getApplicationContext(), "Venta cancelada", Toast.LENGTH_SHORT).show();
            VentasReport.setMontoTotal(0);
            finish();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error al cancelar la venta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        toggleProgressBar(false);
    }
    private void enviarALista(){
        toggleProgressBar(true);
        if(productoStored!=null){
            int cantidadVenta = Integer.parseInt(binding.editTextCantidad.getText().toString());

            // Asegurar de que haya suficiente stock disponible
            if (productoStored.getCantidad() >= cantidadVenta) {
                // actualización bdd
                DatabaseReference productoRef = FirebaseDatabase.getInstance().getReference("productos").child(productoStored.getCodigo());
                int nuevaCantidad = productoStored.getCantidad() - cantidadVenta;
                VentasReport.setMontoTotal( VentasReport.getMontoTotal() + (cantidadVenta * productoStored.getPrecio()) );
                binding.totalVenta.setText("TOTAL VENTA: $"+VentasReport.getMontoTotal());
                productoRef.child("cantidad").setValue(nuevaCantidad);
                productoStored.setCantidad(nuevaCantidad);
                ProductoVenta prodAux = new ProductoVenta();
                prodAux.cantidad = cantidadVenta;
                boolean productoExistente = false;

                for (ProductoVenta productoVenta : ControlStock) {
                    if (productoVenta.getProducto().getCodigo().equals(productoStored.getCodigo())) {
                        // Producto ya existe en la lista, actualiza la cantidad
                        productoVenta.setCantidad(productoVenta.getCantidad() + cantidadVenta);
                        productoVenta.producto = productoStored;
                        productoExistente = true;
                        break;
                    }
                }
                if(!productoExistente){

                    prodAux.producto = productoStored;
                    ControlStock.add(prodAux);
                }
               //mover a lista UI
                listaProductosAdapter.add(productoStored.getNombre() + " " + productoStored.getMarca() + " cant: " + prodAux.cantidad);
                listaProductosAdapter.notifyDataSetChanged();
                guardarEstadoVentaEnCurso(true);
                limpiarCampos();
                if(nuevaCantidad==0){
                    // Inflar la vista del Toast personalizado
                    LayoutInflater inflater = getLayoutInflater();
                    View layout = inflater.inflate(R.layout.fondo_toast_alert, null);

// Configurar el texto del TextView en la vista inflada
                    TextView text = layout.findViewById(R.id.textViewToastMessage);
                    text.setText("QUEDARÁ SIN STOCK DE: " + productoStored.getNombre());

// Crear y mostrar el Toast personalizado
                    Toast toast = new Toast(getApplicationContext());
                    toast.setDuration(Toast.LENGTH_SHORT);
                    toast.setView(layout);
                    toast.show();
                }
                productoStored = null;
            } else {
                Toast.makeText(getApplicationContext(), "Stock insuficiente", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(getApplicationContext(), "Debe buscar un nuevo producto", Toast.LENGTH_SHORT).show();
        }
        toggleProgressBar(false);
    }

    //control ante posible cierre durante una venta
    private void guardarEstadoVentaEnCurso(boolean ventaEnCurso) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_VENTA_EN_CURSO, ventaEnCurso);
        editor.apply();
        guardarControlStock();
    }
    private boolean ventaEnCurso() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_VENTA_EN_CURSO, false);
    }

    private void guardarControlStock() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Serializar el ArrayList completo utilizando Gson
        Gson gson = new Gson();
        String controlStockJson = gson.toJson(ControlStock);

        // Guardar la cadena JSON en SharedPreferences
        editor.putString("ControlStock", controlStockJson);

        editor.apply();
    }

    private void restaurarControlStock() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Obtener la cadena JSON almacenada en SharedPreferences
        String controlStockJson = prefs.getString("ControlStock", "");

        if (!controlStockJson.isEmpty()) {
            // Deserializar la cadena JSON utilizando Gson
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<ProductoVenta>>(){}.getType();
            ControlStock = gson.fromJson(controlStockJson, listType);
        } else {
            ControlStock = new ArrayList<>(); // Si no hay datos almacenados, inicializar un ArrayList vacío
        }
    }


    private void toggleProgressBar(boolean show) {
        if (show) {
            binding.progressBar.bringToFront();
            binding.progressBar.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }

    private void limpiarCampos(){
        //Limpiar campos
        binding.imageViewProducto.setImageResource(R.drawable.placeholder_image);
        binding.textViewNombreProducto.setText("");
        binding.editTextCantidad.setText("");
        binding.textViewPrecio.setText("");
    }


    //AÑADIENDO BUSCADOR POR NOMBRE
    private void mostrarDialogoBusqueda() {
        runOnUiThread(() -> {
            // Inflar el diseño personalizado
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_buscar_por_nombre, null);

            // Obtener referencias de widgets
            editTextBuscar = dialogView.findViewById(R.id.editTextBuscar);
            recyclerView = dialogView.findViewById(R.id.recyclerView);

            // Configurar el RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(Venta.this));
            recyclerView.setAdapter(adapterSearch);

            // Configurar el EditText para la búsqueda en tiempo real
            editTextBuscar.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    // Realizar la búsqueda en tiempo real
                    buscarCoincidencias(charSequence.toString());
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });

            // Configurar el listener para mostrar el código al seleccionar un nombre en el RecyclerView
            adapterSearch.setOnItemClickListener((view, position) -> {
                Producto selectedProduct = adapterSearch.getItem(position);
                if (selectedProduct != null) {
                    String selectedProductCode = selectedProduct.getCodigo();

                    obtenerInformacionProducto(selectedProductCode);

                    //Toast.makeText(AddStock.this, "Código del producto: " + selectedProductCode, Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();  // Cerrar el diálogo después de seleccionar
                }
            });

            // Cargar la lista completa de productos
            cargarListaProductos(adapterSearch);

            // Configurar el diálogo
            AlertDialog.Builder builder = new AlertDialog.Builder(Venta.this);
            builder.setView(dialogView);
            alertDialog = builder.create();
            alertDialog.show();
        });
    }

    // Nuevo método para buscar coincidencias en tiempo real
    private void buscarCoincidencias(String searchText) {
        DatabaseReference productosRef = FirebaseDatabase.getInstance().getReference("productos");

        productosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Producto> productosCoincidentes = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Producto producto = snapshot.getValue(Producto.class);
                    if (producto != null) {
                        // Filtrar localmente para obtener posibles coincidencias
                        if (producto.getNombre().toLowerCase().contains(searchText.toLowerCase())) {
                            // Agregar el objeto Producto completo a la lista de coincidencias
                            productosCoincidentes.add(producto);
                        }
                    }
                }

                adapterSearch.clear();
                adapterSearch.addAll(productosCoincidentes);
                adapterSearch.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Error al leer datos de Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void cargarListaProductos(ProductAdapter adapter) {
        DatabaseReference productosRef = FirebaseDatabase.getInstance().getReference("productos");

        productosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Producto> productos = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Producto producto = snapshot.getValue(Producto.class);
                    if (producto != null) {
                        productos.add(producto);
                    }
                }

                adapter.clear();
                adapter.addAll(productos);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Error al leer datos de Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
