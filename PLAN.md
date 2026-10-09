# Gemce — AI study buddy that works anywhere you go

Planning document v0.2 · 9 Oct 2026 · Owner: Melo
Changes in v0.2: app named Gemce; added interactive test (M16) and Ask Gemce Q&A (M17); added terminal install cheat sheet (11.9).
Target: native Android (Kotlin + Jetpack Compose) · On-device model: Gemma 4 E2B via LiteRT-LM

> How to use this file: keep it in the project root. Treat it as the source of truth for scope and architecture, and point your coding agent at it (see section 16). Items marked **TBD** or **verify** must be settled by the Phase 0 spike (section 12), not guessed.

---

## 1. Product definition

**Tagline.** AI study buddy that works anywhere you go.

**One-liner.** Gemce is an Android app that turns a student's own notes (photos, PDF, DOCX, PPTX) into flashcards, quizzes and a custom reviewer, gives interactive tests, answers questions about the notes, and explains everything in English, Taglish, Filipino or Bisaya. Everything runs on the phone. After a one-time model download, no signal or mobile data is needed.

**Problem.** In many Philippine schools there is little or no signal and mobile data is expensive, so cloud AI study tools are unusable exactly when students need quick help (in the classroom, between classes, in the review center).

**Primary users.** College students and board-exam takers. Secondary: senior high school students.

**Value proposition.**
1. Works fully offline after setup.
2. Private: notes and generated material never leave the device.
3. Local languages: Taglish, Filipino and Bisaya explanations (quality to be validated, see section 13).
4. Built from the student's own materials, so content matches their course.

**Non-goals for the MVP.** Cloud AI, accounts or login, social features, iOS, runtime code generation, in-app purchases, web search, tablet or desktop layouts.

---

## 2. Design principles

1. **The model is the language layer, not the source of truth.** It rewrites, summarizes, quizzes and explains the student's own text. It does not decide what is correct.
2. **Ground everything in the source.** Every card and question links back to the section it came from. Anything the model adds from its own knowledge is labeled "unverified".
3. **Structured output, fixed UI.** The model emits data (JSON Lines). The app renders it with a fixed set of screens. The app never generates or runs code at runtime. Small on-device models are unreliable at code, and Google Play restricts apps that download and execute new code.
4. **The study loop works without AI.** Manual cards, spaced repetition and quiz mode must be fully usable when the model is slow, absent or wrong.
5. **Offline-first and private by default.** No analytics SDKs, no cloud calls except the one-time model download.
6. **Stay responsive despite slow inference.** Background processing, streamed results, and cached outputs per section.

---

## 3. Features

### MVP (v0.1)

| ID | Feature | Notes |
|----|---------|-------|
| M1 | Model setup | First-run download with resume, SHA-256 check, free-space check, progress, delete and re-download. Also "import model from file". |
| M2 | Import from camera or gallery | OCR with ML Kit (Latin script covers English, Filipino, Bisaya). |
| M3 | Import PDF | Text extraction first; render and OCR pages when a PDF has no text layer. |
| M4 | Import DOCX and PPTX | Unzip and parse XML. Legacy .doc and .ppt are out of scope. |
| M5 | Paste text | Fastest path and good for testing. |
| M6 | Review and edit extracted text | OCR is imperfect; the student fixes text before generating. |
| M7 | Generate study material per section | Flashcards, multiple-choice quiz, key-points reviewer. Language selector: English, Taglish, Filipino, Bisaya (Bisaya marked experimental until validated). |
| M8 | Flashcard study mode | Flip cards, daily queue scheduled by SM-2 spaced repetition. |
| M9 | Quiz mode | Score, per-question explanation, link to source section. |
| M10 | Reviewer view | Readable key points per section, tap to jump to the source text. |
| M11 | "Explain this" | Short, grounded Q&A on the selected section ("Explain in Taglish", "give an example"). |
| M12 | Library | Documents and decks, edit and delete cards, full-text search (SQLite FTS). |
| M13 | Settings | Default language, "process only while charging", storage usage, clear all data. |
| M14 | Manual card creation | So the app is useful with zero AI. |
| M15 | Export | Anki-compatible CSV. |
| M16 | **Interactive test** | The student asks Gemce to test them. They pick the scope (document or sections), number of questions, and language. Questions are shown one at a time: multiple choice, fill-in-the-blank and short answer. Short answers are graded by the model against the source text and a reference answer, with feedback and the missing points. The student can override any AI grade. Ends with a score report and weak topics, which feed the flashcard queue. Optional timer. |
| M17 | **Ask Gemce (Q&A)** | A chat about the student's own materials. The app finds the most relevant sections (full-text search), gives them to the model, and streams an answer with section citations. If the notes do not contain the answer, Gemce says so before adding outside knowledge, and labels that part "unverified". Works per document or across the whole library. |

