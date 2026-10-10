import os
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import PP_ALIGN

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
blank_layout = prs.slide_layouts[6]

# Theme Colors
BG_COLOR = RGBColor(14, 16, 21)        # Deep Charcoal Navy
CARD_BG = RGBColor(24, 27, 36)         # Surface Card
CARD_BORDER = RGBColor(46, 53, 70)     # Card Border
PRIMARY_GREEN = RGBColor(61, 220, 132) # Gemce Mint Green
ACCENT_GREEN_BG = RGBColor(22, 45, 34) # Pill Background
TEXT_WHITE = RGBColor(255, 255, 255)
TEXT_MUTED = RGBColor(165, 175, 190)
TEXT_ACCENT = RGBColor(141, 227, 181)
BLUE_ACCENT = RGBColor(96, 165, 250)

MOCKUPS_DIR = '/tmp/gemce_mockups'
LOGO_PATH = '/home/mel-garcia/AndroidStudioProjects/Gemce/app/src/main/res/drawable/gemce_logo.png'

def set_slide_background(slide):
    bg = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
    bg.fill.solid()
    bg.fill.fore_color.rgb = BG_COLOR
    bg.line.fill.background()
    return bg

def add_header(slide, badge_text, title_text, subtitle_text):
    # Badge
    badge = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(0.45), Inches(3.4), Inches(0.36))
    badge.fill.solid()
    badge.fill.fore_color.rgb = ACCENT_GREEN_BG
    badge.line.color.rgb = PRIMARY_GREEN
    badge.line.width = Pt(1)
    tf_b = badge.text_frame
    tf_b.word_wrap = False
    tf_b.margin_left = tf_b.margin_right = tf_b.margin_top = tf_b.margin_bottom = 0
    p_b = tf_b.paragraphs[0]
    p_b.text = badge_text
    p_b.alignment = PP_ALIGN.CENTER
    p_b.font.size = Pt(10)
    p_b.font.bold = True
    p_b.font.color.rgb = PRIMARY_GREEN
    p_b.font.name = "Arial"

    # Title & Subtitle Box
    tb = slide.shapes.add_textbox(Inches(0.8), Inches(0.88), Inches(11.7), Inches(1.15))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
    
    p1 = tf.paragraphs[0]
    p1.text = title_text
    p1.font.size = Pt(25)
    p1.font.bold = True
    p1.font.color.rgb = TEXT_WHITE
    p1.font.name = "Arial"
    
    p2 = tf.add_paragraph()
    p2.text = subtitle_text
    p2.font.size = Pt(13)
    p2.font.color.rgb = TEXT_MUTED
    p2.font.name = "Arial"
    p2.space_before = Pt(4)

def add_card(slide, left, top, width, height, title, items, border_color=CARD_BORDER, bg_color=CARD_BG, title_color=TEXT_WHITE):
    card = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
    card.fill.solid()
    card.fill.fore_color.rgb = bg_color
    card.line.color.rgb = border_color
    card.line.width = Pt(1)
    
    tf = card.text_frame
    tf.word_wrap = True
    tf.margin_left = Inches(0.25)
    tf.margin_right = Inches(0.25)
    tf.margin_top = Inches(0.22)
    tf.margin_bottom = Inches(0.22)
    
    if title:
        p_title = tf.paragraphs[0]
        p_title.text = title
        p_title.font.size = Pt(15)
        p_title.font.bold = True
        p_title.font.color.rgb = title_color
        p_title.font.name = "Arial"
        p_title.space_after = Pt(8)
    
    first = True if not title else False
    for item in items:
        if first:
            p = tf.paragraphs[0]
            first = False
        else:
            p = tf.add_paragraph()
        p.text = item
        p.font.size = Pt(12)
        p.font.color.rgb = TEXT_MUTED
        p.font.name = "Arial"
        p.space_after = Pt(4)
    return card

# ==========================================
# SLIDE 1: COVER SLIDE
# ==========================================
slide1 = prs.slides.add_slide(blank_layout)
set_slide_background(slide1)

