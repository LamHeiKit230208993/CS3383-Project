"use strict";

const chatWorkspace = document.querySelector("[data-ask-workspace]");
const transcript = chatWorkspace.querySelector("[data-ask-thread]");
const historyItems = chatWorkspace.querySelector("[data-ask-history]");
const recentChats = chatWorkspace.querySelector(".ask-recent");
const composer = chatWorkspace.querySelector(".ask-composer");
const questionField = composer.elements.question;
const sendStatus = chatWorkspace.querySelector("[data-ask-status]");
const retryReply = chatWorkspace.querySelector("[data-ask-retry]");
const askUrl = new URL(chatWorkspace.dataset.askUrl, location.href);
// The first page may carry Tomcat's URL session marker before its cookie returns.
askUrl.pathname = askUrl.pathname.split(";")[0];
const desktop = matchMedia("(min-width: 1051px)");
let accepting = false;
let resolving = false;
let awaitingReply = transcript.dataset.awaitingReply === "true";

function updateHistoryLayout(event) {
  recentChats.open = event.matches;
}

updateHistoryLayout(desktop);
desktop.addEventListener("change", updateHistoryLayout);

function scrollToLatest() {
  transcript.scrollTop = transcript.scrollHeight;
}

function scrollToMessage(id) {
  const message = document.getElementById(id);
  if (!message || !transcript.contains(message)) return false;
  transcript.scrollTop += message.getBoundingClientRect().top - transcript.getBoundingClientRect().top - 12;
  return true;
}

if (!scrollToMessage(location.hash.slice(1))) scrollToLatest();

recentChats.addEventListener("keydown", event => {
  if (event.key === "Escape" && recentChats.open && !desktop.matches) {
    recentChats.open = false;
    recentChats.querySelector("summary").focus();
  }
});

questionField.addEventListener("keydown", event => {
  if (event.key === "Enter" && !event.shiftKey && !event.isComposing && event.keyCode !== 229) {
    event.preventDefault();
    if (!accepting && !awaitingReply) composer.requestSubmit();
  }
});

function addPendingTurn(text) {
  if (!text) return null;
  const bubble = document.createElement("li");
  bubble.className = "chat-message chat-shopper chat-pending";
  const author = document.createElement("strong");
  author.textContent = chatWorkspace.dataset.you;
  const message = document.createElement("p");
  message.textContent = text;
  bubble.append(author, message);
  transcript.querySelector(".chat-messages").append(bubble);
  return bubble;
}

function updateControls() {
  for (const button of chatWorkspace.querySelectorAll('button[type="submit"]')) {
    button.disabled = accepting || awaitingReply;
  }
  const packChoice = transcript.querySelector(".ask-choice select");
  if (packChoice) packChoice.disabled = accepting || awaitingReply;
  // Once the app accepts this message, the shopper can compose the next one.
  questionField.readOnly = accepting;
}

function announce(message, { visible = false, error = false } = {}) {
  sendStatus.classList.toggle("sr-only", !visible);
  sendStatus.classList.toggle("ask-send-error", error);
  sendStatus.textContent = message;
}

function routeFor(action) {
  const route = new URL(askUrl);
  route.pathname += "/" + action;
  return route;
}

function currentChatUrl() {
  const url = new URL(askUrl);
  url.searchParams.set("chatId", composer.elements.chatId.value);
  return url;
}

async function loadChat(url, fields) {
  const options = { cache: "no-store", signal: AbortSignal.timeout(45_000) };
  if (fields) {
    options.method = "POST";
    options.body = new URLSearchParams(fields);
  }
  const response = await fetch(url, options);
  const replyUrl = new URL(response.url || url);
  if (!response.ok || replyUrl.origin !== askUrl.origin || replyUrl.pathname.split(";")[0] !== askUrl.pathname) {
    throw new Error("Chat request failed");
  }
  const replyPage = new DOMParser().parseFromString(await response.text(), "text/html");
  const nextTranscript = replyPage.querySelector("[data-ask-thread]");
  const nextHistory = replyPage.querySelector("[data-ask-history]");
  const chatId = replyPage.querySelector(".ask-composer [name=chatId]")?.value;
  if (!nextTranscript || !nextHistory || !chatId) throw new Error("Chat reply is missing");
  return { nextTranscript, nextHistory, chatId };
}

