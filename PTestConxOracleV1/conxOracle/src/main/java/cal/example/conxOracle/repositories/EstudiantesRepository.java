package cal.example.conxOracle.repositories;

import cal.example.conxOracle.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstudiantesRepository extends JpaRepository <Estudiante, Integer> {
}