# Brand Logo
if os.path.exists(LOGO_PATH):
    slide1.shapes.add_picture(LOGO_PATH, Inches(0.8), Inches(1.1), Inches(1.2), Inches(1.2))

# Brand Title & Pitch
tb1 = slide1.shapes.add_textbox(Inches(0.8), Inches(2.5), Inches(8.0), Inches(3.8))
tf1 = tb1.text_frame
tf1.word_wrap = True
tf1.margin_left = tf1.margin_right = tf1.margin_top = tf1.margin_bottom = 0

p1 = tf1.paragraphs[0]
p1.text = "Gemce"
p1.font.size = Pt(56)
p1.font.bold = True
p1.font.color.rgb = TEXT_WHITE
p1.font.name = "Arial"

p2 = tf1.add_paragraph()
p2.text = "AI study buddy that works anywhere you go."
p2.font.size = Pt(22)
p2.font.bold = True
p2.font.color.rgb = PRIMARY_GREEN
p2.font.name = "Arial"
p2.space_before = Pt(8)

p3 = tf1.add_paragraph()
p3.text = "The 100% offline, privacy-first Android study platform powered by on-device Google Gemma 4 E2B via LiteRT-LM. Eliminating the connectivity divide in education."
p3.font.size = Pt(14)
p3.font.color.rgb = TEXT_MUTED
p3.font.name = "Arial"
p3.space_before = Pt(14)

# Pill Badges
badges = ["🔒 100% Offline & Private", "⚡ Gemma 4 E2B via LiteRT", "📚 Verbatim Source Citations", "🧠 SuperMemo SM-2"]
bx = Inches(0.8)
by = Inches(5.3)
for b_text in badges:
    b_shape = slide1.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, bx, by, Inches(2.2), Inches(0.42))
    b_shape.fill.solid()
    b_shape.fill.fore_color.rgb = RGBColor(26, 31, 42)
    b_shape.line.color.rgb = RGBColor(50, 60, 80)
    b_shape.line.width = Pt(1)
    tf_b = b_shape.text_frame
    tf_b.word_wrap = False
    p = tf_b.paragraphs[0]
    p.text = b_text
    p.alignment = PP_ALIGN.CENTER
    p.font.size = Pt(10)
    p.font.color.rgb = RGBColor(220, 230, 245)
    p.font.name = "Arial"
    bx += Inches(2.35)

# Slide 1 Mockup
mockup1 = f'{MOCKUPS_DIR}/01_start_screen_framed.png'
if os.path.exists(mockup1):
    slide1.shapes.add_picture(mockup1, Inches(10.2), Inches(0.9), Inches(2.4), Inches(5.7))

# ==========================================
# SLIDE 2: THE PROBLEM
# ==========================================
slide2 = prs.slides.add_slide(blank_layout)
set_slide_background(slide2)
add_header(slide2, "01 · THE PROBLEM & CONTEXT", "Millions of Students Cannot Rely on Cloud AI", "Cloud-based learning platforms collapse the moment connectivity drops.")

cards_data = [
    ("📶 Connectivity & Latency Barriers", [
        "• High data tariffs and spotty mobile reception across rural schools, transit commutes, and dormitories.",
        "• Cloud AI tools (ChatGPT, Claude, Gemini Web) completely fail when data runs out or signal is lost.",
        "• Students are cut off from AI study tools right when they need exam revision the most."
    ]),
    ("💳 Prohibitive Subscription Paywalls", [
        "• $20/month subscription fees are unattainable for mainstream students in developing economies.",
        "• Proprietary paywalls gatekeep high-quality tutoring behind credit cards and recurring payments.",
        "• Educational inequality widens as only connected students access AI assistance."
    ]),
    ("🛡️ Data Privacy & Cloud Surveillance", [
        "• Uploading student lecture notes, essays, and study materials creates unmonitored cloud data harvesting.",
        "• No data sovereignty: student materials are retained on external corporate servers.",
        "• Institutional compliance issues when handling proprietary textbooks or curriculum syllabi."
    ])
]