### v0.2
Adaptive interactive tests (repeat weak topics, vary difficulty); voice answers and voice Q&A; matching games; handwriting enhancement using the model's vision input; voice question using audio input; offline sharing of the model file and APK between classmates; study reminders; FSRS scheduling; optional larger model (E4B) on high-RAM phones.

### Later
Subject packs, board-exam simulator, streaks, tablet layout.

---

## 4. MVP user flow

1. First launch: explain offline promise, download model on Wi-Fi (or import from file). Show size before downloading.
2. Home: "Add material" (camera, gallery, PDF, DOCX, PPTX, paste).
3. Extraction: show progress, then the normalized text split into sections.
4. Student reviews and edits text, selects sections, picks language.
5. Generate: sections are processed one at a time in the background. Cards appear as they are produced.
6. Study: flashcards, quiz, reviewer. Progress and due counts on Home.
7. Test me: choose scope, length and language; answer one question at a time; get feedback after each answer and a score report at the end.
8. Ask Gemce: type a question about your notes; get a streamed answer with citations; tap a citation to open the source section.
9. Anytime: "Explain this" on a card or section.

---

## 5. Architecture

### 5.1 Layers

```
┌─────────────────────────────────────────────────────────────┐
│ UI (Jetpack Compose, single Activity, Navigation)           │
│  Home · Import · Review Text · Generate · Study · Library   │
└───────────────▲─────────────────────────────────────────────┘
                │ StateFlow / events (MVVM, unidirectional)
┌───────────────┴─────────────────────────────────────────────┐
│ Domain (plain Kotlin): use cases + interfaces                │
│  IngestDocument · ChunkSections · GenerateMaterial ·         │
│  ScheduleReview (SM-2) · ExplainSection · RunTest · AskGemce │
└───────▲──────────────▲──────────────────▲───────────────────┘
        │              │                  │
┌───────┴──────┐ ┌─────┴─────────┐ ┌──────┴───────────────────┐
│ Ingest       │ │ AI            │ │ Data                     │
│ ML Kit OCR   │ │ LlmEngine     │ │ Room (SQLite + FTS)      │
│ PdfBox-      │ │  (interface)  │ │ DataStore (settings)     │
│ Android      │ │ LiteRtLmEngine│ │ App files (model, images)│
│ DOCX/PPTX    │ │ FakeEngine    │ │                          │
│ XML parsers  │ │ ModelManager  │ │                          │
└──────────────┘ └───────────────┘ └──────────────────────────┘
```

Key rule: the domain layer talks to `LlmEngine`, an interface. Production uses `LiteRtLmEngine`; UI development and unit tests use `FakeEngine`, which returns canned JSONL. This lets you build and test the whole app without waiting on the model.

### 5.2 Tech stack

