# Edu Report (माझी शाळा)

Edu Report is a premium, comprehensive Android application tailored for educators to manage student records, track attendance, perform academic grading, input descriptive remarks, and instantly generate official CCE-compliant report card PDFs. The app features localized support in both **English and Marathi**, seamless Firebase cloud synchronization, and offline-first performance caching.

1)
""make it user click and hold existing student then it select and user want only this selected student export and do other thing only show this thing swhen student selected"""

---

## 📖 App Descriptions

### Short Description
*Edu Report is a bilingual (English & Marathi) Android app designed for teachers to efficiently manage class records, take attendance, perform CCE-compliant grading, and generate printable student progress report PDFs with offline-first caching and OCR scanning support.*

### Long Description
*Edu Report serves as an all-in-one digital assistant for primary and secondary school educators, specifically aligned with the Continuous and Comprehensive Evaluation (CCE) pattern of grading. Built to eliminate the burden of manual ledger calculations, the app automates class management by keeping track of student rosters, daily attendance metrics, and formative/summative marks.*

*Teachers can record grading marks across specific academic categories (like Oral, Practical, Project, and Written) and let the built-in grading engine automatically compute percentage ratios and standard alphabetical grades (A-1, A-2, B-1, etc.). In addition to numeric grades, the app provides a structured interface for recording descriptive remarks (strengths, interests, and areas of improvement) to provide holistic student feedback. All reports are compiled locally into print-ready, bilingual PDFs using a custom layout manager. To support varying network conditions in remote areas, Edu Report operates on a dual-layer caching strategy that stores records locally for instant zero-latency loads and synchronizes changes to a Firestore database in the background when connectivity is available.*

---

## 👥 Target Users & Use Cases

* **Primary & Secondary School Teachers**: Automate the computation of formative and summative marks and cut down report card processing time from weeks to minutes.
* **School Administrators**: Maintain centralized, digital rosters of classes, divisions, subjects, and teacher assignments.
* **Bilingual Educators (Maharashtra/India)**: Switch instantly between English and Marathi interfaces to match school instruction mediums (semi-english, Marathi-medium, or English-medium schools).

---

## 📊 App Pages & Components Directory

Edu Report comprises a total of **36 distinct screens and overlay panels** (14 Activities, 14 Fragments, and 8 Dialog/Bottom Sheets). Below is the complete page directory:

### 📱 1. Core Activities (14 Screens)
| Screen Name | Description |
| :--- | :--- |
| **Splash Screen** | Clean entry animation displaying the school brand. |
| **Login Screen** | Secure credentials entry via Firebase Auth (with Google Sign-In toggle). |
| **Register Screen** | Multi-field educator registration (name, phone, email, password). |
| **School Register** | Initial setup form for school name, UDISE code, board, address, and logo. |
| **Home Activity** | Navigation drawer and bottom-bar coordinator hosting active fragments. |
| **Student Profile** | Displays individual student details, attendance statistics, and reports. |
| **Student Register** | Add new student screen with details like roll number, gender, and category. |
| **Student Editor** | Editor to modify existing student demographics and parent details. |
| **Enter Marks** | Main marks matrix input grid allowing numeric entry for all subjects of a student. |
| **Marksheet Activity** | Summarized academic scorecard preview. |
| **Promote Students** | Batch tool to shift students from one standard/division to the next academic year. |
| **Class Setup** | Create and configure class metadata. |
| **Subject Update** | Add, edit, and reorganize subjects assigned to a class. |
| **Subscription History** | Chronological log of subscription renewal dates and statuses. |

---

### 🧩 2. Core Fragments (14 Screens)
| Fragment Name | Description |
| :--- | :--- |
| **Stats Dashboard** | Analytical dashboard showing total classes, active students, and overall entry completion rates. |
| **Extra Dashboard** | Shortcuts grid to settings, databases, default school configurations, and utilities. |
| **Class & Division List**| Renders active standards and divisions with quick-access buttons. |
| **Student List** | Searchable, paginated registry of all students enrolled in a division. |
| **Subjects Manager** | Allows teachers to activate or customize specific subjects for their division. |
| **Weightage Manager** | Declarative parameters config screen (maximum formative & summative scores per subject). |
| **Formative/Summative** | Core grading page containing student rows, sub-mark grades, and nested subject blocks. |
| **Descriptive Entries** | Portal to assign qualitative feedback cards (interests, strengths, areas of work). |
| **Attendance Summary** | Overall attendance calendars, summaries, and monthly statistics. |
| **Attendance Roll Call** | Grid of checkbox roll calls to log daily class attendance. |
| **Report Printing Hub** | Selection manager to compile and batch-print progress cards to PDF files. |
| **Print Preferences** | Form to customize signature labels, PDF borders, logo positioning, and font scaling. |
| **Profile Settings** | Details of the logged-in teacher and basic app synchronization triggers. |
| **Preferences & System**| Toggle language (English/Marathi), clear system caches, and review app preferences. |

---

### 💬 3. Dialogs & Bottom Sheets (8 Overlays)
| Overlay Name | Description |
| :--- | :--- |
| **Subscription Sheet** | Pop-up containing payment instructions and verification screenshot uploader. |
| **Single Subject Marks** | Quick-input dialog allowing teachers to edit marks for one subject without opening the full editor. |
| **Attendance Picker** | Floating calendar-based panel to edit attendance for individual dates. |
| **Categorized Remarks** | Floating window containing ready-to-select remark templates grouped by subject. |
| **Custom Page Help** | Contextual guidelines and FAQs depending on the active page. |
| **PDF Viewer** | Seamless previewer showing generated PDF layouts before sending to print. |
| **QR Previewer** | Renders QR code payloads for student verification. |
| **Report Settings** | Quick preferences for toggling marks or percentages on printed PDF reports. |

---

## ⚙️ Technical Core Engines

### 📈 1. CCE Automatic Grading System
Calculations are processed dynamically via the `GradeCalculator` utility class. The engine maps total accumulated marks to academic letter grades:
* **A-1 (अ-१)**: 91% to 100%
* **A-2 (अ-२)**: 81% to 90%
* **B-1 (ब-१)**: 71% to 80%
* **B-2 (ब-२)**: 61% to 70%
* **C-1 (क-१)**: 51% to 60%
* **C-2 (क-२)**: 41% to 50%
* **D (ड)**: 33% to 40%
* **E-1/E-2 (इ)**: Below 33% (Needs Improvement)

### 📸 2. OCR Scan-to-Mark Engine
Powered by **CameraX** (for real-time device camera feeds) and **Google ML Kit Text Recognition**:
* Scans physical paper marksheets.
* Filters and aligns rows using a boundary rectangle (`scanner_frame.xml`).
* Sanitizes numerical data out of raw OCR string grids.
* Auto-populates score fields directly inside target student adapters, reducing manual data entry work.

### 💾 3. Hybrid Caching Architecture
Provides a local session cache (`AppCache`) for zero-latency screen transitions.
* **Instant Renders**: When a class is opened, `AppCache` values are rendered in under `200ms`.
* **Parallel Fetching**: The app pulls the latest cloud records from Firebase Firestore in the background.
* **Smart Synchronization**: If the background fetch contains newer timestamp edits (`updatedAt`), the local cache is patched dynamically; otherwise, local modifications are synchronized back to Cloud Firestore.

### 🌐 4. Runtime Bilingual Engine
Localized string maps reside inside `values/strings.xml` and `values-mr/strings.xml` (Marathi).
* **Bilingual Report Cards**: The custom PDF generation engine translates report card metadata and subjects dynamically using `PdfLocalizer`.
* **Zero-Reboot Locale Changes**: Applying a new locale changes language layouts instantly at runtime using `AppCompatDelegate.setApplicationLocales()`, accompanied by a smooth UI transition.

---

## 🛠️ Tech Stack & Dependencies
* **Core Language**: Java 17
* **UI Pattern**: MVVM/Repository pattern, ViewBinding, Navigation Components
* **Image Loading**: Glide
* **Camera & AI**: CameraX Core, CameraX Lifecycle, ML Kit Text Recognition
* **Cloud Service**: Firebase Auth, Firebase Firestore, Firebase Storage
* **PDF Builder**: iText PDF Engine

---

## 🚀 How to Build & Run
1. Clone this repository in Android Studio.
2. Place a valid `google-services.json` file inside the `app/` directory (configured for Firebase Authentication and Cloud Firestore).
3. Synchronize the project with Gradle.
4. Clean and run the project on a device/emulator running **Android 7.0 (API 24) or higher** (Targeting Android 15, API 35).
