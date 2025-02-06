package com.example.reports.service;

import org.springframework.stereotype.Service;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileService {

    public String generarArchivo(String nombreArchivo, List<Map<String, Object>> data, String rutaGeneracion) throws IOException {
        String filePath = rutaGeneracion + "/" + nombreArchivo + ".csv";  // Usando la ruta proporcionada
        try (FileWriter writer = new FileWriter(filePath)) {
            for (Map<String, Object> row : data) {
                writer.write(row.toString() + "\n");
            }
        }
        return filePath;
    }

    public String comprimirArchivo(String filePath) throws IOException {
        String zipFilePath = filePath + ".zip";
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(Paths.get(zipFilePath)))) {
            zos.putNextEntry(new ZipEntry(Paths.get(filePath).getFileName().toString()));
            Files.copy(Paths.get(filePath), zos);
            zos.closeEntry();
        }
        return zipFilePath;
    }
}
