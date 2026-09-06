package com.proyecto.san_felipe.Services.chat;

import com.proyecto.san_felipe.Services.CarService;
import com.proyecto.san_felipe.Services.ClientService;
import com.proyecto.san_felipe.Services.EmployeeService;
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
import static com.proyecto.san_felipe.Services.chat.ToolSchema.obligatorio;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.opcional;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.params;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.texto;

/**
 * Herramientas que escriben en la base de datos.
 *
 * Todas devuelven lo que quedo guardado para que el asistente confirme al usuario
 * el dato real y no una suposicion.
 */
@Configuration
public class WriteTools {

    @Bean
    public ChatTool registrarCliente(ClientService servicio, ChatDataAccess datos) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("nombre", texto("Nombre."));
        propiedades.put("apellido", texto("Apellido."));
        propiedades.put("nit", texto("NIT o documento."));
        propiedades.put("telefono", texto("Telefono."));
        return new ChatTool(
                "registrar_cliente",
                "Registra un cliente nuevo. Pide los datos que falten antes de llamarla.",
                params(propiedades, "nombre", "apellido"),
                args -> {
                    Client cliente = new Client();
                    cliente.setName(obligatorio(args, "nombre"));
                    cliente.setLastName(obligatorio(args, "apellido"));
                    cliente.setNit(opcional(args, "nit"));
                    cliente.setPhoneNumber(opcional(args, "telefono"));

                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    respuesta.put("registrado", datos.describirCliente(servicio.registerClient(cliente)));
                    return respuesta;
                });
    }

    @Bean
    public ChatTool registrarVehiculo(CarService servicio, ChatDataAccess datos) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("placa", texto("Placa."));
        propiedades.put("marca", texto("Marca."));
        propiedades.put("color", texto("Color."));
        propiedades.put("cliente", texto("Nombre o NIT del cliente propietario."));
        return new ChatTool(
                "registrar_vehiculo",
                "Registra un vehiculo nuevo. Todo vehiculo pertenece a un cliente.",
                params(propiedades, "placa", "marca", "cliente"),
                args -> {
                    Car vehiculo = new Car();
                    vehiculo.setLicencePlate(obligatorio(args, "placa"));
                    vehiculo.setMake(obligatorio(args, "marca"));
                    vehiculo.setColor(opcional(args, "color"));
                    vehiculo.setClientId(datos.resolverCliente(obligatorio(args, "cliente")).getId());

                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    respuesta.put("registrado", datos.describirVehiculo(servicio.registerCar(vehiculo)));
                    return respuesta;
                });
    }

    @Bean
    public ChatTool registrarEmpleado(EmployeeService servicio, ChatDataAccess datos) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("nombre", texto("Nombre."));
        propiedades.put("apellido", texto("Apellido."));
        propiedades.put("cargo", texto("Cargo."));
        propiedades.put("telefono", texto("Telefono."));
        return new ChatTool(
                "registrar_empleado",
                "Registra un empleado nuevo.",
                params(propiedades, "nombre", "apellido"),
                args -> {
                    Employee empleado = new Employee();
                    empleado.setName(obligatorio(args, "nombre"));
                    empleado.setLastName(obligatorio(args, "apellido"));
                    empleado.setPosition(opcional(args, "cargo"));
                    empleado.setPhoneNumber(opcional(args, "telefono"));

                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    respuesta.put("registrado", datos.describirEmpleado(servicio.registerEmployee(empleado)));
                    return respuesta;
                });
    }

    @Bean
    public ChatTool registrarLavado(WashRecordService servicio, ChatDataAccess datos) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("cliente", texto("Nombre o NIT del cliente."));
        propiedades.put("placa", texto("Placa del vehiculo."));
        propiedades.put("empleado", texto("Empleado que lo realizo."));
        propiedades.put("servicios", listaDeTextos("Servicios aplicados, ej. [\"Premium\"]."));
        return new ChatTool(
                "registrar_lavado",
                "Registra un lavado realizado hoy. El total se calcula solo, no lo indiques.",
                params(propiedades, "cliente", "placa", "empleado", "servicios"),
                args -> {
                    Client cliente = datos.resolverCliente(obligatorio(args, "cliente"));
                    Car vehiculo = datos.resolverVehiculo(obligatorio(args, "placa"));
                    Employee empleado = datos.resolverEmpleado(obligatorio(args, "empleado"));

                    List<String> idsServicios = new ArrayList<>();
                    double total = 0;
                    for (String nombreServicio : listaObligatoria(args, "servicios")) {
                        ServiceOffered encontrado = datos.resolverServicio(nombreServicio);
                        idsServicios.add(encontrado.getId());
                        total += encontrado.getPrice();
                    }

                    WashRecord lavado = new WashRecord();
                    lavado.setClient(cliente.getId());
                    lavado.setCar(vehiculo.getId());
                    lavado.setEmployee(empleado.getId());
                    lavado.setServiceOffered(idsServicios);
                    lavado.setTotal(total);

                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    respuesta.put("registrado", datos.describirLavado(servicio.registerWashRecord(lavado)));
                    return respuesta;
                });
    }
}
