package bd.stock.njoystick;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import androidx.appcompat.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import bd.stock.njoystick.Models.ProductAdapter;
import bd.stock.njoystick.Models.Producto;
import bd.stock.njoystick.Services.CaptureActivityPortrait;
import bd.stock.njoystick.databinding.AddStockBinding;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
public class AddStock extends AppCompatActivity {


    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;


    private AddStockBinding binding;
    private Spinner spinner;
    private ImageView imageView;
    private Button btnTomarFoto, btnGuardarProducto;
    private EditText codigoProducto, nombreProducto, marcaProducto, cantidad, precio,  descripcion;
    private CheckBox tomoDoble;
    private Uri imagenUri = null;
    private ProgressBar progressBar;
    private String urlAux = null;
    private AlertDialog alertDialog;
    private ProductAdapter adapterSearch;
    private EditText editTextBuscar;
    private RecyclerView recyclerView;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Toast.makeText(this, "CANCELADO", Toast.LENGTH_SHORT).show();
        } else {
            binding.codigoProducto.setText(result.getContents());
            toggleProgressBar(true);
            verificarExistenciaEnFirebase(result.getContents());
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Utilizar la clase de binding correcta
        binding = AddStockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        adapterSearch =  new ProductAdapter(this, new ArrayList<>());

        spinner = findViewById(R.id.spinner);
        btnTomarFoto = findViewById(R.id.btn_tomarFoto);
        btnGuardarProducto = findViewById(R.id.btn_guardarProducto);
        codigoProducto = findViewById(R.id.codigoProducto);
        nombreProducto = findViewById(R.id.nombreProducto);
        marcaProducto = findViewById(R.id.marcaProducto);
        tomoDoble = findViewById(R.id.isTomoDoble);
        cantidad = findViewById(R.id.cantidad);
        precio = findViewById(R.id.precio);
        descripcion = findViewById(R.id.descripcion);
        imageView = findViewById(R.id.imagenProducto);
        progressBar = findViewById(R.id.progressBar);
        // Obtener las opciones del array de recursos
        String[] opciones = getResources().getStringArray(R.array.opciones_spinner);

        // Crear un ArrayAdapter usando el array de opciones y un diseño simple
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opciones);

        // Especificar el diseño a usar cuando la lista de opciones aparezca
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Asignar el adaptador al Spinner
        spinner.setAdapter(adapter);

        binding.btnLeerCodigo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                escanear();
            }
        });

        btnTomarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        btnGuardarProducto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (codigoProducto.getText().toString().isEmpty() ||
                        nombreProducto.getText().toString().isEmpty() ||
                        marcaProducto.getText().toString().isEmpty() ||
                        cantidad.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                } else {
                    if(imagenUri!=null){
                        toggleProgressBar(true);
                        // Obtener la referencia del almacenamiento en Firebase
                        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("imagenes_productos/" + codigoProducto.getText().toString());

                        // Subir la imagen al Storage
                        storageRef.putFile(imagenUri)
                                .addOnSuccessListener(taskSnapshot -> {
                                    // La imagen se ha subido exitosamente, obtén la URL de descarga
                                    obtenerURLDescarga(storageRef, codigoProducto.getText().toString());
                                })
                                .addOnFailureListener(e -> {
                                    // Handle unsuccessful uploads
                                    Toast.makeText(getApplicationContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show();
                                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    toggleProgressBar(false);
                                });
                    }else if(urlAux!=null){
                        guardarProductoEnFirebase(codigoProducto.getText().toString(),urlAux);
                    }else{
                        Toast.makeText(getApplicationContext(), "Debe añadir una foto", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });

        binding.btnCancelar.setOnClickListener(view -> finish());
        binding.btnBuscarPorNombre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoBusqueda();
            }
        });
    }

    private void dispatchTakePictureIntent() {
        // Crear un Intent para abrir la galería
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        // Crear un Intent para tomar la foto
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE_SECURE);

        // Crear un Intent que agrupe ambos Intents anteriores
        Intent chooserIntent = Intent.createChooser(pickPhoto, "Seleccionar foto o tomar una nueva");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePictureIntent});

        // Verificar si hay aplicaciones disponibles para manejar el Intent
        if (chooserIntent.resolveActivity(getPackageManager()) != null) {
            try {
                // Iniciar la actividad usando el Intent agrupado
                startActivityForResult(chooserIntent, REQUEST_IMAGE_CAPTURE);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error al iniciar la captura de imagen", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "No hay una aplicación de cámara o galería disponible", Toast.LENGTH_SHORT).show();
        }
    }

    // Nuevo método para obtener la URL de descarga de la imagen
    private void obtenerURLDescarga(StorageReference storageRef, String codigoProducto) {
        storageRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    // Aquí puedes obtener la URL de descarga y guardarla junto con otros datos del producto
                    String urlDescarga = uri.toString();
                    // Puedes utilizar la URL de descarga como desees, por ejemplo, guardar el producto en Firebase Realtime Database
                    guardarProductoEnFirebase(codigoProducto, urlDescarga);
                })
                .addOnFailureListener(e -> {
                    // Handle any errors
                    toggleProgressBar(false);
                    Toast.makeText(getApplicationContext(), "Error al obtener la URL de descarga", Toast.LENGTH_SHORT).show();
                });
    }

    // Método para guardar los datos del producto en Firebase Realtime Database
    private void guardarProductoEnFirebase(String codigoProducto, String urlDescarga) {
        // Crear un objeto Producto con todos los datos, incluida la URL de descarga de la imagen
        Producto producto = new Producto(codigoProducto, nombreProducto.getText().toString(), marcaProducto.getText().toString(),
                Integer.parseInt( precio.getText().toString()),tomoDoble.isChecked(),
                Integer.parseInt(cantidad.getText().toString()), descripcion.getText().toString(), spinner.getSelectedItem().toString(), urlDescarga);

        // Obtener la referencia a la base de datos en Firebase
        DatabaseReference productosRef = FirebaseDatabase.getInstance().getReference("productos");

        // Guardar el producto en la base de datos
        productosRef.child(codigoProducto).setValue(producto)
                .addOnSuccessListener(aVoid -> {
                    // Éxito al guardar el producto
                    Toast.makeText(getApplicationContext(), "Producto guardado exitosamente", Toast.LENGTH_SHORT).show();
                    binding.codigoProducto.setText("");
                    binding.cantidad.setText("1");
                    binding.descripcion.setText("");
                    binding.marcaProducto.setText("");
                    binding.precio.setText("0");
                    binding.imagenProducto.setImageResource(R.drawable.placeholder_image);
                    urlAux=null;
                    toggleProgressBar(false);
                })
                .addOnFailureListener(e -> {
                    // Error al guardar el producto
                    Toast.makeText(getApplicationContext(), "Error al guardar el producto", Toast.LENGTH_SHORT).show();
                    urlAux=null;
                    toggleProgressBar(false);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    // Usar la Uri directamente
                    imageView.setImageURI(imageUri);
                    // Guardar la URI de la imagen para usarla al guardar el producto
                    imagenUri = imageUri;
                } else {
                    throw new IOException("Uri de la imagen es nula después de capturar la imagen");
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void verificarExistenciaEnFirebase(String codigoProducto) {
        DatabaseReference productosRef = FirebaseDatabase.getInstance().getReference("productos").child(codigoProducto);
        productosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // El producto existe en la base de datos
                    Toast.makeText(getApplicationContext(), "Ya existe, se sobrescribirá el producto", Toast.LENGTH_SHORT).show();
                    Producto productoExistente = dataSnapshot.getValue(Producto.class);
                    binding.nombreProducto.setText(productoExistente.getNombre());
                    binding.marcaProducto.setText(productoExistente.getMarca());
                    binding.cantidad.setText(String.valueOf(productoExistente.getCantidad()));
                    binding.precio.setText(String.valueOf(productoExistente.getPrecio()));
                    binding.descripcion.setText(productoExistente.getDescripcion());
                    String[] opciones = getResources().getStringArray(R.array.opciones_spinner);
                    int posicionSeleccionada = -1;
                    for (int i = 0; i < opciones.length; i++) {
                        if (opciones[i].equals(productoExistente.getCategoria())) {
                            posicionSeleccionada = i;
                            break;  // Detener la búsqueda una vez encontrado
                        }
                    }
                    if (posicionSeleccionada != -1) {
                        // Seleccionar el elemento en el Spinner
                        spinner.setSelection(posicionSeleccionada);
                    } else {
                        // Manejar el caso en el que el elemento no se encuentre en la lista
                        Toast.makeText(getApplicationContext(), "La categoria no coincide", Toast.LENGTH_SHORT).show();
                    }

                    Picasso.get().load(productoExistente.getUrlImagen()).into(binding.imagenProducto);
                    urlAux = productoExistente.getUrlImagen();
                } else {
                    // El producto no existe en la base de datos
                    Toast.makeText(getApplicationContext(), "Ingresará un nuevo producto", Toast.LENGTH_SHORT).show();
                }
                toggleProgressBar(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Error al leer datos de Firebase", Toast.LENGTH_SHORT).show();
                toggleProgressBar(false);
            }
        });
    }

    // Obtener la URI de una imagen a partir de un Bitmap
    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
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

    // Método para mostrar u ocultar el ProgressBar
    private void toggleProgressBar(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        } else {
            progressBar.setVisibility(View.GONE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }

    //BUSQUEDA POR NOMBRE
    private void mostrarDialogoBusqueda() {
        runOnUiThread(() -> {
            // Inflar el diseño personalizado
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_buscar_por_nombre, null);

            // Obtener referencias de widgets
            editTextBuscar = dialogView.findViewById(R.id.editTextBuscar);
            recyclerView = dialogView.findViewById(R.id.recyclerView);

            // Configurar el RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(AddStock.this));
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
                    // Puedes mostrar el código como desees
                    Toast.makeText(AddStock.this, "Código del producto: " + selectedProductCode, Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();  // Cerrar el diálogo después de seleccionar
                }
            });

            // Cargar la lista completa de productos
            cargarListaProductos(adapterSearch);

            // Configurar el diálogo
            AlertDialog.Builder builder = new AlertDialog.Builder(AddStock.this);
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

    public void volverAtras(View view) {
        finish(); // Cierra la actividad actual y vuelve a la actividad anterior (si existe).
    }
}