| Concern | Choice | Notes |
|---------|--------|-------|
| Language, UI | Kotlin, Jetpack Compose, Material 3 | Use the versions the Android Studio New Project wizard gives you. |
| Architecture | MVVM, coroutines + Flow, Hilt (or Koin) | Single Gradle module with package-by-feature; split later if needed. |
| On-device LLM | LiteRT-LM Kotlin API: `com.google.ai.edge.litertlm:litertlm-android` | Pin an exact version in release builds instead of `latest.release`. |
| Model | Gemma 4 E2B, `.litertlm` build from the `litert-community/gemma-4-E2B-it-litert-lm` Hugging Face repo | File is about 2.6 GB. Verify license terms at release time (the model is listed as Apache 2.0). |
| OCR | ML Kit Text Recognition v2, **bundled** artifact `com.google.mlkit:text-recognition` | The bundled variant works offline from install. The `play-services-mlkit-*` variant relies on Google Play services fetching the model. Verify latest version. |
| PDF text | `com.tom-roush:pdfbox-android` | Initialize its resource loader before use. Scanned pages: Android `PdfRenderer` to bitmap, then OCR. |
| DOCX, PPTX | `java.util.zip` + `XmlPullParser` | DOCX: `word/document.xml` (paragraphs, heading styles). PPTX: `ppt/slides/slideN.xml` and `ppt/notesSlides`. Avoid Apache POI on Android (heavy and awkward). |
| Database | Room (+ FTS) | Local only. |
| Settings | DataStore | |
| Background work | WorkManager (foreground or expedited work) | Generation must survive leaving the app. Declare the correct foreground service type (verify requirements for API 36). |
| Camera | CameraX, or the system photo picker and TakePicture contract for MVP | |
| Serialization | kotlinx.serialization | For JSONL contracts. |
| Spaced repetition | SM-2 (own implementation) | Deterministic and simple; FSRS later. |

### 5.3 Ingestion pipeline

```
file / photo → text extraction → normalize → split into sections → store
```

- Normalize to an internal structure that keeps provenance: `Document → Section(order, title, text, sourceRef)`, where `sourceRef` is page number, slide number or heading.
- Split by headings, slides or pages first, then by size so each chunk fits the model comfortably. Start with roughly 800–1,500 words per chunk and **tune from measurements**. Do not assume the advertised 128K context is usable on a phone.
- Never send a whole document in one prompt. Process section by section.

### 5.4 Generation pipeline

```
section text + language + task → prompt → LlmEngine (stream)
  → JSONL line parser → validate → store (cards/questions) → UI updates
```

- **Output format: JSON Lines**, one object per line. Parse each completed line as it streams. A malformed line is skipped (or retried once); it never ruins the whole batch.
- Validate against the schema (section 7). Reject cards with empty fields, duplicates, or text not supported by the source (simple overlap check against the section).
- Cache results per `(sectionId, task, language, promptVersion)` so nothing is generated twice.
- Disable or cap "thinking" for card generation (`thinkingTokenBudget` low or 0) to save time; consider thinking only for "Explain this".
- Run one generation at a time. Queue the rest. Show "n of m sections done".

### 5.5 Model manager

- Model lives in app-private storage (no storage permission needed).
- States: `NotInstalled → Downloading → Verifying → Ready → Failed`.
- Resumable download (HTTP range requests, WorkManager), SHA-256 verification, free-space pre-check, Wi-Fi-only default.
- "Import from file" path for people who got the file from a classmate or SD card.
- Distribution: host the model yourself or point to the Hugging Face file, or use Play Asset Delivery. Check Play's current asset-pack size limits; a 2.6 GB model probably will not fit in a single pack. Decide in Phase 4.

### 5.6 Engine lifecycle and performance

- `engine.initialize()` can take several seconds. Always call it off the main thread. Set `cacheDir` in `EngineConfig` to speed up later loads.
- Keep one `Engine` alive while the user is generating or studying; release it after an idle timeout to free RAM.
- Create short-lived `Conversation` objects per task.
- GPU backend needs two `uses-native-library` entries in the manifest (`libvndksupport.so`, `libOpenCL.so`, both `required="false"`). Try CPU and GPU on your phone and keep the faster, stabler one; fall back to CPU automatically.
- Multi-token prediction (speculative decoding, experimental flag) is recommended for GPU backends; test whether it helps on your device.
- Handle `LiteRtLmJniException` and out-of-memory conditions: show a clear message, never crash.

### 5.7 Interactive test and Ask Gemce (Q&A)

