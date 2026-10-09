# Interview Coach

A full-stack, browser-based interview practice app. The frontend is built with
HTML, CSS, and JavaScript; a Java 17+ backend serves the site and provides REST
endpoints for questions and answer feedback. It has no third-party Java
dependencies, account setup, database, or external AI service.

## Run on Windows

From the project folder, double-click `run.bat`, or run it from PowerShell:

```text
.\run.bat
```

Then open [http://localhost:8080](http://localhost:8080) in your browser.
Set the `PORT` environment variable to change the port, or pass a port to the
Java application.

## Run manually

```text
javac --add-modules jdk.httpserver -d out src/main/java/InterviewCoach.java
java --add-modules jdk.httpserver -cp out InterviewCoach
```

## Features

- Responsive web interface with behavioral, Java technical, and mixed tracks.
- Five randomized questions per practice session.
- Multi-line answer entry, live word count, session progress, and a session
  summary.
- Backend-generated feedback checklist for answer detail, relevance, and
  concrete examples.
- Same-origin REST API; answers are processed in memory and are not saved.

The coach feedback is a transparent, rule-based checklist, not an AI
interviewer or an assessment of your ability. The session score is a reflection
prompt, not a measure of interview readiness.

## API

- `GET /api/health` — backend status.
- `GET /api/questions?track=behavioral|java|mixed` — questions for a track.
- `POST /api/feedback` — form-encoded `questionId` and `answer`; returns a
  score, word count, and feedback criteria.
