package com.proyecto.san_felipe.Controllers;

import com.proyecto.san_felipe.Services.CountService;
// import com.proyecto.san_felipe.Services.CountService;
import com.proyecto.san_felipe.entities.Count;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/count")
public class CountController {

    @Autowired
    private CountService countsService;

    @GetMapping
    public Count getCount() {
        return countsService.getCounts();
    }
}
