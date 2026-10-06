package com.gestionbiblioteca.Evaluacion.config;

import com.gestionbiblioteca.Evaluacion.service.PrestamoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Marca periódicamente como ATRASADO los préstamos vencidos (además de la verificación bajo demanda). */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrestamoScheduler {

    private final PrestamoService prestamoService;

    @Scheduled(cron = "0 0 * * * *")
    public void marcarAtrasados() {
        int total = prestamoService.actualizarPrestamosAtrasados();
        if (total > 0) {
            log.info("{} préstamo(s) marcados como ATRASADO", total);
        }
    }
}
