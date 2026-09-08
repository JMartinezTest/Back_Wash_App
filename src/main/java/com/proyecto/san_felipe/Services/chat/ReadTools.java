package com.proyecto.san_felipe.Services.chat;

import com.proyecto.san_felipe.Services.CountService;
import com.proyecto.san_felipe.Services.WashRecordService;
import com.proyecto.san_felipe.Repository.CarRepository;
import com.proyecto.san_felipe.Repository.ClientRepository;
import com.proyecto.san_felipe.Repository.EmployeeRepository;
import com.proyecto.san_felipe.Repository.ServiceOfferedRepository;
import com.proyecto.san_felipe.entities.Count;
import com.proyecto.san_felipe.entities.WashRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.proyecto.san_felipe.Services.chat.ToolSchema.obligatorio;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.opcional;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.params;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.sinParametros;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.texto;

/** Herramientas de consulta: el asistente lee datos reales en lugar de suponerlos. */
@Configuration
public class ReadTools {

    /** Un lavado antiguo puede no tener fecha; este es el limite inferior por defecto. */
    private static final LocalDate DESDE_SIEMPRE = LocalDate.of(2000, 1, 1);

    /**
     * Cuantas filas como mucho se le mandan al modelo en un listado.
     *
     * Lo que devuelve una herramienta se queda en la conversacion y vuelve a viajar en
     * todas las rondas siguientes, asi que un listado sin filtrar acaba costando mas
     * tokens que el resto de la consulta junta. Se recorta el detalle, nunca las cifras
     * totales, y se avisa al modelo de que la lista esta incompleta para que pida
     * concretar en vez de dar por buenos los que ve.
     */
    private static final int MAX_FILAS = 25;

    private static final String AVISO_RECORTE =
            "La lista esta recortada: hay mas resultados. Pide al usuario que concrete la "
            + "busqueda en vez de darla por completa.";

    /** Deja pasar los listados cortos tal cual y recorta los largos avisando de ello. */
    private static Object conTope(List<Map<String, Object>> filas) {
        if (filas.size() <= MAX_FILAS) {
            return filas;
        }
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("total_encontrados", filas.size());
        respuesta.put("mostrados", MAX_FILAS);
        respuesta.put("aviso", AVISO_RECORTE);
        respuesta.put("resultados", filas.subList(0, MAX_FILAS));
        return respuesta;
    }

    @Bean
    public ChatTool listarServicios(ServiceOfferedRepository repositorio, ChatDataAccess datos) {
        return new ChatTool(
                "listar_servicios",
                "Servicios del lavadero con precio y duracion. Usala para cualquier pregunta de precios.",
                sinParametros(),
                args -> repositorio.findAll().stream().map(datos::describirServicio).toList());
    }