function renderChat({ nextTranscript, nextHistory, chatId }, messageId = location.hash.slice(1)) {
  const oldScroll = transcript.scrollTop;
  const readingLatest = transcript.scrollHeight - transcript.clientHeight - oldScroll < 80;
  awaitingReply = nextTranscript.dataset.awaitingReply === "true";
  transcript.dataset.awaitingReply = String(awaitingReply);
  transcript.replaceChildren(...nextTranscript.childNodes);
  historyItems.replaceChildren(...nextHistory.childNodes);
  composer.elements.chatId.value = chatId;
  const url = currentChatUrl();
  if (messageId && scrollToMessage(messageId)) {
    url.hash = messageId;
  } else if (readingLatest) {
    scrollToLatest();
  } else {
    transcript.scrollTop = oldScroll;
  }
  history.replaceState(history.state, "", url);
  updateControls();
}

async function submitAction(form, submitter) {
  const fields = new FormData(form, submitter);
  const messageId = form.closest(".chat-message").id;
  const actionId = fields.get("actionId");
  accepting = true;
  updateControls();
  announce(chatWorkspace.dataset.sending);
  try {
    renderChat(await loadChat(form.action, fields), messageId);
    const action = [...transcript.querySelectorAll("[data-ask-action]")]
      .find(section => section.dataset.askAction === actionId);
    const result = action?.querySelector("[data-ask-action-state]") || document.getElementById(messageId);
    result?.focus({ preventScroll: true });
    announce("");
  } catch {
    announce(chatWorkspace.dataset.actionFailed, { visible: true, error: true });
  } finally {
    accepting = false;
    updateControls();
  }
}

async function requestReply() {
  if (!awaitingReply || resolving) return;
  resolving = true;
  retryReply.hidden = true;
  const working = transcript.querySelector("[data-ask-working]");
  if (working) working.hidden = false;
  announce(chatWorkspace.dataset.checking);
  try {
    renderChat(await loadChat(routeFor("reply"), { chatId: composer.elements.chatId.value }));
    // Another tab may already be requesting this reply.
    for (let attempt = 0; awaitingReply && attempt < 30; attempt++) {
      await new Promise(resolve => setTimeout(resolve, 500));
      renderChat(await loadChat(currentChatUrl()));
    }
    if (awaitingReply) throw new Error("Chat reply still pending");
    announce(chatWorkspace.dataset.received);
  } catch {
    const indicator = transcript.querySelector("[data-ask-working]");
    if (indicator) indicator.hidden = true;
    announce(chatWorkspace.dataset.interrupted, { visible: true, error: true });
    retryReply.hidden = false;
  } finally {
    resolving = false;
    updateControls();
  }
}

retryReply.addEventListener("click", () => void requestReply());

chatWorkspace.addEventListener("submit", async event => {
  const form = event.target;
  const action = new URL(form.action);
  const path = action.pathname.split(";")[0];
  const isQuestion = path === askUrl.pathname;
  const isAction = path === askUrl.pathname + "/action";
  if (form.method !== "post" || action.origin !== askUrl.origin ||
      (!isQuestion && !isAction && path !== askUrl.pathname + "/choose")) return;
  event.preventDefault();
  if (accepting || awaitingReply) return;
  if (isAction) {
    await submitAction(form, event.submitter);
    return;
  }

  const fields = new FormData(form, event.submitter);
  if (!isQuestion) fields.set("deferReply", "true");
  const question = fields.get("question");
  const selectedPack = form.elements.code?.selectedOptions[0]?.textContent.trim();
  const pendingBubble = addPendingTurn(question?.trim() || selectedPack);
  const emptyState = transcript.querySelector(".ask-empty");
  const focusComposer = composer.contains(document.activeElement);
  accepting = true;
  if (emptyState) emptyState.hidden = true;
  if (form === composer) questionField.value = "";
  updateControls();
  announce(chatWorkspace.dataset.sending, { visible: true });
  scrollToLatest();

  try {
    renderChat(await loadChat(isQuestion ? routeFor("send") : action, fields), "");
    if (!awaitingReply) announce(chatWorkspace.dataset.received);
  } catch {
    pendingBubble?.remove();
    if (emptyState) emptyState.hidden = false;
    if (question != null) questionField.value = question;
    announce(chatWorkspace.dataset.failed, { visible: true, error: true });
  } finally {
    accepting = false;
    updateControls();
    if (focusComposer && document.activeElement === document.body) questionField.focus({ preventScroll: true });
  }
  if (awaitingReply) void requestReply();
});

updateControls();
if (awaitingReply) void requestReply();
