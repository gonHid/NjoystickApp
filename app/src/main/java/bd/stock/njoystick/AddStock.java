package bd.stock.njoystick;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import bd.stock.njoystick.databinding.AddStockBinding;

public class AddStock extends AppCompatActivity {


    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;


    private AddStockBinding binding;
    private Spinner spinner;
    private ImageView imageView;
    private Button btnTomarFoto, btnGuardarProducto;
    private EditText codigoProducto;
    private EditText nombreProducto;
    private EditText marcaProducto;
    private EditText cantidad;
    private EditText precio;
    private EditText descripcion;
    private CheckBox alternativo;
    private Uri imagenUri = null;
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Toast.makeText(this, "CANCELADO", Toast.LENGTH_SHORT).show();
        } else {
            binding.codigoProducto.setText(result.getContents());
            verificarExistenciaEnFirebase(result.getContents());
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Utilizar la clase de binding correcta
        binding = AddStockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        spinner = findViewById(R.id.spinner);
        btnTomarFoto = findViewById(R.id.btn_tomarFoto);
        btnGuardarProducto = findViewById(R.id.btn_guardarProducto);
        codigoProducto = findViewById(R.id.codigoProducto);
        nombreProducto = findViewById(R.id.nombreProducto);
        marcaProducto = findViewById(R.id.marcaProducto);
        alternativo = findViewById(R.id.isAlternativo);
        cantidad = findViewById(R.id.cantidad);
        precio = findViewById(R.id.precio);
        descripcion = findViewById(R.id.descripcion);
        imageView = findViewById(R.id.imagenProducto);
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
                        cantidad.getText().toString().isEmpty() ||
                        descripcion.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                } else {
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
                            });
                }
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE_SECURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
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
                    Toast.makeText(getApplicationContext(), "Error al obtener la URL de descarga", Toast.LENGTH_SHORT).show();
                });
    }

    // Método para guardar los datos del producto en Firebase Realtime Database
    private void guardarProductoEnFirebase(String codigoProducto, String urlDescarga) {
        // Crear un objeto Producto con todos los datos, incluida la URL de descarga de la imagen
        Producto producto = new Producto(codigoProducto, nombreProducto.getText().toString(), marcaProducto.getText().toString(),
                Integer.parseInt( precio.getText().toString()),alternativo.isChecked(),
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
                })
                .addOnFailureListener(e -> {
                    // Error al guardar el producto
                    Toast.makeText(getApplicationContext(), "Error al guardar el producto", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        imageView.setImageBitmap(imageBitmap);
                        // Guardar la URI de la imagen para usarla al guardar el producto
                        imagenUri = getImageUri(getApplicationContext(), imageBitmap);
                    } else {
                        throw new IOException("Bitmap es nulo después de capturar la imagen");
                    }
                } else {
                    throw new IOException("Extras es nulo después de capturar la imagen");
                }
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
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

                    // Aquí puedes realizar otras acciones si el producto existe
                } else {
                    // El producto no existe en la base de datos
                    Toast.makeText(getApplicationContext(), "Ingresará un nuevo producto", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Error al leer datos de Firebase", Toast.LENGTH_SHORT).show();
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

    public void volverAtras(View view) {
        finish(); // Cierra la actividad actual y vuelve a la actividad anterior (si existe).
    }
}
