package com.proyecto.san_felipe.Services.chat;

import com.proyecto.san_felipe.Repository.CarRepository;
import com.proyecto.san_felipe.Repository.ClientRepository;
import com.proyecto.san_felipe.Repository.EmployeeRepository;
import com.proyecto.san_felipe.Repository.ServiceOfferedRepository;
import com.proyecto.san_felipe.entities.Car;
import com.proyecto.san_felipe.entities.Client;
import com.proyecto.san_felipe.entities.Employee;
import com.proyecto.san_felipe.entities.ServiceOffered;
import com.proyecto.san_felipe.entities.WashRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduce entre el lenguaje del usuario y la base de datos.
 *
 * El modelo habla de "Juan Perez", "placa ABC-123" o "Premium"; la coleccion WashRecord
 * guarda ids. Aqui se resuelven esos nombres a entidades y se arman las vistas legibles
 * que se devuelven al modelo, para que nunca tenga que manipular ids.
 */
@Component
public class ChatDataAccess {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ServiceOfferedRepository serviceOfferedRepository;

    // ---------- Resolucion de nombres ----------

    public Client resolverCliente(String busqueda) {
        List<Client> todos = clientRepository.findAll();
        List<Client> encontrados = filtrar(todos, busqueda,
                c -> List.of(nombreCompleto(c.getName(), c.getLastName()), texto(c.getNit()), texto(c.getId())));
        return unico(encontrados, "cliente", busqueda,
                c -> nombreCompleto(c.getName(), c.getLastName()) + " (NIT " + texto(c.getNit()) + ")");
    }

    public Car resolverVehiculo(String busqueda) {
        List<Car> todos = carRepository.findAll();
        List<Car> encontrados = filtrar(todos, busqueda,
                c -> List.of(texto(c.getLicencePlate()), texto(c.getMake()), texto(c.getId())));
        return unico(encontrados, "vehiculo", busqueda,
                c -> texto(c.getMake()) + " " + texto(c.getColor()) + " (placa " + texto(c.getLicencePlate()) + ")");
    }

    public Employee resolverEmpleado(String busqueda) {
        List<Employee> todos = employeeRepository.findAll();
        List<Employee> encontrados = filtrar(todos, busqueda,
                e -> List.of(nombreCompleto(e.getName(), e.getLastName()), texto(e.getId())));
        return unico(encontrados, "empleado", busqueda,
                e -> nombreCompleto(e.getName(), e.getLastName()));
    }

    public ServiceOffered resolverServicio(String busqueda) {
        List<ServiceOffered> todos = serviceOfferedRepository.findAll();
        List<ServiceOffered> encontrados = filtrar(todos, busqueda,
                s -> List.of(texto(s.getName()), texto(s.getId())));
        return unico(encontrados, "servicio", busqueda, ServiceOffered::getName);
    }

    /**
     * Busca coincidencia exacta primero y, si no hay, por palabras sueltas. Asi "Juan"
     * encuentra a "Juan Perez" pero un nombre exacto nunca queda tapado por uno parcial.
     *
     * Se compara palabra a palabra y no la cadena entera porque los nombres reales casi
     * nunca vienen como los escribe quien pregunta: "gregorio morales" tiene que encontrar
     * igual a "GREGORIO  MORALES" con un espacio de mas, a "MORALES GREGORIO" y a
     * "GREGORIO ANDRES MORALES". Comparando cadenas completas, cualquiera de las tres
     * daba por inexistente a un cliente que si estaba registrado.
     */
    private <T> List<T> filtrar(List<T> candidatos, String busqueda, java.util.function.Function<T, List<String>> campos) {
        String objetivo = normalizar(busqueda);
        List<String> palabras = List.of(objetivo.split(" "));
        List<T> exactos = new ArrayList<>();
        List<T> parciales = new ArrayList<>();
        for (T candidato : candidatos) {
            List<String> valores = campos.apply(candidato).stream()
                    .map(this::normalizar)
                    .filter(valor -> !valor.isEmpty())
                    .toList();
            if (valores.stream().anyMatch(objetivo::equals)) {
                exactos.add(candidato);
                continue;
            }
            String texto = String.join(" ", valores);
            if (palabras.stream().allMatch(texto::contains)
                    // Al reves: se escribe de mas ("el cliente juan perez") y aun asi debe valer.
                    || valores.stream().anyMatch(objetivo::contains)) {
                parciales.add(candidato);
            }
        }
        return exactos.isEmpty() ? parciales : exactos;
    }

    /**
     * Exige una sola coincidencia. Si no hay ninguna o hay varias, lanza un error redactado
     * para que el modelo pueda repreguntar al usuario en lugar de inventar un dato.
     */
    private <T> T unico(List<T> encontrados, String tipo, String busqueda, java.util.function.Function<T, String> etiqueta) {
        if (encontrados.isEmpty()) {
            // Se recuerda que hay otras colecciones: un mismo nombre puede ser cliente o
            // empleado, y sin esta pista el modelo se queda en la que probo primero.
            throw new IllegalArgumentException(
                    "No se encontro ningun " + tipo + " que coincida con '" + busqueda + "'. "
                    + "Si en realidad es un cliente, un empleado, un vehiculo o un servicio, "
                    + "vuelve a intentarlo con la herramienta de esa categoria.");
        }
        if (encontrados.size() > 1) {
            String opciones = encontrados.stream().map(etiqueta).reduce((a, b) -> a + "; " + b).orElse("");
            throw new IllegalArgumentException("Hay varios resultados de " + tipo + " para '" + busqueda
                    + "': " + opciones + ". Pide al usuario que precise cual.");
        }
        return encontrados.get(0);
    }

