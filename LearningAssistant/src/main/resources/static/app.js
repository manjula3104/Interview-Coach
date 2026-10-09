const STORAGE_KEY = "learnwell-progress-v1";
const homeView = document.querySelector("#home-view");
const lessonsView = document.querySelector("#lessons-view");
const lessonView = document.querySelector("#lesson-view");
const progressView = document.querySelector("#progress-view");

let subjects = [];
let activeSubject = null;
let activeLesson = null;
let quizAnswered = false;
let progress = loadProgress();

document.querySelector("#brand-home").addEventListener("click", (event) => {
  event.preventDefault();
  showHome();
});
document.querySelector("#footer-home").addEventListener("click", (event) => {
  event.preventDefault();
  showHome();
});
document.querySelector("#explore-link").addEventListener("click", () => showHome());
document.querySelector("#progress-nav").addEventListener("click", showProgress);
document.querySelector("#lessons-back").addEventListener("click", showLessons);
document.querySelector("#lesson-back").addEventListener("click", showLessons);
document.querySelector("#progress-back").addEventListener("click", showHome);

loadSubjects();

async function loadSubjects() {
  try {
    const data = await request("/api/subjects");
    subjects = data.subjects;
    document.querySelector("#subject-count").textContent =
      `${subjects.length} LEARNING SHELVES`;
    document.querySelector("#home-available").textContent =
      String(subjects.reduce((total, subject) => total + subject.lessonCount, 0));
    renderSubjects();
    updateHomeProgress();
  } catch (error) {
    document.querySelector("#subject-grid").replaceChildren(
      errorPanel(error.message, loadSubjects));
  }
}

function renderSubjects() {
  const grid = document.querySelector("#subject-grid");
  grid.replaceChildren(...subjects.map((subject) => {
    const card = document.createElement("article");
    card.className = "subject-card";
    const button = document.createElement("button");
    button.type = "button";
    button.className = "subject-card-button";
    button.addEventListener("click", () => openSubject(subject));
    const icon = document.createElement("span");
    icon.className = "subject-icon";
    icon.style.backgroundColor = subject.color;
    icon.textContent = subject.icon;
    icon.setAttribute("aria-hidden", "true");
    const title = document.createElement("h3");
    title.textContent = subject.title;
    const description = document.createElement("p");
    description.textContent = subject.description;
    const footer = document.createElement("div");
    footer.className = "subject-card-footer";
    const count = document.createElement("span");
    count.textContent = `${subject.lessonCount} bite-sized lessons`;
    const progressLabel = document.createElement("strong");
    const completed = subject.lessonsCompleted || 0;
    progressLabel.textContent = completed > 0 ? `${completed} completed` : "Take a look →";
    footer.append(count, progressLabel);
    button.append(icon, title, description, footer);
    card.append(button);
    return card;
  }));
}

async function openSubject(subject) {
  activeSubject = subject;
  lessonsView.classList.remove("hidden");
  homeView.classList.add("hidden");
  lessonView.classList.add("hidden");
  progressView.classList.add("hidden");
  const heading = document.querySelector("#lessons-heading");
  heading.replaceChildren();
  const icon = document.createElement("span");
  icon.className = "subject-icon";
  icon.style.backgroundColor = subject.color;
  icon.textContent = subject.icon;
  const title = document.createElement("h1");
  title.textContent = subject.title;
  const description = document.createElement("p");
  description.textContent = subject.description;
  heading.append(icon, title, description);
  const grid = document.querySelector("#lesson-grid");
  grid.replaceChildren(loadingMessage("Gathering your lessons..."));

  try {
    const data = await request(`/api/lessons?subject=${encodeURIComponent(subject.id)}`);
    renderLessons(data.lessons);
  } catch (error) {
    grid.replaceChildren(errorPanel(error.message, () => openSubject(subject)));
  }
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function renderLessons(lessons) {
  const grid = document.querySelector("#lesson-grid");
  grid.replaceChildren(...lessons.map((lesson, index) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "lesson-card";
    button.addEventListener("click", () => openLesson(lesson.id));
    const number = document.createElement("span");
    number.className = "lesson-number";
    number.textContent = String(index + 1).padStart(2, "0");
    const copy = document.createElement("span");
    copy.className = "lesson-copy";
    const title = document.createElement("strong");
    title.textContent = lesson.title;
    const detail = document.createElement("span");
    detail.textContent = `${lesson.minutes} minute read · ${lesson.questionCount} quick questions`;
    copy.append(title, detail);
    const status = document.createElement("span");
    status.className = "lesson-status";
    const record = progress[lesson.id];
    status.textContent = record?.completed ? "✓ Done" : "";
    const arrow = document.createElement("span");
    arrow.className = "lesson-arrow";
    arrow.textContent = "→";
    button.append(number, copy, status, arrow);
    return button;
  }));
}

