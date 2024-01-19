package bd.stock.njoystick;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import bd.stock.njoystick.databinding.VentaBinding; // Asegúrate de importar la clase correcta

public class Venta extends AppCompatActivity {

    VentaBinding binding; // Utiliza la clase de binding correcta
    private ArrayAdapter<String> listaProductosAdapter;
    private ArrayList<ProductoVenta> ControlStock = new ArrayList<>();
    private Producto productoStored = null;
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

        binding = VentaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.imageViewProducto.setImageResource(R.drawable.placeholder_image);
        // Inicializa el ArrayAdapter y asócialo con el ListView
        listaProductosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        binding.listaProductos.setAdapter(listaProductosAdapter);

        binding.btnLeerCodigo.setOnClickListener(view -> escanear());
        binding.btnAddALista.setOnClickListener(view -> enviarALista());
        binding.btnRealizarVenta.setOnClickListener(view -> procesarCompra());
        binding.btnCancelarVenta.setOnClickListener(view -> cancelarCompra());
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
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Producto no encontrado", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Error al leer datos de Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void procesarCompra(){
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) binding.listaProductos.getAdapter();
        adapter.clear();
        adapter.notifyDataSetChanged();
        limpiarCampos();
        ControlStock.clear();
    }

    public void cancelarCompra() {
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

            Toast.makeText(getApplicationContext(), "Venta cancelada", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error al cancelar la venta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void enviarALista(){
        if(productoStored!=null){
            int cantidadVenta = Integer.parseInt(binding.editTextCantidad.getText().toString());

            // Asegurar de que haya suficiente stock disponible
            if (productoStored.getCantidad() >= cantidadVenta) {
                // actualización bdd
                DatabaseReference productoRef = FirebaseDatabase.getInstance().getReference("productos").child(productoStored.getCodigo());
                int nuevaCantidad = productoStored.getCantidad() - cantidadVenta;
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

                limpiarCampos();
                productoStored = null;
            } else {
                Toast.makeText(getApplicationContext(), "Stock insuficiente", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(getApplicationContext(), "Debe buscar un nuevo producto", Toast.LENGTH_SHORT).show();
        }
    }

    private void limpiarCampos(){
        //Limpiar campos
        binding.imageViewProducto.setImageResource(R.drawable.placeholder_image);
        binding.textViewNombreProducto.setText("");
        binding.editTextCantidad.setText("");
        binding.textViewPrecio.setText("");
    }
}
