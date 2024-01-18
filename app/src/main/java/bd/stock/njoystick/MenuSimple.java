package bd.stock.njoystick;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
public class MenuSimple extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_simple);
        showToast("Entrando a menu");
        Button buttonGoToVenta = findViewById(R.id.changeVenta);
        Button buttonGoToStock = findViewById(R.id.changeVerStock);
        Button buttonGoToAnnadir = findViewById(R.id.changeAnnadirStock);
        Button buttonGoToReport = findViewById(R.id.changeReportarBug);

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
    }
    private void showToast(String message) {
        // Método auxiliar para mostrar Toasts
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
