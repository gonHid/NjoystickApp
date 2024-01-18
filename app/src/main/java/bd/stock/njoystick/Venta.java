package bd.stock.njoystick;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import bd.stock.njoystick.databinding.VentaBinding; // Asegúrate de importar la clase correcta

public class Venta extends AppCompatActivity {

    VentaBinding binding; // Utiliza la clase de binding correcta
    private ArrayAdapter<String> listaProductosAdapter;
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

        // Inicializa el ArrayAdapter y asócialo con el ListView
        listaProductosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        binding.listaProductos.setAdapter(listaProductosAdapter);

        binding.btnLeerCodigo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                escanear();
            }
        });
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
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Producto producto = dataSnapshot.getValue(Producto.class);
                    if (producto != null) {
                        // Muestra la imagen del producto en el ImageView correspondiente
                        // Utiliza la URL de la imagen almacenada en el objeto 'producto'
                        // Puedes usar una biblioteca como Picasso o Glide para cargar la imagen desde la URL
                        // Picasso.get().load(producto.getUrlImagen()).into(binding.imageViewProducto);

                        // Añade el nombre del producto a la lista
                        listaProductosAdapter.add(producto.getNombre());

                        // Notifica al adaptador que los datos han cambiado
                        listaProductosAdapter.notifyDataSetChanged();

                        // Actualiza la interfaz de usuario con la información del producto
                        actualizarUI(producto);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Producto no encontrado", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Error al leer datos de Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarUI(Producto producto) {
        // Actualiza la interfaz de usuario con la información del producto
        binding.textViewNombreProducto.setText(producto.getNombre());
        binding.textViewPrecio.setText(String.valueOf(producto.getPrecio()));
        // ... (agrega más actualizaciones según sea necesario)
    }
}