**Interactive test (M16).**
- `TestSession` holds scope, language, the question list, the current index, answers and an optional timer. State is saved so a test survives the app being closed.
- Question sources, cheapest first: (1) multiple-choice questions already in the cache (instant); (2) fill-in-the-blank built by code from cards and key points (blank out a key term, no model needed); (3) short-answer questions generated by the model per section (`type: "short"`).
- While the student answers question N, prepare question N+1 (and grade N−1 if needed) in the background so the session never waits on the model. Only one engine call runs at a time.
- Grading: multiple-choice and fill-in answers are graded by code (normalized match with a small typo tolerance). Short answers are graded by the model, given the question, reference answer, rubric, source evidence and the student's answer, and it returns one JSON object (section 7).
- Model grading is an aid, never final. Label it "AI-graded", show the reference answer next to it, and offer a one-tap "I was right / I was wrong" override. The override wins and feeds spaced repetition.
- Results screen: score, time, per-question review, weak topics (sections with the most misses). Wrong or partial answers create or boost flashcards due soon.

**Ask Gemce (M17).**
- Retrieval: SQLite full-text search over `section_fts` picks the top 3–5 sections for the question. Keyword search is enough for the MVP; embedding-based retrieval is a later upgrade. For mixed Filipino/English questions, also search the question's key terms (lowercased, stopwords removed).
- Prompt: system instruction ("answer from the provided sections, cite them as [S1], [S2], and if the answer is not in them say so first"), the sections labeled S1..Sn, the last 3–4 chat turns, and the question.
- The answer streams as plain text. The app turns citation markers into tappable chips that open the source section, and removes markers that do not match a provided section.
- Keep chat memory short to protect speed and RAM; drop or summarize older turns.
- Scope selector: this document or the whole library.
- Same grounding rule as everywhere: outside knowledge only after saying the notes do not cover it, and labeled "unverified".

---

## 6. Data model (Room)

| Table | Key columns |
|-------|-------------|
| `document` | id, title, sourceType (photo, pdf, docx, pptx, paste), createdAt |
| `section` | id, documentId, order, title, text, sourceRef |
| `deck` | id, documentId, language, createdAt |
| `card` | id, deckId, sectionId, front, back, origin (`source` or `model_knowledge` or `manual`), edited, createdAt |
| `question` | id, sectionId, stem, choices (JSON), answerIndex, explanation, origin |
| `review_state` | cardId, ease, intervalDays, repetitions, dueAt |
| `review_log` | id, cardId, reviewedAt, grade |
| `gen_cache` | key (sectionId + task + language + promptVersion), status, outputJsonl, updatedAt |
| `test_session` | id, scopeJson, language, startedAt, finishedAt, score |
| `test_item` | id, sessionId, sectionId, type (mcq, fill, short), payloadJson, order |
| `test_answer` | itemId, answerText, aiVerdict, aiScore, aiFeedback, userOverride, answeredAt |
| `chat_thread` | id, scopeJson, language, createdAt |
| `chat_message` | id, threadId, role (user or model), text, citationsJson, createdAt |
| `section_fts` | FTS table over section text and titles |

---

## 7. Generation contracts (JSONL)

Flashcard line:
```json
{"type":"card","front":"Ano ang function ng mitochondria?","back":"Gumagawa ng ATP (energy) para sa cell.","evidence":"Mitochondria produce ATP through cellular respiration."}
```

Multiple-choice question line:
```json
{"type":"mcq","stem":"Which organelle produces most of the cell's ATP?","choices":["Ribosome","Mitochondrion","Golgi body","Lysosome"],"answer":1,"explanation":"Mitochondria carry out cellular respiration.","evidence":"..."}
```

Key point line:
```json
{"type":"point","text":"Mitochondria produce ATP through cellular respiration.","evidence":"..."}
```

Short-answer question line (for interactive tests):
```json
{"type":"short","stem":"Explain how mitochondria produce ATP.","reference_answer":"Through cellular respiration, converting nutrients and oxygen into ATP.","rubric":["mentions cellular respiration","mentions ATP as the product"],"evidence":"..."}
```