cx = Inches(0.8)
for title, items in cards_data:
    add_card(slide2, cx, Inches(2.2), Inches(3.7), Inches(4.3), title, items, border_color=CARD_BORDER)
    cx += Inches(3.9)

# Bottom Callout banner
banner2 = slide2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(6.65), Inches(11.5), Inches(0.55))
banner2.fill.solid()
banner2.fill.fore_color.rgb = ACCENT_GREEN_BG
banner2.line.color.rgb = PRIMARY_GREEN
banner2.line.width = Pt(1)
p_ban = banner2.text_frame.paragraphs[0]
p_ban.text = "💡 Fundamental Principle: Quality academic assistance should never be contingent on having a cellular data connection."
p_ban.alignment = PP_ALIGN.CENTER
p_ban.font.size = Pt(12)
p_ban.font.bold = True
p_ban.font.color.rgb = TEXT_ACCENT

# ==========================================
# SLIDE 3: THE SOLUTION & ARCHITECTURE
# ==========================================
slide3 = prs.slides.add_slide(blank_layout)
set_slide_background(slide3)
add_header(slide3, "02 · THE SOLUTION & ARCHITECTURE", "Gemce: On-Device AI Study Architecture", "Sovereign on-device intelligence executing Google's Gemma 4 E2B via LiteRT-LM.")

sol_cards = [
    ("🔒 100% Local Inference & Offline Guarantee", [
        "• Over 2.4 GB of quantized model weights stored permanently in on-device storage.",
        "• Zero outbound network requests during study sessions. No tracking, telemetry, or cloud dependencies."
    ]),
    ("⚡ Google LiteRT-LM GPU Accelerated Engine", [
        "• Direct execution on Android OpenCL GPU backend (Adreno / Mali).",
        "• Sub-second Time-To-First-Token and smooth streaming token generation on modern Android devices."
    ]),
    ("📦 Persistent Weights & Zero-AI Fallback", [
        "• Model file survives app updates, cache clears, and uninstalls.",
        "• Manual flashcards, deck browsing, and SM-2 scheduling remain 100% functional even with zero AI."
    ])
]

cy = Inches(2.2)
for title, items in sol_cards:
    add_card(slide3, Inches(0.8), cy, Inches(8.6), Inches(1.35), title, items)
    cy += Inches(1.5)

mockup3 = f'{MOCKUPS_DIR}/05_settings_engine_framed.png'
if os.path.exists(mockup3):
    slide3.shapes.add_picture(mockup3, Inches(10.0), Inches(1.8), Inches(2.35), Inches(5.2))

# ==========================================
# SLIDE 4: PERSONALIZATION
# ==========================================
slide4 = prs.slides.add_slide(blank_layout)
set_slide_background(slide4)
add_header(slide4, "03 · CURRICULUM CALIBRATION", "Tailored Rigor Across Disciplines & Languages", "Calibrates explanation depth, curriculum context, and vocabulary to the student.")

calib_cards = [
    ("🎓 Dynamic Academic Level Calibration", [
        "• Calibrates from Junior High, Senior High tracks (GAS, HUMSS, ABM, STEM, TVL, Arts) to College majors.",
        "• Aligns technical vocabulary and pedagogical depth to university programs like Accountancy, Nursing, Education, and Engineering."
    ]),
    ("🌐 Multilingual Academic Tutoring", [
        "• Speaks English, Filipino (Tagalog), Taglish, and Binisaya (Cebuano).",
        "• Students understand tough conceptual mechanisms in their natural, conversational tongue."
    ]),
    ("📖 Packaged Reading Typography", [
        "• Bundles packaged TrueType fonts: Calibri (modern), Serif (editorial), and Monospace (STEM).",
        "• System-wide font selection optimizes legibility for dense textbook reading sessions."
    ])
]

