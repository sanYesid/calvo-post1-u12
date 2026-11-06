package com.empresa.reembolsos.infraestructura;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para la persistencia de solicitudes de reembolso.
 */
public interface RepositorioSolicitudesJpa extends JpaRepository<SolicitudReembolso, Long> {

    List<SolicitudReembolso> findByEmpleadoIdAndEstado(String empleadoId, EstadoSolicitud estado);
}