async function openLesson(id) {
  quizAnswered = false;
  lessonView.classList.remove("hidden");
  lessonsView.classList.add("hidden");
  homeView.classList.add("hidden");
  progressView.classList.add("hidden");
  const container = document.querySelector("#lesson-content");
  container.replaceChildren(loadingMessage("Opening your lesson..."));

  try {
    activeLesson = await request(`/api/lesson?id=${encodeURIComponent(id)}`);
    renderLesson(activeLesson);
  } catch (error) {
    container.replaceChildren(errorPanel(error.message, () => openLesson(id)));
  }
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function renderLesson(lesson) {
  const container = document.querySelector("#lesson-content");
  const heading = document.createElement("header");
  heading.className = "lesson-page-header";
  const eyebrow = document.createElement("span");
  eyebrow.className = "eyebrow";
  eyebrow.textContent = `${lesson.category.toUpperCase()}  ·  A LITTLE LEARNING`;
  const title = document.createElement("h1");
  title.textContent = lesson.title;
  const intro = document.createElement("p");
  intro.textContent = lesson.introduction;
  const time = document.createElement("span");
  time.className = "lesson-time";
  time.textContent = `◷  About ${lesson.minutes} minutes`;
  heading.append(eyebrow, title, intro, time);

  const layout = document.createElement("div");
  layout.className = "learning-layout";
  const mainColumn = document.createElement("div");
  const lessonBody = document.createElement("article");
  lessonBody.className = "lesson-body";
  const bodyTitle = document.createElement("h2");
  bodyTitle.textContent = "A few ideas to take with you";
  const concepts = document.createElement("ol");
  concepts.className = "concept-list";
  lesson.points.forEach((point, index) => {
    const item = document.createElement("li");
    const number = document.createElement("span");
    number.className = "concept-number";
    number.textContent = String(index + 1);
    const text = document.createElement("span");
    text.textContent = point;
    item.append(number, text);
    concepts.append(item);
  });
  lessonBody.append(bodyTitle, concepts);
  mainColumn.append(lessonBody, renderQuiz(lesson));

  const aside = document.createElement("aside");
  aside.className = "lesson-sidebar";
  const note = document.createElement("section");
  note.className = "lesson-sidebar-card";
  const noteTitle = document.createElement("h3");
  noteTitle.textContent = "A note for your journey";
  const noteText = document.createElement("p");
  noteText.textContent = "There is no rush here. Read through the ideas, try the questions, and take what is useful with you.";
  note.append(noteTitle, noteText);
  const progressNote = document.createElement("section");
  progressNote.className = "lesson-sidebar-card";
  const progressTitle = document.createElement("h3");
  progressTitle.textContent = "Your progress";
  const progressText = document.createElement("p");
  const saved = progress[lesson.id];
  progressText.textContent = saved?.completed
    ? `You have finished this lesson${saved.score == null ? "." : ` with ${saved.score}/${saved.total} quiz answers correct.`}`
    : "Finish a lesson and its quick quiz to add it to your learning journal.";
  progressNote.append(progressTitle, progressText);
  aside.append(note, progressNote);
  layout.append(mainColumn, aside);
  container.replaceChildren(heading, layout);
}

function renderQuiz(lesson) {
  const section = document.createElement("section");
  section.className = "quiz-panel";
  section.id = "quiz-panel";
  const title = document.createElement("h2");
  title.textContent = "A quick check-in";
  const intro = document.createElement("p");
  intro.className = "quiz-intro";
  intro.textContent = "Give these a try. Getting something wrong is just another way to learn.";
  const form = document.createElement("form");
  form.id = "quiz-form";

  lesson.quiz.forEach((question, questionIndex) => {
    const fieldset = document.createElement("fieldset");
    fieldset.className = "quiz-question";
    const legend = document.createElement("legend");
    legend.textContent = `${questionIndex + 1}. ${question.prompt}`;
    fieldset.append(legend);
    question.options.forEach((option, optionIndex) => {
      const label = document.createElement("label");
      const radio = document.createElement("input");
      radio.type = "radio";
      radio.name = `answer${questionIndex}`;
      radio.value = String(optionIndex);
      radio.required = true;
      const text = document.createElement("span");
      text.textContent = option;
      label.append(radio, text);
      fieldset.append(label);
    });
    form.append(fieldset);
  });

  const error = document.createElement("p");
  error.className = "quiz-error";
  error.setAttribute("role", "alert");
  error.id = "quiz-error";
  const submit = document.createElement("button");
  submit.type = "submit";
  submit.className = "button button-primary quiz-submit";
  submit.textContent = "Check my answers →";
  const results = document.createElement("div");
  results.id = "quiz-results";
  results.className = "quiz-results hidden";
  form.append(error, submit, results);
  form.addEventListener("submit", submitQuiz);
  section.append(title, intro, form);
  return section;
}

async function submitQuiz(event) {
  event.preventDefault();
  if (quizAnswered) return;
  const form = event.currentTarget;
  const button = form.querySelector('button[type="submit"]');
  const error = form.querySelector(".quiz-error");
  error.textContent = "";
  button.disabled = true;
  button.textContent = "Checking your answers...";
  try {
    const response = await request("/api/quiz/submit", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: new URLSearchParams(new FormData(form)),
    });
    quizAnswered = true;
    showQuizResults(response);
    recordLesson(response);
    button.remove();
  } catch (requestError) {
    error.textContent = requestError.message;
    button.disabled = false;
    button.textContent = "Check my answers →";
  }
}

