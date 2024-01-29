package bd.stock.njoystick;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import bd.stock.njoystick.Models.ProductoVenta;
import bd.stock.njoystick.Models.Ventas;
import bd.stock.njoystick.databinding.ReporteVentasBinding;
public class ReporteVentas extends AppCompatActivity {
    private ReporteVentasBinding binding;
    private ArrayAdapter<String> listaProductosAdapter;
    private ArrayList<Ventas> listaVentas = new ArrayList<>();
    private ArrayList<ProductoVenta> listaFiltrada = new ArrayList<>();
    int totalMangas = 0, totalVideoJuegos = 0, totalFiguras = 0, totalVarios = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ReporteVentasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize the ArrayAdapter with an empty list
        listaProductosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        binding.listaVentas.setAdapter(listaProductosAdapter);

        binding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        binding.opcionReporte.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Esta función se ejecutará cada vez que se modifique la selección del Spinner
                filtrarVentas();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Se llama cuando no hay ninguna selección
            }
        });

        DatabaseReference ventasRef = FirebaseDatabase.getInstance().getReference("ventas");

        ventasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                listaVentas.clear(); // Clear existing data
                for (DataSnapshot ventaSnapshot : dataSnapshot.getChildren()) {
                    Ventas ventas = ventaSnapshot.getValue(Ventas.class);
                    listaVentas.add(ventas);
                }

                filtrarVentas();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Log to check if onCancelled is being called and to show the error
                Toast.makeText(getApplicationContext(), "Error al leer datos de Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarVentas() {
        listaProductosAdapter.clear();
        totalMangas = 0;
        totalVideoJuegos = 0;
        totalFiguras = 0;
        totalVarios = 0;

        for (Ventas venta : listaVentas) {
            if (fechaMismoDia(venta.getFecha()) && "Diario".equals(binding.opcionReporte.getSelectedItem().toString())) {
                listaFiltrada.addAll(venta.getListaProductosVenta());
                for (ProductoVenta pv : venta.getListaProductosVenta()) {
                    listaProductosAdapter.add(formatProducto(pv));
                    sumarMontos(pv);
                }
            }
            if (fechaMismaSemana(venta.getFecha()) && "Semanal".equals(binding.opcionReporte.getSelectedItem().toString())) {
                listaFiltrada.addAll(venta.getListaProductosVenta());
                for (ProductoVenta pv : venta.getListaProductosVenta()) {
                    listaProductosAdapter.add(formatProducto(pv));
                    sumarMontos(pv);
                }
            }
            if (fechaMismoMes(venta.getFecha()) && "Mensual".equals(binding.opcionReporte.getSelectedItem().toString())) {
                listaFiltrada.addAll(venta.getListaProductosVenta());
                for (ProductoVenta pv : venta.getListaProductosVenta()) {
                    listaProductosAdapter.add(formatProducto(pv));
                    sumarMontos(pv);
                }
            }
        }
        listaProductosAdapter.notifyDataSetChanged();
        generarGrafico();
    }

    private String formatProducto(ProductoVenta pv) {
        // Formatear el producto como desees para mostrarlo en la lista
        return pv.producto.getNombre() + " " + pv.producto.getMarca() + " cantidad: " + pv.cantidad;
    }

    private void generarGrafico() {
        // Configuración del gráfico
        //binding.grafico.setUsePercentValues(true);
        binding.grafico.getDescription().setEnabled(false);

        // Configuración de los valores y etiquetas
        ArrayList<PieEntry> entries = new ArrayList<>();
        if(totalMangas!=0){
            entries.add(new PieEntry(totalMangas, "Mangas"));
        }
        if(totalVideoJuegos!=0){
            entries.add(new PieEntry(totalVideoJuegos, "Videojuegos"));
        }
        if(totalFiguras!=0){
            entries.add(new PieEntry(totalFiguras, "Figuras"));
        }
        if(totalVarios!=0){
            entries.add(new PieEntry(totalVarios, "Varios"));
        }

        // Configuración del conjunto de datos del gráfico
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setDrawValues(false);  // Oculta las etiquetas en la leyenda

        // Oculta la leyenda completamente
        binding.grafico.getLegend().setEnabled(false);

        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        // Crear el conjunto de datos del gráfico
        PieData data = new PieData(dataSet);
        binding.grafico.setData(data);


        // Configura la descripción (puedes omitirla si no la necesitas)
        binding.grafico.getDescription().setEnabled(false);
        binding.grafico.animateY(1300);
        // Actualiza la vista del gráfico
        binding.grafico.invalidate();
    }

    private void sumarMontos(ProductoVenta pv) {
        switch (pv.producto.getCategoria()) {
            case "Videojuegos":
                totalVideoJuegos += (pv.cantidad * pv.producto.getPrecio());
                break;
            case "Mangas":
                totalMangas += (pv.cantidad * pv.producto.getPrecio());
                break;
            case "Figuras":
                totalFiguras += (pv.cantidad * pv.producto.getPrecio());
                break;
            default:
                totalVarios += (pv.cantidad * pv.producto.getPrecio());
                break;
        }
    }

    private static boolean fechaMismoDia(Date date) {
        Calendar currentDate = Calendar.getInstance();
        Calendar dateToCheck = Calendar.getInstance();
        dateToCheck.setTime(date);

        return currentDate.get(Calendar.YEAR) == dateToCheck.get(Calendar.YEAR)
                && currentDate.get(Calendar.MONTH) == dateToCheck.get(Calendar.MONTH)
                && currentDate.get(Calendar.DAY_OF_MONTH) == dateToCheck.get(Calendar.DAY_OF_MONTH);
    }

    private static boolean fechaMismaSemana(Date date) {
        Calendar currentDate = Calendar.getInstance();
        Calendar dateToCheck = Calendar.getInstance();
        dateToCheck.setTime(date);

        return currentDate.get(Calendar.YEAR) == dateToCheck.get(Calendar.YEAR)
                && currentDate.get(Calendar.WEEK_OF_YEAR) == dateToCheck.get(Calendar.WEEK_OF_YEAR);
    }

    private static boolean fechaMismoMes(Date date) {
        Calendar currentDate = Calendar.getInstance();
        Calendar dateToCheck = Calendar.getInstance();
        dateToCheck.setTime(date);

        return currentDate.get(Calendar.YEAR) == dateToCheck.get(Calendar.YEAR)
                && currentDate.get(Calendar.MONTH) == dateToCheck.get(Calendar.MONTH);
    }
}