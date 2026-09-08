package com.proyecto.san_felipe.Services.chat;

import com.proyecto.san_felipe.Services.CarService;
import com.proyecto.san_felipe.Services.ClientService;
import com.proyecto.san_felipe.Services.EmployeeService;
import com.proyecto.san_felipe.Services.ServiceOfferedService;
import com.proyecto.san_felipe.Services.WashRecordService;
import com.proyecto.san_felipe.entities.Car;
import com.proyecto.san_felipe.entities.Client;
import com.proyecto.san_felipe.entities.Employee;
import com.proyecto.san_felipe.entities.ServiceOffered;
import com.proyecto.san_felipe.entities.WashRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.proyecto.san_felipe.Services.chat.ToolSchema.listaDeTextos;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.listaObligatoria;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.numero;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.numeroOpcional;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.oMantener;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.obligatorio;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.opcional;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.params;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.texto;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.unaDe;

/**
 * Herramientas que corrigen o eliminan registros que ya existen.
 *
 * En una edicion, lo que el modelo no manda se deja como estaba: pedir "cambiale el
 * telefono a Juan" no puede borrarle el NIT. Como en WriteTools, todas devuelven el
 * dato tal y como quedo -o el que se elimino- para que el asistente confirme al
 * usuario lo que de verdad paso en la base de datos.
 *
 * Las definiciones de todas las herramientas viajan en cada ronda, asi que aqui se
 * escribe corto: un borrado unico con el tipo como parametro cuesta bastantes menos
 * tokens que cinco herramientas casi identicas, y la regla de que lo omitido se
 * conserva se dice una vez por herramienta en vez de en cada campo.
 */
@Configuration
public class EditTools {

    private static final String SOLO_LO_QUE_CAMBIA =
            " Manda solo los campos que cambian; lo que omitas se conserva.";

    // ---------- Ediciones ----------

