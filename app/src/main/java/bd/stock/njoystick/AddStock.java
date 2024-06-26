package bd.stock.njoystick;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import bd.stock.njoystick.Models.ProductAdapter;
import bd.stock.njoystick.Models.Producto;
import bd.stock.njoystick.Services.CaptureActivityPortrait;
import bd.stock.njoystick.databinding.AddStockBinding;

import androidx.core.content.FileProvider;
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
    private Spinner tomoDoble;
    private Uri imagenUri = null;
    private ProgressBar progressBar;
    private String urlAux = null;
    private AlertDialog alertDialog;
    private ProductAdapter adapterSearch;
    private EditText editTextBuscar;
    private RecyclerView recyclerView;
    private String currentPhotoPath;

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
        if (!isTaskRoot() && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER) && getIntent().getAction() != null &&
                getIntent().getAction().equals(Intent.ACTION_MAIN)) {
            finish();
            return;
        }
        binding = AddStockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        adapterSearch =  new ProductAdapter(this, new ArrayList<>());

        spinner = findViewById(R.id.spinner);
        btnTomarFoto = findViewById(R.id.btn_tomarFoto);
        btnGuardarProducto = findViewById(R.id.btn_guardarProducto);
        codigoProducto = findViewById(R.id.codigoProducto);
        nombreProducto = findViewById(R.id.nombreProducto);
        marcaProducto = findViewById(R.id.marcaProducto);
        tomoDoble = findViewById(R.id.selecTipoTomo);
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
                mostrarOpcionesDialog();
            }
        });

        btnGuardarProducto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String codigoProductoStr = codigoProducto.getText().toString();
                if(codigoProductoStr.trim().isEmpty()){
                    Toast.makeText(getApplicationContext(), "INGRESE PRIMERO EL CODIGO A TRABAJAR", Toast.LENGTH_SHORT).show();
                }else{
                    if (imagenUri != null) {
                        toggleProgressBar(true);

                        // Obtener la referencia del almacenamiento en Firebase con la carpeta "imagenes_productos" y el nombre del archivo como el código del producto
                        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("imagenes_productos/" + codigoProductoStr);

                        // Subir la imagen al Storage
                        storageRef.putFile(imagenUri)
                                .addOnSuccessListener(taskSnapshot -> {
                                    // La imagen se ha subido exitosamente, obtén la URL de descarga
                                    obtenerURLDescarga(storageRef, codigoProductoStr);
                                })
                                .addOnFailureListener(e -> {
                                    // Handle unsuccessful uploads
                                    Toast.makeText(getApplicationContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show();
                                    toggleProgressBar(false);
                                });
                    } else if(urlAux!=null){
                        toggleProgressBar(true);
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

        binding.btnEliminarProducto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(urlAux!=null){
                    mostrarConfirmacionDialog();
                }else{
                    Toast.makeText(getApplicationContext(), "Debe seleccionar un producto existente en sistema", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void mostrarConfirmacionDialog() {
        // Supongamos que "codigoProducto" y "nombreProducto" son las variables que contienen la información del producto
        final String codigoProducto = binding.codigoProducto.getText().toString();
        final String nombreProducto = binding.nombreProducto.getText().toString();

        // Crea un cuadro de diálogo de confirmación
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmar Eliminación");
        builder.setMessage("¿Seguro que desea eliminar el producto '" + nombreProducto + "' de código '" + codigoProducto + "'?");

        // Agrega los botones "Sí" y "Cancelar" al cuadro de diálogo
        builder.setPositiveButton("ELIMINAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Si el usuario hace clic en "Sí", elimina el producto
                eliminarProducto(codigoProducto);
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Si el usuario hace clic en "Cancelar", cierra el cuadro de diálogo sin hacer nada
                dialogInterface.dismiss();
            }
        });

        // Muestra el cuadro de diálogo
        builder.show();
    }

    private void eliminarProducto(String codigoProducto) {
        toggleProgressBar(true);
        DatabaseReference productoEliminarRef = FirebaseDatabase.getInstance().getReference("productos").child(codigoProducto);
        productoEliminarRef.removeValue();
        binding.codigoProducto.setText("");
        binding.cantidad.setText("");
        binding.descripcion.setText("");
        binding.marcaProducto.setText("");
        binding.precio.setText("");
        binding.imagenProducto.setImageResource(R.drawable.placeholder_image);
        urlAux=null;
        Toast.makeText(getApplicationContext(), "PRODUCTO ELIMINADO", Toast.LENGTH_SHORT).show();
        toggleProgressBar(false);
    }


    private void mostrarOpcionesDialog() {
        CharSequence[] opciones = {"Tomar Foto", "Seleccionar desde Galería"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Elige una opción");
        builder.setItems(opciones, (dialog, item) -> {
            if (item == 0) {
                // Tomar foto con la cámara
                dispatchTakePictureIntent();
            } else if (item == 1) {
                // Seleccionar imagen desde la galería
                seleccionarImagenDesdeGaleria();
            }
        });
        builder.show();
    }

    private void seleccionarImagenDesdeGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "bd.stock.njoystick.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
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
                Integer.parseInt(cantidad.getText().toString()), descripcion.getText().toString(),
                spinner.getSelectedItem().toString(),urlDescarga,Integer.parseInt( precio.getText().toString()) , binding.selecTipoTomo.getSelectedItem().toString());

        // Obtener la referencia a la base de datos en Firebase
        DatabaseReference productosRef = FirebaseDatabase.getInstance().getReference("productos");

        // Guardar el producto en la base de datos
        productosRef.child(codigoProducto).setValue(producto)
                .addOnSuccessListener(aVoid -> {
                    // Éxito al guardar el producto
                    Toast.makeText(getApplicationContext(), "Producto guardado exitosamente", Toast.LENGTH_SHORT).show();
                    binding.codigoProducto.setText("");
                    binding.cantidad.setText("");
                    binding.descripcion.setText("");
                    binding.marcaProducto.setText("");
                    binding.precio.setText("");
                    binding.imagenProducto.setImageResource(R.drawable.placeholder_image);
                    //MODIFICAR PRECIO DE LOS MANGAS CUANDO SE CUMPLAN LAS CONDICIONES ESTABLECIDAS POR EL CLIENTE.
                    if(producto.getCategoria().equals("Mangas") && ( producto.getMarca().equalsIgnoreCase("Ivrea") || producto.getMarca().equalsIgnoreCase("Panini"))
                            && (producto.getTipoTomo().equalsIgnoreCase("Tomo Simple") || producto.getTipoTomo().equalsIgnoreCase("Tomo Doble"))){
                        Toast.makeText(getApplicationContext(), "Modificando precios de mangas coincidentes", Toast.LENGTH_SHORT).show();

                        DatabaseReference prodRef = FirebaseDatabase.getInstance().getReference("productos");

                        // Consultar productos con la misma marca y tipo de tomo
                        prodRef.orderByChild("marca").equalTo(producto.getMarca())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                            Producto productoRelacionado = snapshot.getValue(Producto.class);

                                            if (productoRelacionado != null &&
                                                    productoRelacionado.getTipoTomo().equalsIgnoreCase(producto.getTipoTomo())) {
                                                // Modificar el precio del producto relacionado
                                                int nuevoPrecio =producto.getPrecio();
                                                productoRelacionado.setPrecio(nuevoPrecio);

                                                // Actualizar el precio en Firebase
                                                prodRef.child(snapshot.getKey()).setValue(productoRelacionado);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        // Manejar el error en la consulta
                                    }
                                });
                    }
                    urlAux=null;
                    imagenUri=null;
                    toggleProgressBar(false);
                })
                .addOnFailureListener(e -> {
                    // Error al guardar el producto
                    Toast.makeText(getApplicationContext(), "Error al guardar el producto", Toast.LENGTH_SHORT).show();
                    urlAux=null;
                    imagenUri=null;
                    toggleProgressBar(false);
                });
        urlAux=null;
        imagenUri=null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            // Imagen seleccionada desde la galería
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                imageView.setImageURI(selectedImageUri);
                imagenUri = selectedImageUri;
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Imagen capturada con la cámara
            if (currentPhotoPath != null) {
                Uri imageUri = Uri.fromFile(new File(currentPhotoPath));
                imageView.setImageURI(imageUri);
                imagenUri = imageUri;
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
                    opciones = getResources().getStringArray(R.array.opciones_tipoTomo);
                    posicionSeleccionada = -1;
                    for (int i = 0; i < opciones.length; i++) {
                        if (opciones[i].equals(productoExistente.getTipoTomo())) {
                            posicionSeleccionada = i;
                            break;  // Detener la búsqueda una vez encontrado
                        }
                    }
                    if (posicionSeleccionada != -1) {
                        // Seleccionar el elemento en el Spinner
                        tomoDoble.setSelection(posicionSeleccionada);
                    } else {
                        // Manejar el caso en el que el elemento no se encuentre en la lista
                        Toast.makeText(getApplicationContext(), "El tipo de tomo no coincide", Toast.LENGTH_SHORT).show();
                    }

                    Picasso.get().load(productoExistente.getUrlImagen()).into(binding.imagenProducto);
                    urlAux = productoExistente.getUrlImagen();
                    imagenUri = null;
                } else {
                    // El producto no existe en la base de datos
                    Toast.makeText(getApplicationContext(), "Ingresará un nuevo producto", Toast.LENGTH_SHORT).show();
                    imagenUri = null;
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

                    binding.codigoProducto.setText(selectedProductCode);
                    verificarExistenciaEnFirebase(selectedProductCode);

                    //Toast.makeText(AddStock.this, "Código del producto: " + selectedProductCode, Toast.LENGTH_SHORT).show();
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