function showQuizResults(data) {
  const results = document.querySelector("#quiz-results");
  results.classList.remove("hidden");
  const score = document.createElement("div");
  score.className = "quiz-score";
  score.append(document.createTextNode("You got "));
  const strong = document.createElement("strong");
  strong.textContent = `${data.score}/${data.total}`;
  score.append(strong, document.createTextNode(" right. Nice work showing up."));
  results.append(score);
  data.results.forEach((result, index) => {
    const card = document.createElement("div");
    card.className = `quiz-result ${result.correct ? "good" : "needs-work"}`;
    const title = document.createElement("strong");
    title.textContent = result.correct ? "✓ You got it" : "↗ Keep exploring";
    const explanation = document.createElement("p");
    explanation.textContent = result.correct
      ? result.explanation
      : `The answer is "${activeLesson.quiz[index].options[result.correctAnswer]}". ${result.explanation}`;
    card.append(title, explanation);
    results.append(card);
  });

  const complete = document.createElement("button");
  complete.type = "button";
  complete.className = "button button-primary lesson-complete-button";
  complete.textContent = "✓ Lesson complete";
  complete.addEventListener("click", () => {
    complete.classList.add("is-complete");
    complete.textContent = "✓ Added to your learning journal";
    complete.disabled = true;
    updateHomeProgress();
  });
  results.append(complete);
}

function recordLesson(result) {
  progress[activeLesson.id] = {
    completed: true,
    score: result.score,
    total: result.total,
    updatedAt: new Date().toISOString(),
  };
  saveProgress();
  updateHomeProgress();
}

function updateHomeProgress() {
  const completed = Object.values(progress).filter((entry) => entry.completed).length;
  const count = document.querySelector("#home-completed");
  if (count) count.textContent = String(completed);
  const title = document.querySelector("#home-progress-title");
  const copy = document.querySelector("#home-progress-copy");
  if (title && copy) {
    title.textContent = completed > 0
      ? "Look at you, building your own learning path."
      : "Every expert started somewhere.";
    copy.textContent = completed > 0
      ? `${completed} lesson${completed === 1 ? "" : "s"} finished. Little steps are adding up.`
      : "Choose a lesson, learn something new, and start your own learning streak.";
  }
  const navProgress = document.querySelector("#nav-progress");
  if (navProgress) navProgress.textContent = String(completed);
  if (subjects.length) {
    subjects.forEach((subject) => {
      subject.lessonsCompleted = subject.lessons
        ? subject.lessons.filter((lesson) => progress[lesson.id]?.completed).length
        : Object.keys(progress).filter((id) => progress[id].completed
          && subject.lessonIds.includes(id)).length;
    });
    renderSubjects();
  }
}

