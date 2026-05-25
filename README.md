Spring AI Ollama
=================

Project overview
----------------

A Spring Boot application that demonstrates AI-powered endpoints backed by a local Ollama instance and a PostgreSQL pgvector database. Features include conversational chat with memory, movie recommendations via prompt templates, text embeddings, cosine similarity, semantic product search, and a RAG (Retrieval-Augmented Generation) product assistant that handles multi-turn follow-up questions.

What it does
------------

| Endpoint | Method | Description |
|---|---|---|
| `/api/{message}` | GET | Conversational chat with sliding-window memory |
| `/api/recommend` | POST | Structured movie recommendation via prompt template |
| `/api/embedding` | POST | Returns a float vector for input text using `bge-large` |
| `/api/similarity` | POST | Cosine similarity score between two strings |
| `/api/product` | POST | Semantic product search — top 5 matches above 60% similarity |
| `/api/ask` | POST | RAG product assistant with multi-turn follow-up support |

### RAG follow-up behaviour

The `/api/ask` endpoint supports natural follow-up questions within a conversation:

1. **Fresh question** — vector store is searched; matching product documents are retrieved and cached.
2. **Follow-up question** (e.g. *"what is the price of it?"*) — if the vector search returns no results, the previously cached raw product context is reused automatically. This guarantees that field values like `Price` come from the source data, not from what the LLM may have paraphrased.

Prerequisites
-------------

- Java 21 or newer with `JAVA_HOME` pointing to a matching JDK.
- Maven via the included wrapper (`mvnw.cmd` on Windows, `mvnw` on Unix) or a system Maven installation.
- Docker Desktop running (used by `spring-boot-docker-compose` to start PostgreSQL).
- Ollama installed and running locally on port 11434.
- The following models pulled in Ollama:

  ```powershell
  ollama pull mistral:latest
  ollama pull bge-large
  ```

Installing Ollama
-----------------

