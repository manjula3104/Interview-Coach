# Learning Assistant

A full-stack, browser-based learning app with a Java 17+ backend and a
responsive HTML/CSS/JavaScript frontend. Explore Java, web development, and
everyday science through short lessons and two-question quizzes.

## Run on Windows

From PowerShell or the VS Code terminal:

```powershell
cd "C:\Users\deept\OneDrive\Desktop\AGENTIC_AI\LearningAssistant"
.\run.bat
```

Open [http://localhost:8081](http://localhost:8081). Keep the terminal open
while you use the app; press Ctrl+C to stop the server.

To use a different port:

```powershell
java --add-modules jdk.httpserver -cp out LearningAssistant 8082
```

## Features

- Three subject shelves and nine ready-to-study lessons.
- Short concept explanations and interactive, multiple-choice quizzes.
- Backend-graded answers with explanations.
- Learning journal with completed lessons and quiz progress, saved in browser
  local storage.
- No external libraries, database, accounts, or third-party services required.

The learning journal stays in the browser on the device being used. The Java
server keeps lesson and quiz content in memory.

## API

- `GET /api/health` — server status.
- `GET /api/subjects` — available subject shelves.
- `GET /api/lessons?subject=java` — lesson list for a subject.
- `GET /api/lesson?id=java-variables` — lesson content and quiz options.
- `POST /api/quiz/submit` — form-encoded lesson ID and selected answer indexes;
  returns a score and answer explanations.
