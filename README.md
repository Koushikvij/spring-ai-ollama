Spring AI Ollama
=================

Project overview
----------------

This is a small Spring Boot example that demonstrates AI-related endpoints backed by a local Ollama instance. It covers chat with memory, movie recommendations via prompt templates, and text embeddings with cosine similarity. It is intended as a starting point for local development and learning how to wire AI model calls into a Spring Boot application.

What it does
-----------

- **Chat** — conversational endpoint with sliding-window memory (`GET /api/{message}`)
- **Movie recommendation** — structured prompt template that returns title, description, cast, runtime, director, and IMDB rating (`POST /api/recommend`)
- **Text embedding** — converts a float vector using `bge-large:latest` (`POST /api/embedding`)
- **Cosine similarity** — computes the semantic similarity score between two strings (`POST /api/similarity`)

Prerequisites
-------------

- Java 21 or newer installed and `JAVA_HOME` set to a matching JDK.
- Maven available via the included wrapper (`mvnw` on Unix, `mvnw.cmd` on Windows) or a system Maven installation.
- Ollama installed and running locally on port 11434 (see installation steps below).
- The following models pulled in Ollama:

  ```powershell
  ollama pull gpt-oss
  ollama pull bge-large:latest
  ```

Installing Ollama
-----------------

1. Download the installer for your OS from [https://ollama.com/download](https://ollama.com/download).
2. Run the installer and follow the on-screen steps. Ollama runs as a background service after installation.
3. Verify Ollama is running by opening a terminal and running:

   ```powershell
   ollama list
   ```

   This should return a list of installed models (empty at first).

4. Pull the models required by this project:

   ```powershell
   ollama pull gpt-oss
   ollama pull bge-large:latest
   ```

5. Confirm both models appear in `ollama list` before starting the application.

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

API endpoints
-------------

### Chat

```
GET /api/{message}
```

Sends a message to `gpt-oss`. Conversation history is retained in memory for the session (sliding window of recent messages).

### Movie recommendation

```
POST /api/recommend?year=1995&genre=action&language=English
```

Returns a structured movie recommendation for the given criteria using `gpt-oss`.

### Text embedding

```
POST /api/embedding?text=hello+world
```

Returns a float array representing the semantic embedding of the input text using `bge-large:latest`.

### Cosine similarity

```
POST /api/similarity?text1=dog&text2=cat
```

Returns a score between -1 and 1 indicating how semantically similar the two strings are. Values closer to 1 mean more similar.

Where to look in the code
-------------------------

- Main application: `src/main/java/com/koushik/springaicode/SpringAiCodeApplication.java`
- Controller: `src/main/java/com/koushik/springaicode/OllamaController.java`
- Embedding service: `src/main/java/com/koushik/springaicode/EmbeddingService.java`
- Configuration: `src/main/resources/application.properties`

Configuration
-------------

Key properties in `application.properties`:

| Property | Value | Description |
|---|---|---|
| `spring.ai.ollama.base-url` | `http://localhost:11434` | Ollama server URL |
| `spring.ai.ollama.chat.options.model` | `gpt-oss` | Chat model |
| `spring.ai.ollama.chat.options.temperature` | `0.7` | Creativity of chat responses (0.0 = deterministic, 1.0 = most random) |
| `spring.ai.ollama.embedding.options.model` | `bge-large:latest` | Embedding model |

Troubleshooting
---------------

- **Build fails with compilation errors:**
  - Check the Java version required by the project in `pom.xml` (Java 21) and ensure `JAVA_HOME` points to a compatible JDK.
  - Run the build with full output to see the root cause:

  ```powershell
  .\mvnw.cmd -e clean package
  ```

- **Embedding endpoint returns the same vector for all inputs:**
  - This is a known bug in Ollama 0.24.0 where the `/api/embed` endpoint ignores the input text. `EmbeddingService` works around this by calling the older `/api/embeddings` endpoint directly.
  - If you upgrade Ollama and the bug is fixed, you can revert `EmbeddingService` to use Spring AI's `EmbeddingModel` bean.

- **Tests fail or hang:**
  - Run the failing tests directly to see the stack traces.
  - Check for missing test resources or environment variables used by tests.

- **Application fails to start with port conflict:**
  - The default Spring Boot port is 8080. Either stop the service using that port or set a different port in `application.properties`:

  ```properties
  server.port=8081
  ```

- **Problems caused by OneDrive or long path names on Windows:**
  - If you see strange file locking or path length errors, try moving the project to a simple path without spaces, for example `C:\projects\spring-ai-ollama`.

- **Maven wrapper issues on Windows:**
  - Use `mvnw.cmd` on Windows. If permissions prevent execution, run the command from PowerShell or Git Bash and ensure the file is not blocked by Windows Defender or other tools.

- **Ollama connection errors:**
  - Verify Ollama is running: `ollama list` should return your installed models.
  - Confirm the base URL in `application.properties` matches your Ollama setup.
  - Check that `gpt-oss` and `bge-large:latest` are both listed in `ollama list`.

If you still cannot resolve an issue
----------------------------------

Collect the build output and the last stack trace, then open an issue or paste the logs when asking for help. Include the output of:

```powershell
java -version
.\mvnw.cmd -v
.\mvnw.cmd clean package
ollama list
```

License
-------

No license is included. Add one if you plan to publish or share this project broadly.
