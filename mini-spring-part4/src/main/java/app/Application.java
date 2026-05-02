package app;

import minispring.container.MiniApplicationContext;
import minispring.web.MiniWebServer;

/**
 * Entry point for the Mini Spring Part 4 demo.
 *
 * Run:
 *   mvn compile exec:java -Dexec.mainClass="app.Application"
 *
 * Then test:
 *   curl http://localhost:8080/health
 *   curl http://localhost:8080/users/1
 *   curl http://localhost:8080/users/99          # 404
 *   curl "http://localhost:8080/users?limit=2"
 *   curl -X POST http://localhost:8080/users \
 *        -H "Content-Type: application/json" \
 *        -d '{"name":"Ahmed","email":"ahmed@example.com"}'
 *   curl http://localhost:8080/users/4            # just created
 */
public class Application {

    public static void main(String[] args) throws Exception {
        MiniApplicationContext ctx = new MiniApplicationContext("app");

        MiniWebServer server = new MiniWebServer(ctx);
        server.start(8080);

        System.out.println("\nPress Ctrl+C to stop the server\n");

        // Keep the main thread alive
        Thread.currentThread().join();
    }
}