    @Bean
    public ChatTool buscarClientes(ClientRepository repositorio, ChatDataAccess datos) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("busqueda", texto("Nombre, apellido o NIT. Omitelo para listar todos."));
        return new ChatTool(
                "buscar_clientes",
                "Busca clientes por nombre, apellido o NIT. Sin parametros los devuelve todos.",
                params(propiedades),
                args -> {
                    String busqueda = opcional(args, "busqueda");
                    if (busqueda == null) {
                        return conTope(repositorio.findAll().stream().map(datos::describirCliente).toList());
                    }
                    return List.of(datos.describirCliente(datos.resolverCliente(busqueda)));
                });
    }

    @Bean
    public ChatTool buscarVehiculos(CarRepository repositorio, ChatDataAccess datos) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("busqueda", texto("Placa o marca. Omitelo para listar todos."));
        return new ChatTool(
                "buscar_vehiculos",
                "Busca vehiculos por placa o marca. Sin parametros los devuelve todos.",
                params(propiedades),
                args -> {
                    String busqueda = opcional(args, "busqueda");
                    if (busqueda == null) {
                        return conTope(repositorio.findAll().stream().map(datos::describirVehiculo).toList());
                    }
                    return List.of(datos.describirVehiculo(datos.resolverVehiculo(busqueda)));
                });
    }

    @Bean
    public ChatTool listarEmpleados(EmployeeRepository repositorio, ChatDataAccess datos) {
        return new ChatTool(
                "listar_empleados",
                "Empleados del lavadero con cargo y telefono.",
                sinParametros(),
                args -> repositorio.findAll().stream().map(datos::describirEmpleado).toList());
    }

    @Bean
    public ChatTool consultarLavados(WashRecordService servicio, ChatDataAccess datos) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("placa", texto("Placa, opcional."));
        propiedades.put("empleado", texto("Empleado, opcional."));
        propiedades.put("desde", texto("AAAA-MM-DD, opcional."));
        propiedades.put("hasta", texto("AAAA-MM-DD, opcional."));
        return new ChatTool(
                "consultar_lavados",
                "Historial de lavados, con filtros opcionales de vehiculo, empleado y fechas.",
                params(propiedades),
                args -> {
                    String placa = opcional(args, "placa");
                    String empleado = opcional(args, "empleado");
                    Date desde = datos.fecha(opcional(args, "desde"), DESDE_SIEMPRE, false);
                    Date hasta = datos.fecha(opcional(args, "hasta"), LocalDate.now(), true);

                    List<WashRecord> lavados;
                    if (empleado != null) {
                        String idEmpleado = datos.resolverEmpleado(empleado).getId();
                        lavados = servicio.getWashRecordByEmployeeAndDate(idEmpleado, desde, hasta);
                    } else if (placa != null) {
                        String idVehiculo = datos.resolverVehiculo(placa).getId();
                        lavados = servicio.getWashRecordByCarAndTheRange(idVehiculo, desde, hasta);
                    } else {
                        lavados = servicio.getAllWashRecord().stream()
                                .filter(l -> l.getDate() != null
                                        && !l.getDate().before(desde) && !l.getDate().after(hasta))
                                .toList();
                    }
                    // Cuando se piden ambos filtros, el segundo se aplica sobre el resultado del primero.
                    if (empleado != null && placa != null) {
                        String idVehiculo = datos.resolverVehiculo(placa).getId();
                        lavados = lavados.stream().filter(l -> idVehiculo.equals(l.getCar())).toList();
                    }

                    double total = 0;
                    for (WashRecord lavado : lavados) {
                        total += lavado.getTotal();
                    }

                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    // La cantidad y el total se calculan sobre todos los lavados que encajan;
                    // solo se recorta el detalle, que es lo que ocupa.
                    respuesta.put("cantidad", lavados.size());
                    respuesta.put("total_facturado", total);
                    if (lavados.size() > MAX_FILAS) {
                        respuesta.put("mostrados", "los " + MAX_FILAS + " mas recientes");
                        respuesta.put("aviso", AVISO_RECORTE
                                + " La cantidad y el total si estan calculados sobre todos.");
                    }
                    respuesta.put("lavados", lavados.stream()
                            .sorted(Comparator.comparing(WashRecord::getDate,
                                    Comparator.nullsLast(Comparator.reverseOrder())))
                            .limit(MAX_FILAS)
                            .map(datos::describirLavado)
                            .toList());
                    return respuesta;
                });
    }

    @Bean
    public ChatTool calcularComision(WashRecordService servicio, ChatDataAccess datos) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("empleado", texto("Nombre del empleado."));
        propiedades.put("desde", texto("AAAA-MM-DD, opcional."));
        propiedades.put("hasta", texto("AAAA-MM-DD, opcional."));
        return new ChatTool(
                "calcular_comision_empleado",
                "Comision de un empleado (35%) en un rango de fechas. Usala para toda cifra de pago.",
                params(propiedades, "empleado"),
                args -> {
                    var empleado = datos.resolverEmpleado(obligatorio(args, "empleado"));
                    Date desde = datos.fecha(opcional(args, "desde"), DESDE_SIEMPRE, false);
                    Date hasta = datos.fecha(opcional(args, "hasta"), LocalDate.now(), true);
                    double comision = servicio.calculateEmployeePayment(empleado.getId(), desde, hasta);
                    int lavados = servicio.getWashRecordByEmployeeAndDate(empleado.getId(), desde, hasta).size();

                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    respuesta.put("empleado", datos.nombreCompleto(empleado.getName(), empleado.getLastName()));
                    respuesta.put("lavados_realizados", lavados);
                    respuesta.put("comision", comision);
                    respuesta.put("porcentaje_aplicado", "35%");
                    return respuesta;
                });
    }

    @Bean
    public ChatTool resumenDelNegocio(CountService servicio) {
        return new ChatTool(
                "resumen_del_negocio",
                "Cuantos clientes, vehiculos y empleados hay registrados.",
                sinParametros(),
                args -> {
                    Count conteo = servicio.getCounts();
                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    respuesta.put("clientes", conteo.getClientsCount());
                    respuesta.put("vehiculos", conteo.getCarsCount());
                    respuesta.put("empleados", conteo.getEmployeesCount());
                    return respuesta;
                });
    }
}
