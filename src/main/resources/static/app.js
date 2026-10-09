const welcomeScreen = document.querySelector("#welcome-screen");
const practiceScreen = document.querySelector("#practice-screen");
const summaryScreen = document.querySelector("#summary-screen");
const startButton = document.querySelector("#start-button");
const answerForm = document.querySelector("#answer-form");
const answerInput = document.querySelector("#answer-input");
const submitButton = document.querySelector("#submit-button");
const nextButton = document.querySelector("#next-button");
const feedbackPanel = document.querySelector("#feedback-panel");
const formError = document.querySelector("#form-error");
const setupError = document.querySelector("#setup-error");

const trackNames = {
  behavioral: "Behavioral practice",
  java: "Java technical practice",
  mixed: "Mixed practice",
};

let questions = [];
let currentIndex = 0;
let totalScore = 0;
let answeredCount = 0;

document.querySelectorAll('input[name="track"]').forEach((radio) => {
  radio.addEventListener("change", () => {
    document.querySelectorAll(".track-option").forEach((option) => {
      option.classList.toggle("selected", option.contains(radio) && radio.checked);
    });
  });
});

startButton.addEventListener("click", startSession);
document.querySelector("#back-button").addEventListener("click", showHome);
document.querySelector("#restart-button").addEventListener("click", startSession);
document.querySelector("#home-button").addEventListener("click", showHome);
answerInput.addEventListener("input", updateWordCount);
answerForm.addEventListener("submit", submitAnswer);
nextButton.addEventListener("click", showNextQuestion);

async function startSession() {
  const track = document.querySelector('input[name="track"]:checked').value;
  startButton.disabled = true;
  startButton.textContent = "Loading questions...";
  setupError.textContent = "";
  try {
    const response = await fetch(`/api/questions?track=${encodeURIComponent(track)}`);
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error || "Couldn't load questions. Please try again.");
    }
    questions = shuffle(data.questions).slice(0, 5);
    currentIndex = 0;
    totalScore = 0;
    answeredCount = 0;
    document.querySelector("#practice-track-label").textContent = trackNames[track];
    welcomeScreen.classList.add("hidden");
    summaryScreen.classList.add("hidden");
    practiceScreen.classList.remove("hidden");
    showQuestion();
  } catch (error) {
    setupError.textContent = error.message;
  } finally {
    startButton.disabled = false;
    startButton.innerHTML = 'Start a practice session <span aria-hidden="true">→</span>';
  }
}

function showQuestion() {
  const question = questions[currentIndex];
  document.querySelector("#progress-label").textContent =
    `Question ${currentIndex + 1} of ${questions.length}`;
  document.querySelector("#progress-fill").style.width =
    `${((currentIndex + 1) / questions.length) * 100}%`;
  document.querySelector("#question-category").textContent = question.category;
  document.querySelector("#question-prompt").textContent = question.prompt;
  document.querySelector("#question-focus").textContent = question.focus;
  document.querySelector("#answered-count").textContent = answeredCount;
  document.querySelector("#answered-fill").style.width =
    `${(answeredCount / questions.length) * 100}%`;
  answerInput.value = "";
  answerInput.disabled = false;
  submitButton.disabled = false;
  submitButton.classList.remove("hidden");
  feedbackPanel.classList.add("hidden");
  answerForm.classList.remove("hidden");
  formError.textContent = "";
  updateWordCount();
  answerInput.focus();
}

async function submitAnswer(event) {
  event.preventDefault();
  const answer = answerInput.value.trim();
  if (!answer) {
    formError.textContent = "Write a few words before requesting feedback.";
    answerInput.focus();
    return;
  }

  submitButton.disabled = true;
  submitButton.textContent = "Reviewing your answer...";
  formError.textContent = "";
  try {
    const body = new URLSearchParams({
      questionId: questions[currentIndex].id,
      answer,
    });
    const response = await fetch("/api/feedback", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body,
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error || "Couldn't check this answer. Please try again.");
    }
    totalScore += data.score;
    answeredCount += 1;
    showFeedback(data);
  } catch (error) {
    formError.textContent = error.message;
    submitButton.disabled = false;
  } finally {
    submitButton.innerHTML = 'Get my feedback <span aria-hidden="true">→</span>';
  }
}

function showFeedback(data) {
  answerInput.disabled = true;
  submitButton.classList.add("hidden");
  feedbackPanel.classList.remove("hidden");
  document.querySelector("#feedback-score").textContent = `${data.score}/3`;
  const feedbackList = document.querySelector("#feedback-list");
  feedbackList.replaceChildren();
  data.criteria.forEach((criterion) => {
    const item = document.createElement("li");
    item.className = `feedback-item${criterion.passed ? " passed" : ""}`;
    const icon = document.createElement("span");
    icon.className = "feedback-item-icon";
    icon.setAttribute("aria-hidden", "true");
    icon.textContent = criterion.passed ? "✓" : "·";
    const copy = document.createElement("div");
    const label = document.createElement("strong");
    label.textContent = criterion.label;
    const message = document.createElement("p");
    message.textContent = criterion.message;
    copy.append(label, message);
    item.append(icon, copy);
    feedbackList.append(item);
  });
  document.querySelector("#answered-count").textContent = answeredCount;
  document.querySelector("#answered-fill").style.width =
    `${(answeredCount / questions.length) * 100}%`;
  nextButton.innerHTML = currentIndex === questions.length - 1
    ? 'See my session <span aria-hidden="true">→</span>'
    : 'Next question <span aria-hidden="true">→</span>';
  nextButton.focus();
}

function showNextQuestion() {
  if (currentIndex < questions.length - 1) {
    currentIndex += 1;
    showQuestion();
    return;
  }
  practiceScreen.classList.add("hidden");
  summaryScreen.classList.remove("hidden");
  document.querySelector("#summary-copy").textContent =
    `You practiced ${answeredCount} ${answeredCount === 1 ? "question" : "questions"}.`
    + " Take what felt strong with you, and remember: every interview is a chance to learn.";
  document.querySelector("#summary-score").replaceChildren();
  const score = document.createElement("strong");
  score.textContent = `${totalScore}/${answeredCount * 3}`;
  const label = document.createElement("span");
  label.textContent = "reflection checklist points";
  document.querySelector("#summary-score").append(score, label);
}

function showHome() {
  practiceScreen.classList.add("hidden");
  summaryScreen.classList.add("hidden");
  welcomeScreen.classList.remove("hidden");
}

function updateWordCount() {
  const value = answerInput.value.trim();
  const count = value ? value.split(/\s+/).length : 0;
  document.querySelector("#word-count").textContent =
    `${count} ${count === 1 ? "word" : "words"}`;
}

function shuffle(items) {
  const shuffled = [...items];
  for (let index = shuffled.length - 1; index > 0; index -= 1) {
    const randomIndex = Math.floor(Math.random() * (index + 1));
    [shuffled[index], shuffled[randomIndex]] = [shuffled[randomIndex], shuffled[index]];
  }
  return shuffled;
}