Grading result (one JSON object returned for each short answer):
```json
{"verdict":"partial","score":0.5,"feedback":"You named ATP, but you did not mention cellular respiration.","missing":["cellular respiration"]}
```
`verdict` is `correct`, `partial` or `incorrect`; `score` is 0 to 1.

Rules:
- `evidence` must be a short span copied from the section. The app checks it exists in the section text (fuzzy match). No evidence found means the item is flagged `model_knowledge` and labeled "unverified".
- One JSON object per line, no prose, no markdown fences.
- Version the schema and the prompt (`promptVersion`) so cache and evaluation stay consistent.

---

## 8. Prompting rules

- System instruction: role, output language, "use only the provided text", JSONL only, number of items, difficulty.
- Language modes as separate prompt variants: `en`, `taglish`, `fil`, `ceb`. Keep technical terms in English when students normally use them that way (Taglish style).
- Put the schema and one short example in the prompt. Keep prompts small to save tokens and time.
- Ask for a fixed count (for example 5 cards, 3 questions per section).
- For "Explain this": answer only from the section; if the text is not enough, say so before adding outside knowledge, and label it.
- Grading prompt: be strict about facts and lenient about wording. Grade meaning, not language, so answers in Filipino, Taglish or Bisaya are judged the same as English. Return one JSON object only.
- Q&A prompt: sections labeled S1..Sn, cite with [S1] markers, answer from the sections first, say clearly when the notes do not cover the question.
- Feedback and explanations come back in the language the student chose.
- Keep prompts in versioned resource files, not scattered in code.

---

## 9. Performance and device targets (hypotheses to measure in Phase 0)

| Metric | Initial target |
|--------|----------------|
| Model load (cold) | under 15 s on your phone, faster on second load |
| First card visible after tapping Generate | under 30 s on a mid-range phone |
| OCR of one photographed page | under 3 s |
| Grading one short answer | under 10 s (pre-fetching the next question hides most of the wait) |
| Ask Gemce: first words of the answer | under 10 s after sending a question |
| Valid JSONL lines | at least 95% |
| Battery for a 10-minute generation session | measured and shown to user; "only while charging" option available |
| Minimum device | arm64 phone; RAM minimum **TBD** (hypothesis: 6 GB; measure) |
| Minimum Android | whatever `litertlm-android` requires **(verify)**; target SDK 36 |

Test devices: your phone, plus at least one cheaper or older phone borrowed from a classmate. Measure on release builds, not debug.

---

## 10. Privacy, security and Play requirements

- Notes, photos and generated material stay on-device. No analytics or ad SDKs.
- `INTERNET` permission is used only for the model download. Add a test: with airplane mode on, the whole study loop works.
- Provide "delete all my data" and per-document delete. Model file deletion is separate.
- Play requirements: new apps and updates must target Android 16 (API 36) or higher. Handle predictive back and edge-to-edge. Complete the Data safety form honestly (likely "no data collected" if you keep the rules above).
- Disclaimer in the app: generated material can contain mistakes; verify important facts against your textbook or professor.
- Respect copyright: the app processes only what the user imports and does not share it.

---

## 11. Development environment setup (Linux + Antigravity + physical phone)

### 11.1 Check your laptop
- 64-bit Linux, x86_64 CPU (Android Studio does not support ARM-based Linux), glibc 2.31 or newer.
- 8 GB RAM minimum, 16 GB recommended. 8 GB disk minimum for the IDE and SDK; plan **at least 30 GB free** for Gradle caches, project, model files and builds.

### 11.2 Install
1. **Android Studio** (stable channel) from developer.android.com/studio. Extract to `/opt/android-studio`, run `bin/studio`, then *Tools → Create Desktop Entry*. Use it for SDK Manager, Compose previews, Logcat, Layout Inspector and the Profiler, even if you write most code in Antigravity.
2. In the setup wizard / SDK Manager install: **Android SDK Platform 36**, latest **Build-Tools**, **Platform-Tools** (adb), **Command-line Tools**. Skip emulator images unless you want UI tests. The on-device model should be tested on a real phone.
3. **JDK**: Android Studio bundles one (JBR). For builds from the Antigravity terminal either point `JAVA_HOME` at it or install OpenJDK 17.
4. **Git**.
5. **Antigravity** (you already have it). Open the project folder in it.

