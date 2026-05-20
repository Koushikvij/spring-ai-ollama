Spring AI Ollama
=================

Project overview
----------------

This is a small Spring Boot example that demonstrates simple AI-related endpoints. The project includes controllers for experimenting with Ollama and OpenAI-style integrations. It is intended as a starting point for local development and learning how to wire AI model calls into a Spring Boot application.

What it does
-----------

- Exposes REST endpoints implemented in `src/main/java/com/koushik/springaicode`.
- Provides a runnable Spring Boot application that can be started from Maven or by running the packaged jar.

Prerequisites
-------------

- Java 17 or newer installed and `JAVA_HOME` set to a matching JDK.
- Maven available via the included wrapper (`mvnw` on Unix, `mvnw.cmd` on Windows) or a system Maven installation.

Build and run
-------------

Build the project:

```powershell
.\mvnw.cmd clean package
```

Run with the Maven Spring Boot plugin (development mode):

```powershell
.\mvnw.cmd spring-boot:run
```

Or run the packaged jar after building:

```powershell
java -jar target/*.jar
```

Run tests:

```powershell
.\mvnw.cmd test
```

Where to look in the code
-------------------------

- Main application: `src/main/java/com/koushik/springaicode/SpringAiCodeApplication.java`
- Controllers: `src/main/java/com/koushik/springaicode/OllamaController.java` and `src/main/java/com/koushik/springaicode/OpenAIController.java`
- Configuration: `src/main/resources/application.properties`

Troubleshooting
---------------

- Build fails with compilation errors:
  - Check the Java version required by the project in `pom.xml` and ensure `JAVA_HOME` points to a compatible JDK.
  - Run the build with full output to see the root cause:

  ```powershell
  .\mvnw.cmd -e clean package
  ```

- Tests fail or hang:
  - Run the failing tests directly to see the stack traces.
  - Check for missing test resources or environment variables used by tests.

- Application fails to start with port conflict:
  - The default Spring Boot port is 8080. Either stop the service using that port or set a different port in `application.properties`:

  ```properties
  server.port=8081
  ```

- Problems caused by OneDrive or long path names on Windows:
  - If you see strange file locking or path length errors, try moving the project to a simple path without spaces, for example `C:\projects\spring-ai-ollama`.

- Maven wrapper issues on Windows:
  - Use `mvnw.cmd` on Windows. If permissions prevent execution, run the command from PowerShell or Git Bash and ensure the file is not blocked by Windows Defender or other tools.

- Network or API calls fail (if the controllers call external services):
  - Verify network connectivity and any required API keys or endpoints configured in `application.properties`.
  - Check timeouts and proxy settings if you are on a corporate network.

If you still cannot resolve an issue
----------------------------------

Collect the build output and the last stack trace, then open an issue or paste the logs when asking for help. Include the output of:

```powershell
java -version
.\mvnw.cmd -v
.\mvnw.cmd clean package
```

Contributing and next steps
---------------------------

If you want help adding features, running the app locally, or wiring a real AI provider, ask and I can add example configuration and sample requests.

License
-------

No license is included. Add one if you plan to publish or share this project broadly.
