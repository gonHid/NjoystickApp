package bd.stock.njoystick;

import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import bd.stock.njoystick.databinding.ReporteVentasBinding;

public class ReporteVentas extends AppCompatActivity {
    ReporteVentasBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ReporteVentasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

    }
}
