import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class InterviewCoach {
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    private static final int MAX_ANSWER_LENGTH = 10_000;
    private static final Path STATIC_ROOT =
            Path.of("src", "main", "resources", "static").toAbsolutePath().normalize();

    private static final List<Question> BEHAVIORAL_QUESTIONS = List.of(
            new Question("behavioral-problem", "Behavioral",
                    "Tell me about a time you solved a difficult problem.",
                    "Describe the situation, your actions, and the result.",
                    List.of("problem", "challenge", "because", "approach", "result", "learned")),
            new Question("behavioral-disagreement", "Behavioral",
                    "Tell me about a time you worked through a disagreement.",
                    "Explain how you listened, communicated, and reached an outcome.",
                    List.of("listened", "discussed", "communicated", "agreed", "resolved", "outcome")),
            new Question("behavioral-mistake", "Behavioral",
                    "Describe a mistake you made and what you learned from it.",
                    "Take ownership and focus on what you changed afterward.",
                    List.of("mistake", "responsibility", "learned", "changed", "improved", "next")),
            new Question("behavioral-deadline", "Behavioral",
                    "Tell me about a time you had to meet a tight deadline.",
                    "Explain how you prioritized and what you delivered.",
                    List.of("deadline", "prioritized", "planned", "team", "delivered", "result")),
            new Question("behavioral-accomplishment", "Behavioral",
                    "Describe an accomplishment you are proud of.",
                    "Make your contribution and the impact clear.",
                    List.of("goal", "built", "created", "improved", "impact", "result"))
    );

    private static final List<Question> JAVA_QUESTIONS = List.of(
            new Question("java-interface", "Java",
                    "What is the difference between an interface and an abstract class in Java?",
                    "Compare their purpose and how a class can use each.",
                    List.of("contract", "implement", "extend", "multiple", "method", "default")),
            new Question("java-collections", "Java",
                    "How do HashMap and HashSet differ?",
                    "Explain what each stores and how uniqueness works.",
                    List.of("key", "value", "unique", "hash", "mapping", "duplicate")),
            new Question("java-equals", "Java",
                    "What is the difference between == and equals()?",
                    "Distinguish primitive comparison from object comparison.",
                    List.of("reference", "value", "object", "content", "override", "primitive")),
            new Question("java-exceptions", "Java",
                    "How does exception handling work in Java?",
                    "Discuss try/catch/finally and when exceptions should be handled.",
                    List.of("try", "catch", "finally", "throw", "checked", "exception")),
            new Question("java-immutable", "Java",
                    "How would you make a class immutable?",
                    "Consider state, access, and how values are exposed.",
                    List.of("final", "private", "setter", "constructor", "copy", "state"))
    );

    private InterviewCoach() {
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : PORT;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", InterviewCoach::handleRequest);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        System.out.println("Interview Coach is running at http://localhost:" + port);
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                handleApi(exchange, path);
            } else {
                serveStaticFile(exchange, path);
            }
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, "{\"error\":" + jsonString(exception.getMessage()) + "}");
        } catch (IOException exception) {
            System.err.println("Request failed: " + exception.getMessage());
            exchange.close();
        }
    }

    private static void handleApi(HttpExchange exchange, String path) throws IOException {
        String method = exchange.getRequestMethod();
        if (path.equals("/api/health") && method.equals("GET")) {
            sendJson(exchange, 200, "{\"status\":\"ok\"}");
            return;
        }
        if (path.equals("/api/questions") && method.equals("GET")) {
            String track = queryParameters(exchange).getOrDefault("track", "mixed");
            List<Question> questions = questionsFor(track);
            String json = questions.stream()
                    .map(InterviewCoach::questionJson)
                    .reduce((left, right) -> left + "," + right)
                    .orElse("");
            sendJson(exchange, 200, "{\"track\":" + jsonString(track)
                    + ",\"questions\":[" + json + "]}");
            return;
        }
        if (path.equals("/api/feedback") && method.equals("POST")) {
            Map<String, String> form = readForm(exchange);
            String questionId = form.getOrDefault("questionId", "");
            String answer = form.getOrDefault("answer", "").trim();
            Question question = findQuestion(questionId);
            if (question == null) {
                sendJson(exchange, 404, "{\"error\":\"Question not found.\"}");
                return;
            }
            if (answer.isBlank()) {
                sendJson(exchange, 400, "{\"error\":\"Write an answer before requesting feedback.\"}");
                return;
            }
            if (answer.length() > MAX_ANSWER_LENGTH) {
                sendJson(exchange, 413, "{\"error\":\"Answers must be 10,000 characters or fewer.\"}");
                return;
            }
            sendJson(exchange, 200, feedbackJson(answer, question));
            return;
        }
        if (path.equals("/api/questions") || path.equals("/api/feedback")
                || path.equals("/api/health")) {
            exchange.getResponseHeaders().set("Allow", allowedMethod(path));
            sendJson(exchange, 405, "{\"error\":\"Method not allowed.\"}");
            return;
        }
        sendJson(exchange, 404, "{\"error\":\"API endpoint not found.\"}");
    }

    private static String allowedMethod(String path) {
        return path.equals("/api/feedback") ? "POST" : "GET";
    }

    private static List<Question> questionsFor(String track) {
        return switch (track.toLowerCase(Locale.ROOT)) {
            case "behavioral" -> BEHAVIORAL_QUESTIONS;
            case "java" -> JAVA_QUESTIONS;
            case "mixed" -> {
                List<Question> questions = new ArrayList<>(BEHAVIORAL_QUESTIONS);
                questions.addAll(JAVA_QUESTIONS);
                yield questions;
            }
            default -> throw new IllegalArgumentException(
                    "Track must be behavioral, java, or mixed.");
        };
    }

    private static Question findQuestion(String id) {
        return allQuestions().stream()
                .filter(question -> question.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    private static List<Question> allQuestions() {
        List<Question> questions = new ArrayList<>(BEHAVIORAL_QUESTIONS);
        questions.addAll(JAVA_QUESTIONS);
        return questions;
    }

    private static Map<String, String> queryParameters(HttpExchange exchange) {
        return parseForm(exchange.getRequestURI().getRawQuery());
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null
                || !contentType.toLowerCase(Locale.ROOT).startsWith("application/x-www-form-urlencoded")) {
            throw new IllegalArgumentException("Expected form-encoded request data.");
        }
        byte[] body = exchange.getRequestBody().readNBytes(MAX_ANSWER_LENGTH + 1024);
        if (body.length > MAX_ANSWER_LENGTH + 1023) {
            throw new IllegalArgumentException("Request body is too large.");
        }
        return parseForm(new String(body, StandardCharsets.UTF_8));
    }

    private static Map<String, String> parseForm(String encoded) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (encoded == null || encoded.isEmpty()) {
            return parameters;
        }
        for (String pair : encoded.split("&")) {
            int separator = pair.indexOf('=');
            String key = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
            parameters.put(
                    URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return parameters;
    }

    private static String questionJson(Question question) {
        return "{\"id\":" + jsonString(question.id())
                + ",\"category\":" + jsonString(question.category())
                + ",\"prompt\":" + jsonString(question.prompt())
                + ",\"focus\":" + jsonString(question.focus()) + "}";
    }

    private static String feedbackJson(String answer, Question question) {
        String normalized = answer.toLowerCase(Locale.ROOT);
        int wordCount = answer.isBlank() ? 0 : answer.split("\\s+").length;
        long relevantSignals = question.signals().stream()
                .filter(normalized::contains)
                .count();
        boolean detailed = wordCount >= 30;
        boolean relevant = relevantSignals >= 2;
        boolean specific = containsSpecificDetail(normalized);
        int score = (detailed ? 1 : 0) + (relevant ? 1 : 0) + (specific ? 1 : 0);
        String criteria = "["
                + criterionJson("detail", "Answer detail", detailed,
                detailed ? "Good detail: your answer has " + wordCount + " words."
                        : "Add detail: aim for at least 30 words to explain your thinking.")
                + ","
                + criterionJson("relevance", "Relevant ideas", relevant,
                relevant ? "Your answer includes ideas relevant to this question."
                        : "Address the question's focus: " + question.focus())
                + ","
                + criterionJson("specificity", "Concrete example", specific,
                specific ? "You included a concrete detail or outcome."
                        : "Add a specific example, action, or measurable result.")
                + "]";
        return "{\"score\":" + score + ",\"maxScore\":3,\"wordCount\":" + wordCount
                + ",\"criteria\":" + criteria + "}";
    }

    private static String criterionJson(String id, String label, boolean passed, String message) {
        return "{\"id\":" + jsonString(id) + ",\"label\":" + jsonString(label)
                + ",\"passed\":" + passed + ",\"message\":" + jsonString(message) + "}";
    }

    private static boolean containsSpecificDetail(String answer) {
        return answer.matches(".*\\b\\d+\\b.*")
                || List.of("increased", "reduced", "saved", "launched", "delivered",
                        "measured", "improved", "result", "outcome")
                        .stream()
                        .anyMatch(answer::contains);
    }

    private static void serveStaticFile(HttpExchange exchange, String requestPath) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            exchange.getResponseHeaders().set("Allow", "GET");
            sendText(exchange, 405, "Method not allowed.", "text/plain; charset=utf-8");
            return;
        }
        String relativePath = requestPath.equals("/") ? "index.html" : requestPath.substring(1);
        Path file = STATIC_ROOT.resolve(relativePath).normalize();
        if (!file.startsWith(STATIC_ROOT) || !Files.isRegularFile(file)) {
            sendText(exchange, 404, "Page not found.", "text/plain; charset=utf-8");
            return;
        }
        String contentType = Files.probeContentType(file);
        if (contentType == null) {
            contentType = contentTypeFor(file);
        }
        sendBytes(exchange, 200, Files.readAllBytes(file), contentType);
    }

    private static String contentTypeFor(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        if (name.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private static String jsonString(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        sendBytes(exchange, status, json.getBytes(StandardCharsets.UTF_8), "application/json; charset=utf-8");
    }

    private static void sendText(HttpExchange exchange, int status, String body, String contentType)
            throws IOException {
        sendBytes(exchange, status, body.getBytes(StandardCharsets.UTF_8), contentType);
    }

    private static void sendBytes(HttpExchange exchange, int status, byte[] body, String contentType)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
                        + "connect-src 'self'; frame-ancestors 'none'; base-uri 'self'");
        exchange.sendResponseHeaders(status, body.length);
        try (var response = exchange.getResponseBody()) {
            response.write(body);
        }
    }

    private record Question(String id, String category, String prompt, String focus, List<String> signals) {
    }
}
