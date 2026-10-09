# Project: Gemce

Project: Gemce, an offline Android study app (Kotlin, Jetpack Compose).
Read [PLAN.md](file:///home/mel-garcia/AndroidStudioProjects/Gemce/PLAN.md) before any task and follow it.

## Architectural & Coding Guidelines
- Native Android only. Kotlin, Jetpack Compose, MVVM, coroutines/Flow.
- The AI model is accessed only through the `LlmEngine` interface. Provide a `FakeEngine` for tests and previews.
- The model outputs JSON Lines (JSONL). The app renders fixed screens. Never generate, download or execute code at runtime.
- No network calls except the model download. No analytics or ad SDKs.
- Ground everything in the source text. Every card and question links back to the section it came from. Anything added by the model is marked unverified.
- Do not guess dependency versions; use Gradle version catalog (`gradle/libs.versions.toml`) and ask when unsure.
- Work in small steps. After each step, make sure `./gradlew assembleDebug` passes.
- Add unit tests for parsers, JSONL validation and SM-2 scheduling.
- Never block the main thread; model loading and generation run in coroutines / WorkManager.
