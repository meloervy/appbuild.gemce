# Gemce 📚✨

> **AI study buddy that works anywhere you go.**  
> 100% Offline · Private by Default · Powered by on-device Gemma 4 E2B via LiteRT-LM

[![Android](https://img.shields.io/badge/Platform-Android_14+_(API_26–35)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![LiteRT-LM](https://img.shields.io/badge/On--Device_LLM-LiteRT--LM_Gemma_4_E2B-FF6F00?logo=google&logoColor=white)](https://ai.google.dev/edge)
[![Offline](https://img.shields.io/badge/Network-100%25_Offline-success)](#privacy--offline-guarantee)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/meloervy/appbuild.gemce?color=orange&logo=github)](https://github.com/meloervy/appbuild.gemce/releases)

### 📲 Direct APK Installation
Download the ready-to-install Android APK directly from GitHub:
- 🚀 **[Download Latest Gemce APK](https://github.com/meloervy/appbuild.gemce/releases)**
- Every commit to `main` also automatically generates a fresh installable APK under GitHub **[Actions Artifacts](https://github.com/meloervy/appbuild.gemce/actions)**.

---

## 📖 Overview

In many schools and review centers across the Philippines and worldwide, cellular reception is weak or non-existent, and mobile data is prohibitively expensive. When students need rapid exam review, concept clarification, or interactive practice, cloud-based AI tools simply fail to connect.

**Gemce** solves this by running an entire state-of-the-art Large Language Model (**Gemma 4 E2B**) directly on the student's Android smartphone. 

Students import their own syllabus, lecture slides, textbooks, or handwritten review sheets (via camera OCR, PDF, DOCX, PPTX, Markdown, or text paste). Gemce normalizes the text into structured sections and transforms them into:
- ⚡ **Flashcards** with mathematically sound **SM-2 Spaced Repetition**.
- 🎯 **Multiple-Choice Quizzes (MCQ)** with instant scoring and rationales.
- 🧠 **Interactive Tests** with AI-evaluated short answers and student grade overrides.
- 📖 **High-Yield Concept Reviewers** with verbatim citation evidence.
- 💬 **Ask Gemce & Contextual Chat** with multi-session memory, section citations, and native Markdown rendering.
- 🌐 **Multilingual Explanations** in **English**, **Filipino (Tagalog)**, **Taglish**, and **Binisaya (Cebuano)**.

---

## 🎯 Design Principles

1. **The Model is the Language Layer, Not the Source of Truth:**  
   The model summarizes, rewrites, quizzes, and explains the student's *own* notes. It does not hallucinate facts.
2. **Verbatim Grounding & Provenance:**  
   Every generated card, quiz, and test question is strictly bound to its source section with explicit quote evidence. Outside facts added by the model are visibly tagged `[unverified]`.
3. **Structured Outputs, Fixed UI:**  
   The model emits **JSON Lines (JSONL)** contracts parsed at runtime. The app renders fixed, battle-tested Compose screens. Gemce never downloads, generates, or evaluates executable code at runtime.
4. **Study Loop Works with Zero AI:**  
   Manual flashcard creation, deck browsing, SM-2 scheduling, and Anki CSV exports remain 100% functional even without model weights installed.
5. **Privacy by Default:**  
   Notes, questions, and responses never leave the device. No analytics, tracking SDKs, or ad networks are bundled.
6. **Persistent Weights:**  
   Downloaded or sideloaded model weights (`.litertlm`) are stored in persistent storage surviving app updates, data resets, and uninstalls.

---

## 🚀 MVP Features Matrix (PLAN.md v0.2 Compliance)

| ID | Feature | Status | Implementation Details |
|:---|:---|:---:|:---|
| **M1** | **Model Setup** | ✅ Completed | Resume-capable HTTP range downloader, SHA-256 validation, persistent shared storage (`/sdcard/Download/Gemce/models/`), file locator for sideloaded `.litertlm` models. |
| **M2** | **Camera & Gallery OCR** | ✅ Completed | Offline Google ML Kit Text Recognition v2 (`com.google.mlkit:text-recognition`). |
| **M3** | **PDF Ingestion** | ✅ Completed | Pure offline extraction via `pdfbox-android` with fallback OCR rendering. |
| **M4** | **DOCX & PPTX Ingestion** | ✅ Completed | Lightweight streaming `java.util.zip` + `XmlPullParser` extracting `word/document.xml` and `ppt/slides/*.xml` without heavy Apache POI. |
| **M5** | **Text Paste** | ✅ Completed | Instant plain text pasting for syllabi and lecture outlines. |
| **M6** | **Review & Edit Text** | ✅ Completed | Editable text preview before chunking and generation. |
| **M7** | **Section Material Generation** | ✅ Completed | Flashcards, MCQs, short answers, and reviewer summaries generated per section with prompt calibration for English, Filipino, Taglish, and Bisaya. |
| **M8** | **Flashcard Study Mode** | ✅ Completed | 3D flippable card animation with SuperMemo **SM-2** algorithm (Again, Hard, Good, Easy recall ratings). |
| **M9** | **MCQ Quiz Mode** | ✅ Completed | 4-choice questions, instant score calculation, per-question explanations, and source links. |
| **M10** | **Reviewer View** | ✅ Completed | Bulleted high-yield concepts with verbatim quote evidence. |
| **M11** | **"Explain This"** | ✅ Completed | Grounded, deep-dive modal explanation using intuitive real-world analogies in the chosen language. |
| **M12** | **Academic Library** | ✅ Completed | Folder organization, instant search across titles and note contents, AI Context toggle switches, and note reader dialog. |
| **M13** | **Settings & Preferences** | ✅ Completed | Theme switcher (System, Light, Dark), font preferences (Calibri, Serif, Monospace), "Process only while charging" toggle, storage usage breakdown, and clear all data. |
| **M14** | **Manual Card Creation** | ✅ Completed | Direct card creation (+ Add Card) for studying with zero AI dependency. |
| **M15** | **Anki Export** | ✅ Completed | One-tap Anki-compatible TSV/CSV deck export with tags, clipboard copy, and Android share sheet. |
| **M16** | **Interactive Test** | ✅ Completed | Short-answer tests evaluated against reference rubrics with AI scoring, student overrides ("I was right"), and weak topic analysis. |
| **M17** | **Ask Gemce (Q&A)** | ✅ Completed | Multi-session chat history, note retrieval with `[S1]` citations, topic-agnostic tutoring (System Design, STEM, etc.), and native Markdown formatting. |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ UI Layer (Jetpack Compose, Single Activity, Material 3)     │
│  Study Hub · Interactive Test · Library · Chat · Settings   │
└──────────────────────────────▲──────────────────────────────┘
                               │ StateFlow / Events (MVVM)
┌──────────────────────────────┴──────────────────────────────┐
│ Domain Layer (Pure Kotlin Coroutines & Flow)                │
│  Chunking · SM-2 Scheduler · Anki Exporter · Prompt Builder │
└───────────────▲──────────────▲──────────────────▲───────────┘
                │              │                  │
┌───────────────┴──────┐ ┌─────┴─────────┐ ┌──────┴───────────┐
│ Ingestion Pipeline   │ │ AI Inference  │ │ Local Data       │
│ • ML Kit OCR         │ │ • LlmEngine   │ │ • SharedPreferences│
│ • PdfBox-Android     │ │ • LiteRtLm    │ │ • JSON Storage   │
│ • DOCX/PPTX XML      │ │ • FakeEngine  │ │ • Packaged Fonts │
└──────────────────────┘ └───────────────┘ └──────────────────┘
```

### Clean AI Abstraction: `LlmEngine`
To guarantee rock-solid testing and fluid previews without requiring a 2.6 GB model during development, all AI features communicate through the `LlmEngine` interface:
- **`LiteRtLmEngine`**: Production backend wrapping Google's LiteRT-LM Kotlin runtime for Gemma 4 E2B.
- **`FakeEngine`**: Deterministic preview backend returning pre-validated JSON Lines and structured responses for automated tests and previewing.

---

## 🔤 Packaged TrueType Fonts

Gemce bundles 6 TrueType font files directly in `res/font/`, ensuring zero external network font fetching:
- **Calibri** (`calibri_regular.ttf` & `calibri_bold.ttf`): Modern, highly readable study typography (Carlito).
- **Serif** (`serif_regular.ttf` & `serif_bold.ttf`): Classical editorial typeface optimized for dense academic reading (Liberation Serif).
- **Monospace** (`monospace_regular.ttf` & `monospace_bold.ttf`): Fixed-width typography for Computer Science, programming, and STEM subjects (JetBrains Mono).

Font preferences are applied app-wide across all study cards, quiz stems, reviewer notes, and chat bubbles, complete with a live font preview in Settings.

---

## 📝 Native Markdown Rendering

Chat bubbles and note previews render full GitHub Flavored Markdown natively without third-party dependencies:
- **Fenced Code Blocks**: Bordered code cards with language badges (`KOTLIN`, `PYTHON`, `JAVA`), monospace font, horizontal scrolling, and a **"📋 Copy" / "✓ Copied"** clipboard button.
- **Headings**: `# H1` through `#### H4` with scaled typography.
- **Inline Spans**: **Bold** (`**` / `__`), *Italics* (`*` / `_`), ***Bold Italics*** (`***`), and `` `inline code` `` pills.
- **Lists**: Bullet lists (`-`, `*`, `+`) and numbered lists (`1.`, `2.`).
- **Blockquotes**: Bordered accent bars with italic formatting.
- **Grid Tables**: Markdown tables (`| Col 1 | Col 2 |`) with formatted headers and scrollable cells.
- **Academic Citations**: Highlighted citation chips for `[S1]`, `[S2]`, and `[unverified]`.

---

## 📥 Model Setup & Installation

Gemce is calibrated for **Gemma 4 E2B** formatted for LiteRT-LM (`.litertlm`).

### Option 1: In-App Download (Wi-Fi)
1. Open Gemce and navigate to **Settings** (`⚙️`).
2. Under **Gemma 4 E2B AI Model Engine**, tap **Download (Wi-Fi)**.
3. The app downloads the weights directly from Hugging Face (`litert-community/gemma-4-E2B-it-litert-lm`) with progress tracking and automatic SHA-256 verification.

### Option 2: Offline Sideloading via ADB (No Internet Needed)
If you already have `gemma-4-E2B-it.litertlm` on your computer:
```bash
# Push directly to Gemce's persistent storage directory
adb shell mkdir -p /sdcard/Download/Gemce/models
adb push gemma-4-E2B-it.litertlm /sdcard/Download/Gemce/models/
```
Once pushed, Gemce detects the model immediately on launch!

---

## 🛠️ Building & Developing

### Prerequisites
- **Android Studio**: Ladybug / Meerkat (2024.2+) or newer
- **JDK**: Java 17 or Java 21 (Android Studio bundled JBR recommended)
- **Android SDK**: API 34+ (Target SDK: 35, Min SDK: 26)
- **Gradle**: 8.9+ (Gradle wrapper included)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/meloervy/appbuild.gemce.git
cd appbuild.gemce

# Set Java home to Android Studio JBR
export JAVA_HOME=/opt/android-studio/jbr

# Run all unit tests
./gradlew test

# Assemble installable Debug APK
./gradlew assembleDebug

# Install directly on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Privacy & Offline Guarantee

- **Zero Cloud Calls:** Notes, flashcards, questions, and test scores stay 100% on your device.
- **No User Accounts:** No sign-up, login, or personal information required.
- **No Tracking SDKs:** No Google Analytics, Firebase Crashlytics, Facebook SDK, or ad networks.
- **Network Permission Scope:** The network permission is strictly utilized for the optional one-time user-initiated model download.

---

## 📄 License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.
