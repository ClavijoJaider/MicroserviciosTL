package cal.example.conxOracle.services;

import cal.example.conxOracle.model.Estudiante;
import cal.example.conxOracle.repositories.EstudiantesRepository;
import jakarta.websocket.server.ServerEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
@Service
@RequiredArgsConstructor
public class ServicioEstudiante {

    @Autowired
    private EstudiantesRepository estudiantesRepository;

    public Estudiante addEstudiante(Estudiante estudiante){

        Estudiante responseEst = estudiantesRepository.save(estudiante);

        return responseEst;
    }

    public List getEstudiantes(){

        System.out.println("CAL estudiantesRepository: " + estudiantesRepository);
        List<Estudiante> estudiantes = estudiantesRepository.findAll();

        return estudiantes;
    }




}
