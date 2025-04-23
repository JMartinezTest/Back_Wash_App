package com.proyecto.san_felipe.Services;

import com.proyecto.san_felipe.Repository.ClientRepository;
import com.proyecto.san_felipe.entities.Client;
// import com.proyecto.san_felipe.entities.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    public Client registerClient(Client client) {
        return clientRepository.save(client);
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }
}
