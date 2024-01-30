package bd.stock.njoystick;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import bd.stock.njoystick.databinding.ControlStockBinding;
import bd.stock.njoystick.databinding.VentaBinding;

public class ControlStock extends AppCompatActivity {
    private ArrayAdapter<String> listaProductosStock;
    ControlStockBinding binding; // Utiliza la clase de binding correcta
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_stock);
        try {
            // Inicializa el ArrayAdapter y asócialo con el ListView
            listaProductosStock = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            binding.ListaProductoStock.setAdapter(listaProductosStock);
            listaProductosStock.add("hola");
            listaProductosStock.notifyDataSetChanged();
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }




}