1. Download the installer for your OS from [https://ollama.com/download](https://ollama.com/download).
2. Run the installer and follow the on-screen steps. Ollama runs as a background service after installation.
3. Verify Ollama is running:

   ```powershell
   ollama list
   ```

4. Pull the models required by this project:

   ```powershell
   ollama pull mistral:latest
   ollama pull bge-large
   ```

5. Confirm both models appear in `ollama list` before starting the application.

Build and run
-------------

Start Docker (PostgreSQL is managed by Spring Boot's Docker Compose support):

```powershell
docker-compose up -d
```

Run with the Maven Spring Boot plugin:

```powershell
.\mvnw.cmd spring-boot:run
```

Or build and run the packaged jar:

```powershell
.\mvnw.cmd clean package
java -jar target/*.jar
```

Run tests (no Ollama or PostgreSQL required — all external dependencies are mocked):

```powershell
.\mvnw.cmd test
```

Stop the application with `Ctrl+C`, then bring down Docker:

```powershell
docker-compose down
# to also remove volumes:
docker-compose down -v
```

Resetting the product catalogue
--------------------------------

`DataInitializer` checks the `vector_store` table on startup and skips insertion if rows already exist. To reload the catalogue (e.g. after editing `product_details.txt`), delete the existing rows and restart:

```sql
DELETE FROM vector_store;
```

Run this via pgAdmin, IntelliJ's Database tool, or psql:

```powershell
psql -U postgres -d koushik -c "DELETE FROM vector_store;"
```

On the next startup you will see in the logs:

```
Inserted 200 products into the vector store.
```

API endpoints
-------------

### Chat

```
GET /api/{message}
```

Sends a free-text message to `mistral:latest`. Conversation history is retained in a sliding-window in-process memory for the duration of the server session.

**Example:**
```
GET /api/What is the capital of France?
```

---

### Movie recommendation

```
POST /api/recommend?year=1995&genre=action&language=English
```

Returns a structured movie recommendation for the given criteria using a prompt template.

**Response format:**
```
Movie Title: ...
Description: ...
Cast: ...
Length: ...
Director: ...
IMDB Rating: ...
```

---

### Text embedding

```
POST /api/embedding?text=hello+world
```

Returns a JSON float array representing the semantic embedding of the input text using `bge-large`.

---

### Cosine similarity

```
POST /api/similarity?text1=dog&text2=cat
```

Returns a score between `-1` and `1` indicating semantic similarity. Values closer to `1` mean more similar.

---

### Product search

```
POST /api/product?text=laptop+stand
```

Searches the pgvector store and returns up to 5 products whose embedding similarity to the query is at least 60%.

---

### RAG product assistant

```
POST /api/ask?query=tell+me+about+the+insect+repellent+wristband
```

Retrieves relevant product documents from the vector store, injects them as context, and answers using `mistral:latest`. Supports follow-up questions in the same session:

```
POST /api/ask?query=what+is+the+price+of+it
```

The second query finds no new documents; the controller automatically reuses the product context from the previous turn so the price is answered correctly.

---

Where to look in the code
--------------------------

| File | Purpose |
|---|---|
| `src/main/java/.../SpringAiCodeApplication.java` | Application entry point |
| `src/main/java/.../controller/OllamaController.java` | All REST endpoints, RAG logic, context caching |
| `src/main/java/.../service/EmbeddingService.java` | Calls Ollama `/api/embeddings` directly via RestTemplate |
| `src/main/java/.../config/AppConfig.java` | Declares the `PgVectorStore` bean (1024 dimensions) |
| `src/main/java/.../config/DataInitializer.java` | Parses `product_details.txt` and seeds the vector store once on startup |
| `src/main/java/.../config/OllamaEmbeddingConfig.java` | Reserved — currently unused |
| `src/main/java/.../entity/Product.java` | Simple product data model |
| `src/main/java/.../helper/Helper.java` | Regex extraction utility used by `DataInitializer` |
| `src/main/resources/application.properties` | All configuration |
| `src/main/resources/init/schema.sql` | Creates the `vector_store` table and HNSW index |
| `src/main/resources/product_details.txt` | 200 sample products seeded into the vector store |

Tests
-----

| Test class | What it covers |
|---|---|
| `SpringAiCodeApplicationTests` | Spring context starts successfully with all dependencies mocked |
| `HelperTest` | `Helper.extract()` regex parsing — all field patterns and edge cases |
| `DataInitializerTest` | `parseProducts()` correctness; `initData()` idempotency (skips when data exists, inserts when empty) |
| `OllamaControllerTest` | All six endpoints via MockMvc; RAG context caching across two-turn conversations |

All tests run without Ollama or PostgreSQL. External dependencies are replaced with Mockito `@MockBean` instances.

Configuration
-------------

Key properties in `application.properties`:

| Property | Value | Description |
|---|---|---|
| `spring.application.name` | `SpringAICode` | Application name |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | Ollama server URL |
| `spring.ai.ollama.chat.options.model` | `mistral:latest` | Chat model |
| `spring.ai.ollama.chat.options.temperature` | `0.7` | Response creativity (0 = deterministic, 1 = creative) |
| `spring.ai.model.embedding` | `ollama` | Embedding provider |
| `spring.ai.ollama.embedding.options.model` | `bge-large` | Embedding model (1024-dimension vectors) |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/koushik` | PostgreSQL connection |
| `spring.datasource.username` | `postgres` | Database username |
| `spring.jpa.hibernate.ddl-auto` | `none` | Schema managed by `schema.sql`, not Hibernate |
| `spring.sql.init.mode` | `always` | Runs `schema.sql` at every startup (`CREATE ... IF NOT EXISTS` makes it safe) |
| `spring.main.allow-bean-definition-overriding` | `true` | Allows the explicit `PgVectorStore` bean in `AppConfig` to override auto-configuration |

Troubleshooting
---------------

**Build fails with compilation errors**

Check the Java version in `pom.xml` (Java 21) and verify `JAVA_HOME`:

```powershell
java -version
.\mvnw.cmd -e clean package
```

**Application fails to connect to Ollama**

```powershell
ollama list
```

Confirm `mistral:latest` and `bge-large` are listed. If not, pull them:

```powershell
ollama pull mistral:latest
ollama pull bge-large
```

**DataInitializer inserts duplicates on every restart**

This was fixed. The initializer now queries `SELECT COUNT(*) FROM vector_store` before inserting. If you see repeated insertions, your database connection may be pointing to a different schema. Check `spring.datasource.url` in `application.properties`.

To force a fresh load: `DELETE FROM vector_store;` then restart.

**`/api/ask` says it cannot find a product that exists**

1. Check the logs for `"Vector search returned 0 documents"`. If so, the similarity threshold (60%) may be filtering out results — the query phrasing might not be close enough to the stored product text.
2. Check whether the product was actually indexed. Run:
   ```sql
   SELECT COUNT(*) FROM vector_store;
   ```
   If the count is 0, the initializer did not run. Delete any rows and restart.

**Follow-up questions return wrong or no information**

The `/api/ask` endpoint caches the last retrieved product context as a field on the controller. This is a single-user, in-process cache — if the server restarts or a different user session starts, the cache is empty. For the first question in a new session, always ask about the product by name so the cache is populated.

**Port conflict on startup**

The default port is `8080`. To change it:

```properties
server.port=8081
```

**Long path issues on Windows (OneDrive)**

If you see file-locking or path-length errors, move the project to a shorter path without spaces, e.g. `C:\projects\spring-ai-ollama`.

**Maven wrapper blocked on Windows**

Use `mvnw.cmd` in PowerShell or Git Bash. If Windows Defender blocks it, right-click the file → Properties → Unblock.

Collect this output when reporting issues
-----------------------------------------

```powershell
java -version
.\mvnw.cmd -v
.\mvnw.cmd clean package
ollama list
```

License
-------

No license included. Add one if you plan to publish or share this project.