    // ---------- Vistas legibles ----------

    public Map<String, Object> describirCliente(Client cliente) {
        Map<String, Object> vista = new LinkedHashMap<>();
        vista.put("nombre", nombreCompleto(cliente.getName(), cliente.getLastName()));
        vista.put("nit", cliente.getNit());
        vista.put("telefono", cliente.getPhoneNumber());
        return vista;
    }

    /**
     * El cliente con sus vehiculos. Quien pregunta por un cliente para registrarle un
     * lavado necesita saber que coches tiene, y sin esto el asistente gastaba otra ronda
     * -de las cuatro que hay- en preguntar por una placa que ya estaba guardada.
     */
    public Map<String, Object> describirClienteConVehiculos(Client cliente) {
        Map<String, Object> vista = describirCliente(cliente);
        vista.put("vehiculos", carRepository.findByClientId(cliente.getId()).stream()
                .map(c -> texto(c.getLicencePlate()) + " (" + texto(c.getMake()) + ")")
                .toList());
        return vista;
    }

    public Map<String, Object> describirVehiculo(Car vehiculo) {
        Map<String, Object> vista = new LinkedHashMap<>();
        vista.put("placa", vehiculo.getLicencePlate());
        vista.put("marca", vehiculo.getMake());
        vista.put("color", vehiculo.getColor());
        vista.put("propietario", vehiculo.getClientId() == null ? "(sin asignar)"
                : clientRepository.findById(vehiculo.getClientId())
                        .map(c -> nombreCompleto(c.getName(), c.getLastName()))
                        .orElse("(cliente no encontrado)"));
        return vista;
    }

    public Map<String, Object> describirEmpleado(Employee empleado) {
        Map<String, Object> vista = new LinkedHashMap<>();
        vista.put("nombre", nombreCompleto(empleado.getName(), empleado.getLastName()));
        vista.put("cargo", empleado.getPosition());
        vista.put("telefono", empleado.getPhoneNumber());
        return vista;
    }

    public Map<String, Object> describirServicio(ServiceOffered servicio) {
        Map<String, Object> vista = new LinkedHashMap<>();
        vista.put("nombre", servicio.getName());
        vista.put("descripcion", servicio.getDescription());
        vista.put("precio", servicio.getPrice());
        vista.put("duracion", servicio.getDuration());
        return vista;
    }

    /** Convierte un lavado a nombres. Si un id ya no existe se marca en vez de fallar. */
    public Map<String, Object> describirLavado(WashRecord lavado) {
        Map<String, Object> vista = new LinkedHashMap<>();
        // Un lavado no tiene ningun dato que lo identifique de forma unica (el mismo
        // cliente puede lavar el mismo coche dos veces el mismo dia), asi que se
        // expone su referencia para poder editarlo o eliminarlo despues.
        vista.put("referencia", lavado.getId());
        vista.put("fecha", lavado.getDate());
        vista.put("cliente", clientRepository.findById(texto(lavado.getClient()))
                .map(c -> nombreCompleto(c.getName(), c.getLastName()))
                .orElse("(cliente no encontrado)"));
        vista.put("vehiculo", carRepository.findById(texto(lavado.getCar()))
                .map(c -> texto(c.getMake()) + " (placa " + texto(c.getLicencePlate()) + ")")
                .orElse("(vehiculo no encontrado)"));
        vista.put("empleado", employeeRepository.findById(texto(lavado.getEmployee()))
                .map(e -> nombreCompleto(e.getName(), e.getLastName()))
                .orElse("(empleado no encontrado)"));
        List<String> servicios = new ArrayList<>();
        if (lavado.getServiceOffered() != null) {
            for (String idServicio : lavado.getServiceOffered()) {
                servicios.add(serviceOfferedRepository.findById(idServicio)
                        .map(ServiceOffered::getName)
                        .orElse("(servicio no encontrado)"));
            }
        }
        vista.put("servicios", servicios);
        vista.put("total", lavado.getTotal());
        return vista;
    }

    // ---------- Utilidades ----------

    /** Convierte "2026-09-05" a Date. Si viene null usa el valor por defecto indicado. */
    public Date fecha(String iso, LocalDate porDefecto, boolean finDelDia) {
        LocalDate dia = porDefecto;
        if (iso != null) {
            try {
                dia = LocalDate.parse(iso.substring(0, Math.min(10, iso.length())));
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "La fecha '" + iso + "' no es valida. Usa el formato AAAA-MM-DD.");
            }
        }
        return Date.from((finDelDia ? dia.plusDays(1).atStartOfDay() : dia.atStartOfDay())
                .atZone(ZoneId.systemDefault()).toInstant());
    }

    public String nombreCompleto(String nombre, String apellido) {
        return (texto(nombre) + " " + texto(apellido)).trim();
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }

    /** Minusculas, sin acentos y sin espacios de sobra, para que "Perez" y "Pérez" sean lo mismo. */
    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
