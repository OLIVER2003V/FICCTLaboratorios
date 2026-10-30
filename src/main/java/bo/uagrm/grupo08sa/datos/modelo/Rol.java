package bo.uagrm.grupo08sa.datos.modelo;

public class Rol {

    private int id;
    private String codigo;
    private String nombre;
    private String descripcion;

    public Rol(int id, String codigo, String nombre, String descripcion) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public int getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
}
