package com.proyecto.san_felipe.Controllers;

import com.proyecto.san_felipe.Services.ClientService;
// import com.proyecto.san_felipe.entities.Car;
import com.proyecto.san_felipe.entities.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @GetMapping
    public List<Client> findAll() {
        return clientService.getAllClients();
    }
    // @PostMapping("/register")
    // public ResponseEntity<Client> registerClient(@RequestBody Client client) {
    // Client saveClient = clientService.registerClient(client);
    // return new ResponseEntity<>(saveClient, HttpStatus.CREATED);
    // }

    @PostMapping("/register")
    public ResponseEntity<Client> registerCar(@RequestBody Client client) {
        Client savedClient = clientService.registerClient(client);
        return new ResponseEntity<>(savedClient, HttpStatus.CREATED);
    }

}
