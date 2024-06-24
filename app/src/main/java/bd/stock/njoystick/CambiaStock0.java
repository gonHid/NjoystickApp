package bd.stock.njoystick;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import bd.stock.njoystick.databinding.CambiarStockA0Binding;
import bd.stock.njoystick.Models.Producto;

public class CambiaStock0 extends AppCompatActivity {
    CambiarStockA0Binding binding;
    boolean modificado;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cambiar_stock_a_0);
        binding = CambiarStockA0Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        DatabaseReference productosRef = FirebaseDatabase.getInstance().getReference("productos");

        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modificado = false;
                productosRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot productoSnapshot : dataSnapshot.getChildren()) {
                            Producto producto = productoSnapshot.getValue(Producto.class);
                            if(producto!=null && "Videojuegos".equals(producto.getCategoria())){
                                productoSnapshot.getRef().child("cantidad").setValue(0);
                               modificado=true;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Log to check if onCancelled is being called and to show the error
                        Toast.makeText(getApplicationContext(), "Error al leer datos de Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
                if(modificado)
                    Toast.makeText(getApplicationContext(), "STOCK MODIFICADO", Toast.LENGTH_SHORT).show();
            }
        });
        binding.btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
