package bd.stock.njoystick.Models;

import java.util.ArrayList;
import java.util.Date;

public class Ventas {
    private ArrayList<ProductoVenta> listaProductosVenta;
    private Date fecha;
    private int MontoTotal;

    public Ventas(ArrayList<ProductoVenta> listaProductosVenta, Date fecha, int montoTotal) {
        this.listaProductosVenta = listaProductosVenta;
        this.fecha = fecha;
        MontoTotal = montoTotal;
    }

    public ArrayList<ProductoVenta> getListaProductosVenta() {
        return listaProductosVenta;
    }

    public void setListaProductosVenta(ArrayList<ProductoVenta> listaProductosVenta) {
        this.listaProductosVenta = listaProductosVenta;
    }

    public Ventas() {
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public int getMontoTotal() {
        return MontoTotal;
    }

    public void setMontoTotal(int montoTotal) {
        MontoTotal = montoTotal;
    }
}
