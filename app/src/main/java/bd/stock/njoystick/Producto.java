package bd.stock.njoystick;

public class Producto {
    private String codigo;
    private String nombre;
    private String marca;
    private int cantidad;
    private String descripcion;
    private String categoria;
    private String urlImagen;
    private int precio;
    private boolean isAlternativo;
    public Producto() {
        // Constructor vacío requerido por Firebase
    }

    public Producto(String codigo, String nombre, String marca, int precio, boolean isAlternativo, int cantidad, String descripcion, String categoria, String urlImagen) {
        this.codigo = codigo;
        this.isAlternativo = isAlternativo;
        this.nombre = nombre;
        this.marca = marca;
        this.cantidad = cantidad;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.urlImagen = urlImagen;
        this.precio = precio;
    }

    public int getPrecio() {
        return precio;
    }

    public void setPrecio(int precio) {
        this.precio = precio;
    }

    public boolean isAlternativo() {
        return isAlternativo;
    }

    public void setAlternativo(boolean alternativo) {
        isAlternativo = alternativo;
    }

    public String getUrlImagen() {
        return urlImagen;
    }

    public void setUrlImagen(String urlImagen) {
        this.urlImagen = urlImagen;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}
