package bd.stock.njoystick;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class MenuSimple extends AppCompatActivity{
    private static final String PREFERENCIA_MODO_NOCTURNO = "modo_nocturno";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_simple);
        Button buttonGoToVenta = findViewById(R.id.changeVenta);
        Button buttonGoToStock = findViewById(R.id.changeVerStock);
        Button buttonGoToAnnadir = findViewById(R.id.changeAnnadirStock);
        Button buttonGoToReportesVentas = findViewById(R.id.changeReportesVentas);
        Button buttonCambarStock0 = findViewById(R.id.btn_Stock0);
        SwitchMaterial switchModoNocturno = findViewById(R.id.switchTema);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean modoNocturno = preferences.getBoolean(PREFERENCIA_MODO_NOCTURNO, false);

        // Aplicar el modo nocturno según la preferencia
        if (modoNocturno) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        buttonGoToVenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ir a la actividad Venta
                Intent intent = new Intent(MenuSimple.this, Venta.class);
                startActivity(intent);
            }
        });
        buttonGoToAnnadir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ir a la actividad Venta
                Intent intent = new Intent(MenuSimple.this, AddStock.class);
                startActivity(intent);
            }
        });

        buttonGoToReportesVentas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ir a la actividad Venta
                Intent intent = new Intent(MenuSimple.this, ReporteVentas.class);
                startActivity(intent);
            }
        });

        buttonGoToStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ir a la actividad Venta
                Intent intent = new Intent(MenuSimple.this, ControlStock.class);
                startActivity(intent);
            }
        });
        buttonCambarStock0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuSimple.this, CambiaStock0.class);
                startActivity(intent);
            }
        });

        switchModoNocturno.setChecked(modoNocturno);
        switchModoNocturno.setOnCheckedChangeListener((buttonView, isChecked) -> cambiarTema(isChecked));
    }

    private void cambiarTema(boolean modoNocturno) {
        // Guardar la preferencia del modo nocturno
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREFERENCIA_MODO_NOCTURNO, modoNocturno);
        editor.apply();

        // Aplicar el modo nocturno
        if (modoNocturno) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        // Reiniciar la actividad para aplicar los cambios
        recreate();
    }
}