cy = Inches(2.2)
for title, items in calib_cards:
    add_card(slide4, Inches(0.8), cy, Inches(8.6), Inches(1.35), title, items)
    cy += Inches(1.5)

mockup4 = f'{MOCKUPS_DIR}/04_profile_dialog_framed.png'
if os.path.exists(mockup4):
    slide4.shapes.add_picture(mockup4, Inches(10.0), Inches(1.8), Inches(2.35), Inches(5.2))

# ==========================================
# SLIDE 5: ADAPTIVE STUDY HUB
# ==========================================
slide5 = prs.slides.add_slide(blank_layout)
set_slide_background(slide5)
add_header(slide5, "04 · STUDY METHODOLOGY", "Adaptive Study Hub & Intelligent Diagnosis", "Transforms raw syllabus topics and lecture notes into goal-oriented learning workflows.")

path_cards = [
    ("⚡ 1. Rapid Recall & Formulas", [
        "• Diagnoses active retention goals.",
        "• Prescribes Spaced Repetition Flashcards powered by SuperMemo SM-2."
    ]),
    ("🎯 2. Exam Practice & Quizzes", [
        "• Diagnoses test preparation goals.",
        "• Prescribes instant Multiple-Choice Questions with rationale feedback."
    ]),
    ("🧠 3. Deep Problem Solving", [
        "• Diagnoses deep conceptual synthesis.",
        "• Prescribes short-answer essay questions evaluated against rubrics."
    ]),
    ("📖 4. Comprehensive Reviewer", [
        "• Diagnoses structured pre-exam cramming.",
        "• Prescribes high-yield bulleted concept summaries with source quotes."
    ])
]

cx = Inches(0.8)
cy = Inches(2.2)
for i, (title, items) in enumerate(path_cards):
    rx = Inches(0.8) if (i % 2 == 0) else Inches(5.1)
    ry = Inches(2.2) if (i < 2) else Inches(4.3)
    add_card(slide5, rx, ry, Inches(4.1), Inches(1.9), title, items)

mockup5 = f'{MOCKUPS_DIR}/08_hub_earth_selected_framed.png'
if os.path.exists(mockup5):
    slide5.shapes.add_picture(mockup5, Inches(10.0), Inches(1.8), Inches(2.35), Inches(5.2))

# ==========================================
# SLIDE 6: SPACED REPETITION FLASHCARDS
# ==========================================
slide6 = prs.slides.add_slide(blank_layout)
set_slide_background(slide6)
add_header(slide6, "05 · ACTIVE RECALL & RETENTION", "Evidence-Grounded Flashcards with SuperMemo SM-2", "Combines the gold-standard spaced repetition algorithm with zero-hallucination provenance.")

flash_cards = [
    ("🧠 SuperMemo SM-2 Spaced Repetition", [
        "• Automatically calculates ease factor (EF) and repetition intervals (I_n).",
        "• Self-evaluations: Again (1d), Hard (3d), Good (4d), Easy (6d) optimize permanent retention."
    ]),
    ("🚫 Strict Negative Prompt Constraints", [
        "• Explicitly forbids generic meta-questions like 'What was the user intended to study?'.",
        "• Rigorously tests concrete academic terms: Mitochondria, ATP synthesis, cellular respiration."
    ]),
    ("📖 Verbatim Evidence & Source Grounding", [
        "• Card back highlights verbatim source sentences and section provenance tags.",
        "• Guarantees 100% accurate studying without fabricated AI hallucinations."
    ])
]

cy = Inches(2.2)
for title, items in flash_cards:
    add_card(slide6, Inches(0.8), cy, Inches(6.8), Inches(1.35), title, items)
    cy += Inches(1.5)

# Two mockups: Question & Answer
mockup_q = f'{MOCKUPS_DIR}/09_flashcard_question_framed.png'
mockup_a = f'{MOCKUPS_DIR}/10_flashcard_answer_framed.png'
if os.path.exists(mockup_q):
    slide6.shapes.add_picture(mockup_q, Inches(8.0), Inches(1.8), Inches(2.3), Inches(5.2))