### 11.3 Environment variables (add to `~/.bashrc` or `~/.zshrc`)
```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export JAVA_HOME="/opt/android-studio/jbr"   # or your OpenJDK 17 path
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin"
```
Restart the terminal (and Antigravity) so its terminal inherits them.

### 11.4 Phone setup
1. Enable *Developer options* (tap Build number 7 times), then *USB debugging*.
2. Connect by USB, accept the RSA prompt, run `adb devices`. If it shows `no permissions`, install udev rules for adb (Debian/Ubuntu: `android-sdk-platform-tools-common`) and replug.
3. Optional wireless debugging (Android 11+): *Wireless debugging → Pair device*, then `adb pair <ip:port>` and `adb connect <ip:port>`.
4. Quick sanity check before writing code: install **Google AI Edge Gallery** from Google Play, load Gemma 4 E2B, and see how fast and heavy it feels on your phone.

### 11.5 Create the project
Android Studio → New Project → **Empty Activity (Compose)** → Kotlin, Kotlin DSL, package name of your choice (for example `com.yourname.gemce`). Set `targetSdk = 36`. Set `minSdk` in Phase 0 after checking the LiteRT-LM requirement. Commit to Git immediately.

### 11.6 Dependencies (starting list; use a version catalog)
- Compose BOM, Material 3, Navigation Compose, lifecycle-viewmodel-compose
- Room (+ KSP), DataStore, WorkManager, Hilt (or Koin), kotlinx-coroutines, kotlinx-serialization-json
- `com.google.ai.edge.litertlm:litertlm-android`
- `com.google.mlkit:text-recognition` (bundled)
- `com.tom-roush:pdfbox-android`
- CameraX (or photo picker), Coil for images

### 11.7 Getting the model onto your phone during development
1. Download `gemma-4-E2B-it.litertlm` from the `litert-community/gemma-4-E2B-it-litert-lm` repo on Hugging Face (accept any license prompt). It is about 2.6 GB. Some builds are tuned for specific chips; start with the generic one.
2. Push and copy into the app's private files directory of a **debug** build:
```bash
adb push gemma-4-E2B-it.litertlm /data/local/tmp/
adb shell run-as <your.package.name> mkdir -p files/models
adb shell run-as <your.package.name> cp /data/local/tmp/gemma-4-E2B-it.litertlm files/models/
```
3. Point `EngineConfig(modelPath = ...)` at `context.filesDir/models/gemma-4-E2B-it.litertlm`. The real downloader (M1) replaces this later.

### 11.8 Daily build loop (Antigravity terminal)
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat | grep -i <your_tag>
```
Note: if Kotlin code intelligence in Antigravity is weak, use Android Studio as the main editor and Antigravity as the agent that edits files, and keep Studio open for previews, Logcat and profiling.

### 11.9 Terminal install cheat sheet (Ubuntu/Debian)

Run these in order. On Fedora use `dnf` and on Arch use `pacman` with the equivalent packages. Gradle, Kotlin and all Android libraries come from the project's Gradle wrapper, so no global install is needed. Node.js and React Native tools are not needed.

```bash
# 1. Base tools (git, downloaders, pipx, phone screen mirroring, adb udev rules)
sudo apt update
sudo apt install -y git curl wget unzip zip pipx scrcpy android-sdk-platform-tools-common
sudo usermod -aG plugdev "$USER"      # log out and back in afterwards
git config --global user.name "Your Name"
git config --global user.email "you@example.com"

# 2. Android Studio: download the Linux .tar.gz from developer.android.com/studio first
sudo tar -xzf ~/Downloads/android-studio-*-linux.tar.gz -C /opt
/opt/android-studio/bin/studio        # run the setup wizard; it installs the SDK in ~/Android/Sdk
# then in Studio: Tools -> Create Desktop Entry

