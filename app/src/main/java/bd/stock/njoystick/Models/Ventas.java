package bd.stock.njoystick.Models;

import java.util.ArrayList;
import java.util.Date;

public class Ventas {// para guardar en firebase la informacion de las ventas realizadas
    private String listaProductosVenta;
    private Date fecha;
    private int MontoTotal;

    public Ventas(String listaProductosVenta, Date fecha, int montoTotal) {
        this.listaProductosVenta = listaProductosVenta;
        this.fecha = fecha;
        MontoTotal = montoTotal;
    }

    public String getListaProductosVenta() {
        return listaProductosVenta;
    }

    public void setListaProductosVenta(String listaProductosVenta) {
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
        return    MontoTotal;
    }

    public void setMontoTotal(int mmontoTotal) {
        MontoTotal = mmontoTotal;
    }
}
