package cal.example.conxOracle.controller;


import cal.example.conxOracle.model.Estudiante;
import cal.example.conxOracle.services.ServicioEstudiante;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/estudiantes")
public class EstudianteController {

    @Autowired
    private ServicioEstudiante servicioEstudiante;

    @GetMapping(value = "/healthCheck")
    public ResponseEntity<String> healthCheck(){
        return ResponseEntity.ok("Ok!");
    }

    @PostMapping(value = "/")
    public ResponseEntity<Estudiante> addEstudiante (@RequestBody Estudiante estudiante){
        Estudiante est;

        est = servicioEstudiante.addEstudiante(estudiante);

        return ResponseEntity.ok(est);
    }

    @GetMapping(value = "/all")
    public ResponseEntity<List <Estudiante>> getEstudiante(){
        List estudiantes = servicioEstudiante.getEstudiantes();

        if (estudiantes == null  || estudiantes.size() == 0)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(estudiantes);
    }

}