# 3. Environment variables
cat >> ~/.bashrc <<'EOF'
export ANDROID_HOME="$HOME/Android/Sdk"
export JAVA_HOME="/opt/android-studio/jbr"
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin"
EOF
source ~/.bashrc

# 4. SDK packages from the terminal (needs the Command-line Tools installed in Studio's SDK Manager)
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-36"
sdkmanager --list | grep "build-tools;"      # pick the newest version and install it, for example:
sdkmanager "build-tools;36.0.0"

# 5. Verify
java -version
adb version
sdkmanager --list_installed
adb devices                            # phone plugged in, USB debugging on, authorized

# 6. Hugging Face CLI and the model (accept the model license on its Hugging Face page if asked)
pipx ensurepath && source ~/.bashrc
pipx install "huggingface_hub[cli]"
hf auth login                          # older versions: huggingface-cli login
mkdir -p ~/models/gemma4
hf download litert-community/gemma-4-E2B-it-litert-lm gemma-4-E2B-it.litertlm --local-dir ~/models/gemma4

# 7. Put the model on the phone (a debug build of the app must already be installed)
adb push ~/models/gemma4/gemma-4-E2B-it.litertlm /data/local/tmp/
adb shell run-as com.yourname.gemce mkdir -p files/models
adb shell run-as com.yourname.gemce cp /data/local/tmp/gemma-4-E2B-it.litertlm files/models/
```

Optional: try prompts on the laptop before the phone, and mirror the phone screen.

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
uv tool install litert-lm
litert-lm run --from-huggingface-repo=litert-community/gemma-4-E2B-it-litert-lm gemma-4-E2B-it.litertlm
scrcpy                                 # shows and controls the phone from the laptop
```

---

## 12. Roadmap and exit criteria

| Phase | Goal | Exit criteria |
|-------|------|---------------|
| **0. Spike (about 1 week)** | Prove the risky parts on your phone | Model loads and generates in a throwaway app; measured load time, tokens per second, RAM, 10-minute battery use; CPU vs GPU decided; ML Kit OCR tested on 5 real photos; Taglish and Bisaya sample outputs reviewed; minSdk and minimum RAM decided. **Go/no-go gate.** |
| **1. Ingest** | Content in, clean sections out | Photo, PDF, DOCX, PPTX and paste produce editable sections with source refs; scanned PDF fallback works. |
| **2. Generate** | Reliable structured output | JSONL pipeline with streaming, validation, caching, background work; at least 95% valid lines on the golden set; short-answer grading and Ask Gemce retrieval prompts working. |
| **3. Study** | The loop students use daily | Flashcards with SM-2, quiz mode, interactive test with score report, Ask Gemce chat with citations, reviewer view, library and search, manual cards, CSV export. |
| **4. Model delivery and polish** | Real-world installability | Resumable downloader with checksum, import from file, error and low-storage handling, airplane-mode test passes, Play target API requirements met. |
| **5. Pilot** | Learn from real students | 5–10 students use it for two weeks on their own phones; collect problems and what they actually use; fix top issues; then decide on release and v0.2. |

---

## 13. Evaluation plan

Build a **golden set** of 15–20 real inputs: clean typed notes, a phone photo of a textbook page, a blurry photo, a lecture PPTX, a scanned PDF, a table-heavy page, handwriting (expected to fail in MVP).

For each, record:
- OCR/extraction accuracy (manual spot check)
- Valid JSONL rate, duplicates, empty fields
- **Grounding**: share of cards whose `evidence` is found in the source
- **Language quality** per mode (English, Taglish, Filipino, Bisaya), rated by a native speaker or classmate on a 1–5 scale
- Time per section, battery, peak RAM, any crashes
- **Grading agreement**: a classmate writes about 30 short answers (right, partly right, wrong; English, Taglish, Bisaya). Compare the model's verdict with a human's. Hypothesis targets: at least 80% agreement and almost no "correct" verdicts on clearly wrong answers.
- **Q&A grounding**: about 20 questions whose answers are in the notes and about 10 whose answers are not. Check that citations point to the right section and that Gemce says the notes do not cover the unanswerable ones.

