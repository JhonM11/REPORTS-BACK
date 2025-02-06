package com.example.reports.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FileService fileService;

    @Autowired
    private EmailService emailService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Scheduled(fixedRate = 60000) // Cada minuto
    public void executeReports() {
        // Obtener la hora actual en formato HH:mm
        String horaActual = LocalTime.now().format(TIME_FORMATTER);

        // Obtener los reportes que deben ejecutarse en la hora actual y cuyo estado es 'A'
        List<Map<String, Object>> reports = jdbcTemplate.queryForList(
                "SELECT CODIGO, CONSULTA, NOMBRE_ARCHIVO, DESTINATARIOS FROM REPORTS " +
                        "WHERE ESTADO = 'A' AND  TO_CHAR(H_EJECUCION) = ?",
                horaActual
        );

        for (Map<String, Object> report : reports) {
            try {
                String codigo = (String) report.get("CODIGO");
                String consulta = (String) report.get("CONSULTA");
                String nombreArchivo = (String) report.get("NOMBRE_ARCHIVO");
                String destinatarios = (String) report.get("DESTINATARIOS");

                // Ejecutar la consulta y generar el archivo
                List<Map<String, Object>> result = jdbcTemplate.queryForList(consulta);
                String filePath = fileService.generarArchivo(nombreArchivo, result);
                String zipFilePath = fileService.comprimirArchivo(filePath);

                // Enviar por correo
                emailService.enviableCore(destinatarios, zipFilePath);

                // Registrar la ejecución en logs
                jdbcTemplate.update(
                        "INSERT INTO LOGS_EJECUCION (CODIGO, DESCRIPCION, HORA) VALUES (?, ?, SYSTIMESTAMP)",
                        codigo, "Ejecución exitosa"
                );
            } catch (Exception e) {
                // Si ocurre un error, registrar en los logs
                jdbcTemplate.update(
                        "INSERT INTO LOGS_EJECUCION (CODIGO, DESCRIPCION, HORA) VALUES (?, ?, SYSTIMESTAMP)",
                        report.get("CODIGO"), e.getMessage()
                );
            }
        }
    }
}