if os.path.exists(mockup_a):
    slide6.shapes.add_picture(mockup_a, Inches(10.5), Inches(1.8), Inches(2.3), Inches(5.2))

# ==========================================
# SLIDE 7: REVIEWERS & PRACTICE QUIZZES
# ==========================================
slide7 = prs.slides.add_slide(blank_layout)
set_slide_background(slide7)
add_header(slide7, "06 · EXAM PREPARATION", "High-Yield Reviewers & Verified Practice Quizzes", "Synthesizes complex notes into structured study sheets and scored assessments.")

rev_cards = [
    ("📝 High-Yield Topic Synthesis", [
        "• Organizes dense textbook chapters into bulleted key principles, classifications, and definitions.",
        "• Ideal for rapid revision before major prelim, midterm, and board examinations."
    ]),
    ("🏷️ Provenance & Verifiability Badges", [
        "• Cites exact source sentences for every generated key concept.",
        "• External facts added by the model are visibly marked [unverified] for complete transparency."
    ]),
    ("🎯 Multiple-Choice Exam Practice", [
        "• Generates rigorous 4-option MCQs testing conceptual understanding.",
        "• Instant grading with detailed explanations on why distractor options are incorrect."
    ])
]

cy = Inches(2.2)
for title, items in rev_cards:
    add_card(slide7, Inches(0.8), cy, Inches(6.8), Inches(1.35), title, items)
    cy += Inches(1.5)

# Two mockups: MCQ Quiz Player & Synthesized Reviewer
mockup_quiz = f'{MOCKUPS_DIR}/12_mcq_quiz_framed.png'
mockup_rev = f'{MOCKUPS_DIR}/12_reviewer_active_framed.png'
if os.path.exists(mockup_quiz):
    slide7.shapes.add_picture(mockup_quiz, Inches(8.0), Inches(1.8), Inches(2.3), Inches(5.2))
if os.path.exists(mockup_rev):
    slide7.shapes.add_picture(mockup_rev, Inches(10.5), Inches(1.8), Inches(2.3), Inches(5.2))

# ==========================================
# SLIDE 8: ASK GEMCE ACADEMIC TUTOR
# ==========================================
slide8 = prs.slides.add_slide(blank_layout)
set_slide_background(slide8)
add_header(slide8, "07 · INTERACTIVE TUTORING", "Real-Time Conceptual Q&A in Airplane Mode", "Interactive academic mentor explaining tough mechanisms whenever curiosity strikes.")

chat_cards = [
    ("🌍 Universal Multidisciplinary Grounding", [
        "• Mentors across Natural Sciences, Discrete Mathematics, World History, Rizal, Law, and Medicine.",
        "• Resolves complex queries: e.g. rock cycle mechanisms, truth tables, and historical contexts."
    ]),
    ("📑 Rich Markdown & Code Formatting", [
        "• Native Jetpack Compose Markdown rendering with formatted tables, lists, and KaTeX math formulas.",
        "• Clean visual hierarchy avoids walls of unstructured text."
    ]),
    ("💾 Multi-Session Local Memory & Tabs", [
        "• Conversation history persisted securely on device with quick topic tabs.",
        "• Zero cloud leakage: student inquiries never leave device memory."
    ])
]

cy = Inches(2.2)
for title, items in chat_cards:
    add_card(slide8, Inches(0.8), cy, Inches(8.6), Inches(1.35), title, items)
    cy += Inches(1.5)

mockup8 = f'{MOCKUPS_DIR}/14_chat_response_framed.png'
if os.path.exists(mockup8):
    slide8.shapes.add_picture(mockup8, Inches(10.0), Inches(1.8), Inches(2.35), Inches(5.2))