Re-run the golden set whenever the prompt version, model build or backend changes.

---

## 14. Risks and mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Model too slow or heavy on common phones | Poor first impression | Phase 0 gate; background processing; streamed cards; cache; "only while charging"; fall back to manual study; consider smaller or quantized builds later |
| 2.6 GB download is a barrier | Low adoption | Wi-Fi download, resume, import from file, offline sharing between classmates (v0.2) |
| Hallucinated or wrong content | Students learn wrong facts | Grounded generation, evidence check, "unverified" labels, editable cards, visible disclaimer |
| Weak Bisaya or Filipino quality | Core promise suffers | Test early; launch Taglish and English first; mark Bisaya experimental; consider fine-tuning later |
| OCR errors on photos, tables, handwriting | Bad input, bad output | Editable text step; guidance for photographing pages; handwriting only as a later feature |
| Storage and memory pressure | Crashes | Free-space checks, idle unload, graceful out-of-memory handling |
| Battery drain | Bad reviews | Process only on charge option; show estimates; cap sections per run |
| Play policy and target API changes | Release blocked | Target API 36; read policies before Phase 4 |
| Model license or redistribution terms | Release blocked | Re-check license and attribution requirements before shipping |
| AI grading is wrong or unfair in tests | Frustration and mistrust | "AI-graded" label, reference answer shown, one-tap override, multiple-choice and fill-in graded by code, track override rate in the pilot |
| Q&A answers go beyond the student's notes | Wrong or invented facts | Required citations, refusal-first prompt, "unverified" label, citations that do not match a provided section are dropped |
| Test or chat sessions feel slow | Students quit | Pre-fetch the next question, stream answers, cap chat memory, keep one engine call at a time |

---

## 15. Open decisions

1. Package name (the app name is Gemce).
2. Minimum RAM and minSdk (Phase 0).
3. CPU vs GPU default backend (Phase 0).
4. Model distribution: self-hosted, Hugging Face link or Play Asset Delivery (Phase 4).
5. Hilt vs Koin.
6. Which languages ship in the MVP (decide after the language quality check).
7. Whether to offer E4B as an optional download later.

---

## 16. Rules to give your coding agent in Antigravity

Put this in the project's rules or instructions file (check Antigravity's docs for the exact file name and location):

```
Project: Gemce, an offline Android study app (Kotlin, Jetpack Compose).
Read PLAN.md before any task and follow it.
- Native Android only. Kotlin, Compose, MVVM, coroutines/Flow.
- The AI model is accessed only through the LlmEngine interface. Provide a FakeEngine for tests and previews.
- The model outputs JSON Lines. The app renders fixed screens. Never generate, download or execute code at runtime.
- No network calls except the model download. No analytics or ad SDKs.
- Do not guess dependency versions; use the version catalog and ask when unsure.
- Work in small steps. After each step, make sure ./gradlew assembleDebug passes.
- Add unit tests for parsers, JSONL validation and SM-2 scheduling.
- Never block the main thread; model loading and generation run in coroutines/WorkManager.
```

---

## 17. References

- Gemma 4 E2B model card and variants: https://huggingface.co/google/gemma-4-E2B
- LiteRT-LM overview: https://developers.google.com/edge/litert-lm/overview
- LiteRT-LM Kotlin/Android guide: https://developers.google.com/edge/litert-lm/android
- Gemma 4 `.litertlm` builds: https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
- Google AI Edge Gallery (reference app): https://github.com/google-ai-edge/gallery
- ML Kit text recognition v2 (Android): https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- PdfBox-Android: https://github.com/TomRoush/PdfBox-Android
- Android Studio install and system requirements: https://developer.android.com/studio/install
- Google Play target API level requirements: https://developer.android.com/google/play/requirements/target-sdk