    @Bean
    public ChatTool actualizarCliente(ClientService servicio, ChatDataAccess datos) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("cliente", texto("Nombre o NIT actual."));
        propiedades.put("nombre", texto("Nuevo nombre."));
        propiedades.put("apellido", texto("Nuevo apellido."));
        propiedades.put("nit", texto("Nuevo NIT."));
        propiedades.put("telefono", texto("Nuevo telefono."));
        return new ChatTool(
                "actualizar_cliente",
                "Corrige un cliente, de los que pagan los lavados." + SOLO_LO_QUE_CAMBIA,
                params(propiedades, "cliente"),
                args -> {
                    Client cliente = datos.resolverCliente(obligatorio(args, "cliente"));
                    cliente.setName(oMantener(opcional(args, "nombre"), cliente.getName()));
                    cliente.setLastName(oMantener(opcional(args, "apellido"), cliente.getLastName()));
                    cliente.setNit(oMantener(opcional(args, "nit"), cliente.getNit()));
                    cliente.setPhoneNumber(oMantener(opcional(args, "telefono"), cliente.getPhoneNumber()));

                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    respuesta.put("actualizado",
                            datos.describirCliente(servicio.updateClient(cliente.getId(), cliente)));
                    return respuesta;
                });
    }

    @Bean
    public ChatTool actualizarVehiculo(CarService servicio, ChatDataAccess datos) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("vehiculo", texto("Placa actual."));
        propiedades.put("nueva_placa", texto("Nueva placa."));
        propiedades.put("marca", texto("Nueva marca."));
        propiedades.put("color", texto("Nuevo color."));
        propiedades.put("cliente", texto("Nuevo propietario, por nombre o NIT."));
        return new ChatTool(
                "actualizar_vehiculo",
                "Corrige un vehiculo." + SOLO_LO_QUE_CAMBIA,
                params(propiedades, "vehiculo"),
                args -> {
                    Car vehiculo = datos.resolverVehiculo(obligatorio(args, "vehiculo"));
                    vehiculo.setLicencePlate(
                            oMantener(opcional(args, "nueva_placa"), vehiculo.getLicencePlate()));
                    vehiculo.setMake(oMantener(opcional(args, "marca"), vehiculo.getMake()));
                    vehiculo.setColor(oMantener(opcional(args, "color"), vehiculo.getColor()));

                    String propietario = opcional(args, "cliente");
                    if (propietario != null) {
                        vehiculo.setClientId(datos.resolverCliente(propietario).getId());
                    }

                    // Se identifica por id: la placa puede ser justo lo que se esta cambiando.
                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    respuesta.put("actualizado",
                            datos.describirVehiculo(servicio.updateCar(vehiculo.getId(), vehiculo)));
                    return respuesta;
                });
    }

    @Bean
    public ChatTool actualizarEmpleado(EmployeeService servicio, ChatDataAccess datos) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("empleado", texto("Nombre actual."));
        propiedades.put("nombre", texto("Nuevo nombre."));
        propiedades.put("apellido", texto("Nuevo apellido."));
        propiedades.put("cargo", texto("Nuevo cargo."));
        propiedades.put("telefono", texto("Nuevo telefono."));
        return new ChatTool(
                "actualizar_empleado",
                "Corrige un empleado, del personal del lavadero." + SOLO_LO_QUE_CAMBIA,
                params(propiedades, "empleado"),
                args -> {
                    Employee empleado = datos.resolverEmpleado(obligatorio(args, "empleado"));
                    empleado.setName(oMantener(opcional(args, "nombre"), empleado.getName()));
                    empleado.setLastName(oMantener(opcional(args, "apellido"), empleado.getLastName()));
                    empleado.setPosition(oMantener(opcional(args, "cargo"), empleado.getPosition()));
                    empleado.setPhoneNumber(oMantener(opcional(args, "telefono"), empleado.getPhoneNumber()));

                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    respuesta.put("actualizado",
                            datos.describirEmpleado(servicio.updateEmployee(empleado.getId(), empleado)));
                    return respuesta;
                });
    }

    @Bean
    public ChatTool actualizarServicio(ServiceOfferedService servicio, ChatDataAccess datos) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("servicio", texto("Nombre actual."));
        propiedades.put("nombre", texto("Nuevo nombre."));
        propiedades.put("descripcion", texto("Nueva descripcion."));
        propiedades.put("precio", numero("Nuevo precio en pesos."));
        propiedades.put("duracion", texto("Nueva duracion, ej. 45 min."));
        return new ChatTool(
                "actualizar_servicio",
                "Corrige un servicio del catalogo, por ejemplo su precio." + SOLO_LO_QUE_CAMBIA,
                params(propiedades, "servicio"),
                args -> {
                    ServiceOffered ofrecido = datos.resolverServicio(obligatorio(args, "servicio"));
                    ofrecido.setName(oMantener(opcional(args, "nombre"), ofrecido.getName()));
                    ofrecido.setDescription(oMantener(opcional(args, "descripcion"), ofrecido.getDescription()));
                    ofrecido.setDuration(oMantener(opcional(args, "duracion"), ofrecido.getDuration()));

                    Double precio = numeroOpcional(args, "precio");
                    if (precio != null) {
                        ofrecido.setPrice(precio);
                    }

                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    respuesta.put("actualizado",
                            datos.describirServicio(servicio.updateServiceOffered(ofrecido.getId(), ofrecido)));
                    return respuesta;
                });
    }

    @Bean
    public ChatTool actualizarLavado(WashRecordService servicio, ChatDataAccess datos) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("referencia", texto("Referencia que devuelve consultar_lavados."));
        propiedades.put("cliente", texto("Nuevo cliente."));
        propiedades.put("placa", texto("Nuevo vehiculo."));
        propiedades.put("empleado", texto("Nuevo empleado."));
        propiedades.put("servicios", listaDeTextos("Servicios que sustituyen a los actuales; el total se recalcula."));
        return new ChatTool(
                "actualizar_lavado",
                "Corrige un lavado del historial." + SOLO_LO_QUE_CAMBIA,
                params(propiedades, "referencia"),
                args -> {
                    WashRecord lavado = servicio.getWashRecordById(obligatorio(args, "referencia"));

                    String cliente = opcional(args, "cliente");
                    if (cliente != null) {
                        lavado.setClient(datos.resolverCliente(cliente).getId());
                    }
                    String placa = opcional(args, "placa");
                    if (placa != null) {
                        lavado.setCar(datos.resolverVehiculo(placa).getId());
                    }
                    String empleado = opcional(args, "empleado");
                    if (empleado != null) {
                        lavado.setEmployee(datos.resolverEmpleado(empleado).getId());
                    }
                    if (args.get("servicios") != null) {
                        List<String> idsServicios = new ArrayList<>();
                        double total = 0;
                        for (String nombreServicio : listaObligatoria(args, "servicios")) {
                            ServiceOffered encontrado = datos.resolverServicio(nombreServicio);
                            idsServicios.add(encontrado.getId());
                            total += encontrado.getPrice();
                        }
                        lavado.setServiceOffered(idsServicios);
                        lavado.setTotal(total);
                    }

                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    respuesta.put("actualizado",
                            datos.describirLavado(servicio.updateWashRecord(lavado.getId(), lavado)));
                    return respuesta;
                });
    }

    // ---------- Borrado ----------

    /**
     * Un unico borrado para las cinco colecciones. El tipo va como parametro cerrado:
     * el modelo elige de una lista en vez de acertar con el nombre de la herramienta,
     * y las definiciones que viajan en cada ronda ocupan la quinta parte.
     */
    @Bean
    public ChatTool eliminarRegistro(ClientService clientes, CarService vehiculos,
                                     EmployeeService empleados, ServiceOfferedService servicios,
                                     WashRecordService lavados, ChatDataAccess datos) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("tipo", unaDe("Que se elimina.",
                "cliente", "vehiculo", "empleado", "servicio", "lavado"));
        propiedades.put("identificador", texto(
                "Nombre o NIT del cliente, placa del vehiculo, nombre del empleado o del "
                        + "servicio, o referencia del lavado."));
        return new ChatTool(
                "eliminar_registro",
                "Elimina un registro. Confirmalo con el usuario antes de llamarla: no se puede deshacer.",
                params(propiedades, "tipo", "identificador"),
                args -> {
                    String tipo = obligatorio(args, "tipo").toLowerCase();
                    String identificador = obligatorio(args, "identificador");

                    // Se describe antes de borrar: despues ya no se puede confirmar que se fue.
                    Map<String, Object> eliminado;
                    switch (tipo) {
                        case "cliente" -> {
                            Client cliente = datos.resolverCliente(identificador);
                            eliminado = datos.describirCliente(cliente);
                            clientes.deleteClientById(cliente.getId());
                        }
                        case "vehiculo" -> {
                            Car vehiculo = datos.resolverVehiculo(identificador);
                            eliminado = datos.describirVehiculo(vehiculo);
                            vehiculos.deleteCarByLicencePlate(vehiculo.getId());
                        }
                        case "empleado" -> {
                            Employee empleado = datos.resolverEmpleado(identificador);
                            eliminado = datos.describirEmpleado(empleado);
                            empleados.deleteEmployeeById(empleado.getId());
                        }
                        case "servicio" -> {
                            ServiceOffered ofrecido = datos.resolverServicio(identificador);
                            eliminado = datos.describirServicio(ofrecido);
                            servicios.deleteServiceOfferedById(ofrecido.getId());
                        }
                        case "lavado" -> {
                            WashRecord lavado = lavados.getWashRecordById(identificador);
                            eliminado = datos.describirLavado(lavado);
                            lavados.deleteWashRecordById(lavado.getId());
                        }
                        default -> throw new IllegalArgumentException("El tipo '" + tipo
                                + "' no existe. Usa cliente, vehiculo, empleado, servicio o lavado.");
                    }

                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    respuesta.put("eliminado", eliminado);
                    return respuesta;
                });
    }
}
