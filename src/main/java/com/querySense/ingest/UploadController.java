package com.querySense.ingest;

import com.querySense.schema.SchemaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UploadController {

    private final CsvIngestService ingestService;
    private final SchemaService schemaService;

    public UploadController(CsvIngestService ingestService, SchemaService schemaService) {
        this.ingestService = ingestService;
        this.schemaService = schemaService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "table", required = false) String table) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded."));
        }
        try {
            Map<String, Object> result = ingestService.ingest(file, table);
            schemaService.refresh();   // the AI now learns about the new table
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Could not ingest CSV: " + e.getMessage()));
        }
    }
}