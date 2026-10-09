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

public class LearningAssistant {
    private static final int DEFAULT_PORT = 8081;
    private static final int MAX_REQUEST_SIZE = 16_384;
    private static final Path STATIC_ROOT =
            Path.of("src", "main", "resources", "static").toAbsolutePath().normalize();
    private static final List<Subject> SUBJECTS = createSubjects();

    private LearningAssistant() {
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0
                ? Integer.parseInt(args[0])
                : Integer.parseInt(System.getenv().getOrDefault("PORT", String.valueOf(DEFAULT_PORT)));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", LearningAssistant::handleRequest);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        System.out.println("Learning Assistant is running at http://localhost:" + port);
    }

    private static List<Subject> createSubjects() {
        return List.of(
                new Subject("java", "Java foundations", "Build confidence with core Java concepts.",
                        "☕", "#eaf0e5", List.of(
                        new Lesson("java-variables", "Variables & data types", "Java", 6,
                                "Learn how Java names, stores, and checks the values your programs use.",
                                List.of(
                                        "Declare a variable with a type, a name, and an optional starting value.",
                                        "Primitive types such as int and boolean store simple values.",
                                        "Use String for text. Java checks that assigned values match their declared types."
                                ),
                                List.of(
                                        new QuizQuestion("Which type is designed to store whole numbers?",
                                                List.of("boolean", "int", "String"), 1,
                                                "int stores whole-number values."),
                                        new QuizQuestion("Which type is commonly used for text?",
                                                List.of("String", "double", "char"), 0,
                                                "String represents a sequence of text characters.")
                                )),
                        new Lesson("java-loops", "Loops & repetition", "Java", 7,
                                "Use loops to repeat a task without writing the same instructions again and again.",
                                List.of(
                                        "A for loop is useful when you know how many times to repeat.",
                                        "A while loop repeats as long as its condition is true.",
                                        "Update the loop state so the condition eventually becomes false."
                                ),
                                List.of(
                                        new QuizQuestion("Which loop fits a known number of repetitions?",
                                                List.of("for", "switch", "try"), 0,
                                                "A for loop keeps the initialization, condition, and update together."),
                                        new QuizQuestion("What should a loop condition do over time?",
                                                List.of("Stay true forever", "Eventually become false", "Change the data type"),
                                                1, "A changing loop condition allows a loop to finish.")
                                )),
                        new Lesson("java-methods", "Methods & reusable code", "Java", 8,
                                "Organize a program into small, named actions that can accept input and return a result.",
                                List.of(
                                        "A method name describes an action or calculation.",
                                        "Parameters provide input; a return type describes the output.",
                                        "A void method performs an action without returning a value."
                                ),
                                List.of(
                                        new QuizQuestion("What do method parameters represent?",
                                                List.of("Input values", "The class name", "A loop condition"), 0,
                                                "Parameters let a caller pass values into a method."),
                                        new QuizQuestion("What does void mean as a method return type?",
                                                List.of("The method returns text", "The method returns nothing", "The method is private"),
                                                1, "void means the method does not return a value.")
                                ))
                )),
                new Subject("web", "Web development", "Understand how the web turns code into pages.",
                        "⌘", "#f1eae4", List.of(
                        new Lesson("web-html", "HTML page structure", "Web", 6,
                                "Discover how HTML describes the meaning and structure of a web page.",
                                List.of(
                                        "HTML elements describe content such as headings, paragraphs, and navigation.",
                                        "Elements are written with tags and may contain other elements.",
                                        "Semantic elements such as main and article communicate meaning."
                                ),
                                List.of(
                                        new QuizQuestion("Which element is meant for the main page content?",
                                                List.of("main", "title", "meta"), 0,
                                                "The main element identifies the primary content of the page."),
                                        new QuizQuestion("What does semantic HTML help communicate?",
                                                List.of("Meaning and structure", "Internet speed", "Screen brightness"),
                                                0, "Semantic tags describe what their content means.")
                                )),
                        new Lesson("web-css", "CSS & visual design", "Web", 7,
                                "Use CSS rules to shape the appearance, layout, and responsive behavior of a page.",
                                List.of(
                                        "A CSS rule matches elements with a selector and applies declarations.",
                                        "The box model includes content, padding, border, and margin.",
                                        "Flexible layouts and media queries help pages adapt to different screens."
                                ),
                                List.of(
                                        new QuizQuestion("What does a CSS selector identify?",
                                                List.of("Elements to style", "A Java variable", "A network address"), 0,
                                                "Selectors match the elements that receive a CSS rule."),
                                        new QuizQuestion("Which CSS feature can adapt a layout to screen size?",
                                                List.of("Media queries", "HTML comments", "Java packages"), 0,
                                                "Media queries apply styles when conditions such as viewport width match.")
                                )),
                        new Lesson("web-js", "JavaScript interactions", "Web", 8,
                                "Add behavior to web pages by responding to events and updating the interface.",
                                List.of(
                                        "JavaScript can respond to user events such as clicks and form submissions.",
                                        "The DOM represents the page so scripts can find and update elements.",
                                        "Use fetch to request data from a server asynchronously."
                                ),
                                List.of(
                                        new QuizQuestion("What does the DOM represent?",
                                                List.of("The page structure", "A CSS color", "A database password"), 0,
                                                "The Document Object Model represents the page in a tree of objects."),
                                        new QuizQuestion("Which browser API is commonly used to make HTTP requests?",
                                                List.of("fetch", "margin", "int"), 0,
                                                "fetch makes network requests and returns a promise.")
                                ))
                )),
                new Subject("science", "Everyday science", "Explore clear, practical science essentials.",
                        "✳", "#e9edf2", List.of(
                        new Lesson("science-motion", "Forces & motion", "Science", 6,
                                "Explore how pushes, pulls, and mass affect the way objects move.",
                                List.of(
                                        "A force is a push or a pull that can change an object's motion.",
                                        "An object's mass describes how much matter it contains.",
                                        "For a given force, a larger mass accelerates less than a smaller mass."
                                ),
                                List.of(
                                        new QuizQuestion("A force is best described as a...",
                                                List.of("Push or pull", "Kind of temperature", "Unit of time"), 0,
                                                "Forces are pushes or pulls that can affect motion."),
                                        new QuizQuestion("With the same force, which object accelerates less?",
                                                List.of("The one with more mass", "The one with less mass", "Both always accelerate equally"),
                                                0, "More mass means less acceleration for the same applied force.")
                                )),
                        new Lesson("science-ecosystems", "Ecosystems & food webs", "Science", 7,
                                "See how living things and their surroundings interact in an ecosystem.",
                                List.of(
                                        "An ecosystem includes living organisms and the environment around them.",
                                        "Producers such as plants capture energy, while consumers eat other organisms.",
                                        "A food web shows how many food chains connect in an ecosystem."
                                ),
                                List.of(
                                        new QuizQuestion("What role do plants usually play in a food web?",
                                                List.of("Producer", "Decomposer only", "Non-living resource"), 0,
                                                "Plants are producers because they capture energy and make food."),
                                        new QuizQuestion("What does a food web show?",
                                                List.of("Connected food chains", "The age of every organism", "Daily weather"),
                                                0, "A food web links the feeding relationships in an ecosystem.")
                                )),
                        new Lesson("science-space", "The solar system", "Science", 8,
                                "Take a quick tour of the Sun and the objects that travel around it.",
                                List.of(
                                        "The Sun is a star at the center of our solar system.",
                                        "Planets travel around the Sun along orbital paths.",
                                        "Gravity helps keep planets and other objects in orbit."
                                ),
                                List.of(
                                        new QuizQuestion("What is the Sun?",
                                                List.of("A star", "A planet", "A moon"), 0,
                                                "The Sun is the star at the center of our solar system."),
                                        new QuizQuestion("What helps keep planets in orbit?",
                                                List.of("Gravity", "Sound", "Ocean currents"), 0,
                                                "Gravity attracts orbiting objects toward the Sun.")
                                ))
                ))
        );
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                handleApi(exchange, path);
            } else {
                serveStatic(exchange, path);
            }
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, "{\"error\":" + json(exception.getMessage()) + "}");
        } catch (IOException exception) {
            System.err.println("Request failed: " + exception.getMessage());
            exchange.close();
        }
    }

    private static void handleApi(HttpExchange exchange, String path) throws IOException {
        String method = exchange.getRequestMethod();
        if (path.equals("/api/health") && method.equals("GET")) {
            sendJson(exchange, 200, "{\"status\":\"ok\"}");
        } else if (path.equals("/api/subjects") && method.equals("GET")) {
            String subjects = SUBJECTS.stream().map(LearningAssistant::subjectJson)
                    .reduce((left, right) -> left + "," + right).orElse("");
            sendJson(exchange, 200, "{\"subjects\":[" + subjects + "]}");
        } else if (path.equals("/api/lessons") && method.equals("GET")) {
            String subjectId = query(exchange).getOrDefault("subject", "");
            Subject subject = findSubject(subjectId);
            if (subject == null) {
                sendJson(exchange, 404, "{\"error\":\"Subject not found.\"}");
                return;
            }
            String lessons = subject.lessons().stream()
                    .map(LearningAssistant::lessonSummaryJson)
                    .reduce((left, right) -> left + "," + right).orElse("");
            sendJson(exchange, 200, "{\"subject\":" + json(subject.id())
                    + ",\"lessons\":[" + lessons + "]}");
        } else if (path.equals("/api/lesson") && method.equals("GET")) {
            String lessonId = query(exchange).getOrDefault("id", "");
            Lesson lesson = findLesson(lessonId);
            if (lesson == null) {
                sendJson(exchange, 404, "{\"error\":\"Lesson not found.\"}");
                return;
            }
            sendJson(exchange, 200, lessonJson(lesson));
        } else if (path.equals("/api/quiz/submit") && method.equals("POST")) {
            Map<String, String> form = readForm(exchange);
            Lesson lesson = findLesson(form.getOrDefault("lessonId", ""));
            if (lesson == null) {
                sendJson(exchange, 404, "{\"error\":\"Lesson not found.\"}");
                return;
            }
            for (int index = 0; index < lesson.quiz().size(); index++) {
                if (parseAnswer(form.get("answer" + index),
                        lesson.quiz().get(index).options().size()) < 0) {
                    throw new IllegalArgumentException("Select an answer for every quiz question.");
                }
            }
            List<QuizResult> results = new ArrayList<>();
            int score = 0;
            for (int index = 0; index < lesson.quiz().size(); index++) {
                QuizQuestion question = lesson.quiz().get(index);
                String selectedValue = form.get("answer" + index);
                int selected = parseAnswer(selectedValue, question.options().size());
                boolean correct = selected == question.answerIndex();
                if (correct) {
                    score++;
                }
                results.add(new QuizResult(question.prompt(), selected, correct,
                        question.answerIndex(), question.explanation()));
            }
            String response = results.stream().map(LearningAssistant::quizResultJson)
                    .reduce((left, right) -> left + "," + right).orElse("");
            sendJson(exchange, 200, "{\"score\":" + score
                    + ",\"total\":" + results.size() + ",\"results\":[" + response + "]}");
        } else if (path.equals("/api/health") || path.equals("/api/subjects")
                || path.equals("/api/lessons") || path.equals("/api/lesson")
                || path.equals("/api/quiz/submit")) {
            exchange.getResponseHeaders().set("Allow", path.equals("/api/quiz/submit") ? "POST" : "GET");
            sendJson(exchange, 405, "{\"error\":\"Method not allowed.\"}");
        } else {
            sendJson(exchange, 404, "{\"error\":\"API endpoint not found.\"}");
        }
    }

    private static int parseAnswer(String value, int optionCount) {
        if (value == null) {
            return -1;
        }
        try {
            int selected = Integer.parseInt(value);
            return selected >= 0 && selected < optionCount ? selected : -1;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static Subject findSubject(String id) {
        return SUBJECTS.stream().filter(subject -> subject.id().equals(id)).findFirst().orElse(null);
    }

    private static Lesson findLesson(String id) {
        return SUBJECTS.stream().flatMap(subject -> subject.lessons().stream())
                .filter(lesson -> lesson.id().equals(id)).findFirst().orElse(null);
    }

    private static String subjectJson(Subject subject) {
        return "{\"id\":" + json(subject.id())
                + ",\"title\":" + json(subject.title())
                + ",\"description\":" + json(subject.description())
                + ",\"icon\":" + json(subject.icon())
                + ",\"color\":" + json(subject.color())
                + ",\"lessonCount\":" + subject.lessons().size()
                + ",\"lessonIds\":[" + subject.lessons().stream().map(Lesson::id)
                .map(LearningAssistant::json).reduce((left, right) -> left + "," + right)
                .orElse("") + "]}";
    }

    private static String lessonSummaryJson(Lesson lesson) {
        return "{\"id\":" + json(lesson.id())
                + ",\"title\":" + json(lesson.title())
                + ",\"category\":" + json(lesson.category())
                + ",\"minutes\":" + lesson.minutes()
                + ",\"questionCount\":" + lesson.quiz().size() + "}";
    }

    private static String lessonJson(Lesson lesson) {
        String points = lesson.points().stream().map(LearningAssistant::json)
                .reduce((left, right) -> left + "," + right).orElse("");
        String questions = lesson.quiz().stream()
                .map(question -> "{\"prompt\":" + json(question.prompt())
                        + ",\"options\":[" + question.options().stream().map(LearningAssistant::json)
                        .reduce((left, right) -> left + "," + right).orElse("") + "]}")
                .reduce((left, right) -> left + "," + right).orElse("");
        return "{\"id\":" + json(lesson.id())
                + ",\"title\":" + json(lesson.title())
                + ",\"category\":" + json(lesson.category())
                + ",\"minutes\":" + lesson.minutes()
                + ",\"introduction\":" + json(lesson.introduction())
                + ",\"points\":[" + points + "],\"quiz\":[" + questions + "]}";
    }

    private static String quizResultJson(QuizResult result) {
        return "{\"prompt\":" + json(result.prompt())
                + ",\"selected\":" + result.selected()
                + ",\"correct\":" + result.correct()
                + ",\"correctAnswer\":" + result.correctAnswer()
                + ",\"explanation\":" + json(result.explanation()) + "}";
    }

    private static Map<String, String> query(HttpExchange exchange) {
        return parseForm(exchange.getRequestURI().getRawQuery());
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT)
                .startsWith("application/x-www-form-urlencoded")) {
            throw new IllegalArgumentException("Expected form-encoded request data.");
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_REQUEST_SIZE + 1);
        if (bytes.length > MAX_REQUEST_SIZE) {
            throw new IllegalArgumentException("Request body is too large.");
        }
        return parseForm(new String(bytes, StandardCharsets.UTF_8));
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
            parameters.put(URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return parameters;
    }

    private static void serveStatic(HttpExchange exchange, String requestPath) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            exchange.getResponseHeaders().set("Allow", "GET");
            sendText(exchange, 405, "Method not allowed.", "text/plain; charset=utf-8");
            return;
        }
        String relative = requestPath.equals("/") ? "index.html" : requestPath.substring(1);
        Path file = STATIC_ROOT.resolve(relative).normalize();
        if (!file.startsWith(STATIC_ROOT) || !Files.isRegularFile(file)) {
            sendText(exchange, 404, "Page not found.", "text/plain; charset=utf-8");
            return;
        }
        String type = Files.probeContentType(file);
        if (type == null) {
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            type = name.endsWith(".html") ? "text/html; charset=utf-8"
                    : name.endsWith(".css") ? "text/css; charset=utf-8"
                    : name.endsWith(".js") ? "text/javascript; charset=utf-8"
                    : "application/octet-stream";
        }
        sendBytes(exchange, 200, Files.readAllBytes(file), type);
    }

    private static String json(String value) {
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

    private static void sendJson(HttpExchange exchange, int status, String value) throws IOException {
        sendBytes(exchange, status, value.getBytes(StandardCharsets.UTF_8), "application/json; charset=utf-8");
    }

    private static void sendText(HttpExchange exchange, int status, String value, String type)
            throws IOException {
        sendBytes(exchange, status, value.getBytes(StandardCharsets.UTF_8), type);
    }

    private static void sendBytes(HttpExchange exchange, int status, byte[] bytes, String type)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; "
                        + "img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var body = exchange.getResponseBody()) {
            body.write(bytes);
        }
    }

    private record Subject(String id, String title, String description, String icon,
                           String color, List<Lesson> lessons) {
    }

    private record Lesson(String id, String title, String category, int minutes,
                          String introduction, List<String> points, List<QuizQuestion> quiz) {
    }

    private record QuizQuestion(String prompt, List<String> options, int answerIndex, String explanation) {
    }

    private record QuizResult(String prompt, int selected, boolean correct,
                              int correctAnswer, String explanation) {
    }
}