# ==========================================
# SLIDE 9: TECHNICAL ARCHITECTURE
# ==========================================
slide9 = prs.slides.add_slide(blank_layout)
set_slide_background(slide9)
add_header(slide9, "08 · ENGINEERING & TECH STACK", "Engineered for Native Android Performance", "Clean MVVM architecture built with modern Kotlin and on-device machine learning.")

tech_cards = [
    ("🎨 UI & Presentation Layer", [
        "• 100% Jetpack Compose + Material 3.",
        "• Dark / Light / System theme switching.",
        "• Edge-to-edge support across Android 14+.",
        "• Integrated TrueType fonts (Calibri, Serif, Mono)."
    ]),
    ("🤖 On-Device AI Engine", [
        "• Google LiteRT-LM runtime engine.",
        "• Quantized Gemma 4 E2B weights.",
        "• OpenCL / Adreno GPU acceleration.",
        "• Decoupled LlmEngine with test FakeEngine."
    ]),
    ("📥 Document Ingestion Pipeline", [
        "• Offline Google ML Kit Text Recognition v2.",
        "• Pure local PDF extraction via pdfbox-android.",
        "• Normalizes photos, PDF, DOCX, and text paste.",
        "• Clean section parsing with verbatim linking."
    ]),
    ("💾 Local Persistence & Reliability", [
        "• Android Room Database + SharedPreferences.",
        "• WorkManager for background model tasks.",
        "• Model stored in persistent app storage.",
        "• Zero network calls beyond weight download."
    ])
]

coords = [
    (Inches(0.8), Inches(2.2)),
    (Inches(6.8), Inches(2.2)),
    (Inches(0.8), Inches(4.5)),
    (Inches(6.8), Inches(4.5)),
]

for (title, items), (rx, ry) in zip(tech_cards, coords):
    add_card(slide9, rx, ry, Inches(5.7), Inches(2.05), title, items)

# ==========================================
# SLIDE 10: ROADMAP & CONCLUSION
# ==========================================
slide10 = prs.slides.add_slide(blank_layout)
set_slide_background(slide10)
add_header(slide10, "09 · ROADMAP & CALL TO ACTION", "Democratizing AI for Every Student Everywhere", "Gemce is open-source, fully verified on physical hardware, and ready for deployment.")

close_cards = [
    ("✅ Shipped MVP Milestones (v0.2.2)", [
        "• Full compliance with PLAN.md architecture: OCR/PDF ingest, SM-2 flashcards, MCQs, Reviewers, and Ask Gemce chat.",
        "• Verified on Android 14 (OPPO Reno12 F 5G) with Google Gemma 4 E2B model weights."
    ]),
    ("🚀 Immediate Roadmap (v0.3)", [
        "• Anki Deck (.apkg) bidirectional export & import.",
        "• Audio flashcard review mode (hands-free offline playback).",
        "• Camera instant reviewer scanner with camera auto-cropping."
    ]),
    ("📲 Open Source & Distribution", [
        "• GitHub Repository: github.com/meloervy/appbuild.gemce",
        "• Installable APK: Packaged via GitHub Actions CI/CD (Gemce-v0.2.2.apk)."
    ])
]

cy = Inches(2.2)
for title, items in close_cards:
    add_card(slide10, Inches(0.8), cy, Inches(8.6), Inches(1.35), title, items)
    cy += Inches(1.5)

mockup10 = f'{MOCKUPS_DIR}/06_settings_about_framed.png'
if os.path.exists(mockup10):
    slide10.shapes.add_picture(mockup10, Inches(10.0), Inches(1.8), Inches(2.35), Inches(5.2))

# Save presentation
out_pptx = '/home/mel-garcia/AndroidStudioProjects/Gemce/docs/presentation/Gemce_Pitch_Presentation.pptx'
os.makedirs('/home/mel-garcia/AndroidStudioProjects/Gemce/docs/presentation', exist_ok=True)
prs.save(out_pptx)
prs.save('/home/mel-garcia/AndroidStudioProjects/Gemce/Gemce_Pitch_Presentation.pptx')
print(f'Presentation saved successfully to {out_pptx}')
