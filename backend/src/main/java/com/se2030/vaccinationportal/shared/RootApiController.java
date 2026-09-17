package com.se2030.vaccinationportal.shared;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Root controller providing a friendly status landing for the backend REST API server.
 */
@RestController
public class RootApiController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String rootHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Vaccination Portal - Backend API</title>
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f8fafc; color: #0f172a; margin: 0; display: flex; align-items: center; justify-content: center; min-height: 100vh; }
                .card { background: white; padding: 2.5rem; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -2px rgba(0,0,0,0.1); max-width: 520px; text-align: center; border: 1px solid #e2e8f0; }
                .badge { display: inline-block; background: #dcfce7; color: #166534; font-weight: 600; padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.875rem; margin-bottom: 1rem; }
                h1 { margin: 0 0 0.75rem 0; font-size: 1.5rem; color: #0f172a; }
                p { color: #64748b; line-height: 1.5; margin-bottom: 1.5rem; font-size: 0.95rem; }
                .btn { display: inline-block; background: #0e7490; color: white; text-decoration: none; padding: 0.75rem 1.5rem; border-radius: 8px; font-weight: 600; font-size: 0.95rem; transition: background 0.2s; }
                .btn:hover { background: #155e75; }
                .links { margin-top: 1.5rem; font-size: 0.85rem; color: #94a3b8; }
                .links a { color: #0e7490; text-decoration: none; }
                .links a:hover { text-decoration: underline; }
              </style>
            </head>
            <body>
              <div class="card">
                <span class="badge">&#10003; Backend API Running</span>
                <h1>Vaccination Portal API</h1>
                <p>The backend service is healthy and operational. If you are looking for the web portal interface, please access the frontend application.</p>
                <a href="http://localhost:5173" class="btn">Open Frontend Portal</a>
                <div class="links">
                  Health check: <a href="/actuator/health">/actuator/health</a>
                </div>
              </div>
            </body>
            </html>
            """;
    }

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> rootJson() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Vaccination Portal Backend API",
                "frontendUrl", "http://localhost:5173",
                "healthUrl", "/actuator/health"
        ));
    }
}