async function showProgress() {
  setView(progressView);
  document.querySelector("#progress-content").replaceChildren(loadingMessage("Opening your learning journal..."));
  try {
    const loadedSubjects = await Promise.all(subjects.map(async (subject) => {
      const response = await request(`/api/lessons?subject=${encodeURIComponent(subject.id)}`);
      return { subject, lessons: response.lessons };
    }));
    const records = loadedSubjects.flatMap(({ subject, lessons }) =>
      lessons.filter((lesson) => progress[lesson.id]?.completed)
        .map((lesson) => ({ ...lesson, category: subject.title, ...progress[lesson.id] })));
    renderProgress(records, loadedSubjects);
  } catch (error) {
    document.querySelector("#progress-content").replaceChildren(errorPanel(error.message, showProgress));
  }
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function renderProgress(records, subjectData) {
  const content = document.querySelector("#progress-content");
  const stats = document.createElement("div");
  stats.className = "progress-summary";
  const possible = subjectData.reduce((total, entry) => total + entry.lessons.length, 0);
  const correct = records.reduce((total, lesson) => total + (lesson.score || 0), 0);
  const attempts = records.reduce((total, lesson) => total + (lesson.total || 0), 0);
  stats.append(
    statCard(String(records.length), "lessons completed"),
    statCard(String(possible), "lessons to explore"),
    statCard(attempts ? `${Math.round((correct / attempts) * 100)}%` : "—", "quiz check-ins")
  );
  if (!records.length) {
    const empty = document.createElement("div");
    empty.className = "progress-empty";
    const title = document.createElement("strong");
    title.textContent = "Your story starts with one little lesson.";
    const text = document.createElement("span");
    text.textContent = "Complete a lesson and its quick quiz, and it will find its way here.";
    empty.append(title, text);
    content.replaceChildren(stats, empty);
    return;
  }

  const table = document.createElement("section");
  table.className = "progress-table";
  const title = document.createElement("h2");
  title.textContent = "Your completed lessons";
  table.append(title);
  records.forEach((lesson) => {
    const row = document.createElement("div");
    row.className = "progress-row";
    const copy = document.createElement("div");
    copy.className = "progress-row-copy";
    const name = document.createElement("strong");
    name.textContent = lesson.title;
    const details = document.createElement("span");
    const date = new Date(lesson.updatedAt);
    const dateLabel = Number.isNaN(date.getTime())
      ? "Completed"
      : `Completed ${date.toLocaleDateString(undefined, { month: "short", day: "numeric" })}`;
    details.textContent = `${lesson.category} · ${dateLabel}`;
    copy.append(name, details);
    const score = document.createElement("span");
    score.className = "progress-row-score";
    score.textContent = lesson.total ? `${lesson.score}/${lesson.total}` : "✓";
    row.append(copy, score);
    table.append(row);
  });
  content.replaceChildren(stats, table);
}

function statCard(value, label) {
  const card = document.createElement("div");
  card.className = "progress-stat";
  const number = document.createElement("strong");
  number.textContent = value;
  const description = document.createElement("span");
  description.textContent = label;
  card.append(number, description);
  return card;
}

function showHome() {
  setView(homeView);
  updateHomeProgress();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function showLessons() {
  if (activeSubject) {
    setView(lessonsView);
    openSubject(activeSubject);
    return;
  }
  showHome();
}

function setView(activeView) {
  [homeView, lessonsView, lessonView, progressView].forEach((view) => {
    view.classList.toggle("hidden", view !== activeView);
  });
}

function loadProgress() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (!stored) return {};
    const parsed = JSON.parse(stored);
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
  } catch (error) {
    const notice = document.querySelector("#storage-notice");
    if (notice) {
      notice.textContent = "Saved learning progress could not be read. Your new progress will begin in this browser.";
      notice.classList.remove("hidden");
    }
    return {};
  }
}

function saveProgress() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(progress));
  } catch (error) {
    const notice = document.querySelector("#storage-notice");
    if (notice) {
      notice.textContent = "Your quiz was checked, but this browser could not save your learning progress.";
      notice.classList.remove("hidden");
    }
  }
}

async function request(url, options) {
  const response = await fetch(url, options);
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || "Something went wrong. Please try again.");
  return data;
}

function loadingMessage(message) {
  const panel = document.createElement("div");
  panel.className = "loading-card";
  panel.textContent = message;
  return panel;
}

function errorPanel(message, retry) {
  const panel = document.createElement("div");
  panel.className = "loading-card";
  const copy = document.createElement("p");
  copy.textContent = message;
  const button = document.createElement("button");
  button.type = "button";
  button.className = "button button-primary";
  button.textContent = "Try again";
  button.addEventListener("click", retry);
  panel.append(copy, button);
  return panel;
}
