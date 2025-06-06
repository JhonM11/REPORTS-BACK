package com.example.reports.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

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

        logger.info("Ejecutando verificación de reportes a la hora: {}", horaActual);

        // Obtener los reportes que deben ejecutarse en la hora actual y cuyo estado es 'A'
        List<Map<String, Object>> reports = jdbcTemplate.queryForList(
                "SELECT CODIGO, CONSULTA, NOMBRE_ARCHIVO, DESTINATARIOS, ASUNTO, CUERPO, RUTA_GENERACION FROM REPORTS " +
                        "WHERE ESTADO = 'A' AND TO_CHAR(H_EJECUCION) = ?",
                horaActual
        );

        for (Map<String, Object> report : reports) {
            try {
                String codigo = (String) report.get("CODIGO");
                String consulta = (String) report.get("CONSULTA");
                String nombreArchivo = (String) report.get("NOMBRE_ARCHIVO");
                String destinatarios = (String) report.get("DESTINATARIOS");
                String asunto = (String) report.get("ASUNTO");
                String cuerpo = (String) report.get("CUERPO");
                String rutaGeneracion = (String) report.get("RUTA_GENERACION");

                logger.info("Procesando reporte: {}", codigo);


                // Ejecutar la consulta y generar el archivo
                logger.info("Ejecutando consulta en la BD");
                List<Map<String, Object>> result = jdbcTemplate.queryForList(consulta);


                logger.info("Inicia procedo de escritura");
                String filePath = fileService.generarArchivo(nombreArchivo, result, rutaGeneracion);
                logger.info("Archivo generado");



                logger.info("Inicia proceso de compresión del archivo");
                String zipFilePath = fileService.comprimirArchivo(filePath);
                logger.info("Archivo generado y comprimido en: {}", zipFilePath);



                // Enviar por correo'
                logger.info("Se procede a enviar el correo con la información");
                emailService.enviarCorreo(destinatarios, asunto, cuerpo, zipFilePath);
                logger.info("Correo enviado a: {}", destinatarios);


                // Registrar la ejecución en logs
                jdbcTemplate.update(
                        "INSERT INTO LOGS_EJECUCION (CODIGO, DESCRIPCION, HORA) VALUES (?, ?, SYSTIMESTAMP)",
                        codigo, "Ejecución exitosa"
                );

                logger.info("Ejecución exitosa");

            } catch (Exception e) {
                // Si ocurre un error, registrar en los logs
                jdbcTemplate.update(
                        "INSERT INTO LOGS_EJECUCION (CODIGO, DESCRIPCION, HORA) VALUES (?, ?, SYSTIMESTAMP)",
                        report.get("CODIGO"), e.getMessage()
                );

                logger.info("El proceso de generación ha fallado, validar la tabla LOGS_EJECUCION");
            }
        }
    }
}


