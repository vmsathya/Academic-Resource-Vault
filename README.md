# 📚 Academic Resource Vault

A Java-based centralized portal for organizing, managing, and accessing college study materials in one place.

---

## 1. Project Title

**Academic Resource Vault**

---

## 2. Project Overview

Academic Resource Vault is a centralized digital portal that organizes college study materials in one place. It eliminates the need for students to search through messy WhatsApp chats, scattered personal files, or expired Google Drive links before examinations.

College staff and faculty admins can upload and manage verified academic resources, while students can easily search, view, preview, and download the materials they need.

**Built with:** Java 11 (JDBC) | Database: SQLite (via `org.sqlite.JDBC`) / Standard SQL RDBMS | Version: 1.0.0

---

## 3. Problem Statement

Before university examinations, students often face the following critical problems:

- Study materials are scattered across WhatsApp groups, personal devices, and temporary cloud drives.
- Google Drive links expire, reach download quotas, or get deleted.
- Previous Year Question Papers (PYQs) and official answer keys are difficult to find when needed most.
- There is no standardized way to search resources by subject code or semester (Odd/Even).
- Faculty and staff have no single authentic place to publish and maintain authentic, verified material.

There is a direct need for a **single, reliable, organized platform** for academic resources.

---

## 4. Objectives

- Provide one centralized repository for all college academic study materials.
- Allow staff/admins to upload, update, and delete resources securely.
- Organize materials by semester (Odd/Even, Semesters 1 through 8), subject code, and resource category.
- Let students quickly search, preview in-browser, and download verified resources.
- Reduce dependency on informal, unreliable sharing channels.
- Offer a modern, responsive, ultra-professional UI interface with dark/light themes.

---

## 5. Features

- **Role-Based Authentication:** Student and Staff/Faculty Admin login with session management.
- **Upload & Resource Management:** Staff/admins can upload files (PDFs, Notes, Question Banks, Answer Keys) with metadata tagging.
- **Odd / Even Semester Support:** Instant filtering for Odd (1, 3, 5, 7) and Even (2, 4, 6, 8) semesters.
- **Subject-Wise Organization:** Standardized subject codes (e.g., `CS301`, `CS302`, `CS401`, `CS402`, `CS501`, `CS601`).
- **Previous Year Question Papers (PYQs):** Complete exam papers organized with Part-A and Part-B sections.
- **Module Question Banks:** Bloom's taxonomy aligned question sets for exam preparation.
- **Curated Lecture Notes:** Syllabus-aligned study guides, derivations, and pseudocode.
- **Model Answer Keys:** Step-by-step marking rubrics and solutions.
- **Search as You Type:** Instant search across subject code, subject title, topics, and descriptions.
- **In-Browser Document Previewer:** View full document contents, copy text, or print directly inside the portal.
- **Direct Downloads:** Real-time file download handling with live download counters.
- **Student Exam Bookmarks:** Students can star and bookmark critical resources for quick access during revision.
- **Quick Demo Access:** 1-click test login buttons for testing Student and Staff roles instantly.
- **Dark / Light Theme Toggle:** Beautiful modern UI with glassmorphism, responsive cards, and table views.

---

## 6. User Roles

### 👨‍🎓 Students
- Log in to the student portal (or test with demo student account).
- Search resources by subject code, subject name, or keywords.
- Filter by semester (Odd/Even, Sem 1–8) and resource category (PYQs, Notes, Question Banks, Answer Keys).
- Preview verified study documents directly in the browser.
- Download files to local device (increments real-time counter).
- Bookmark key revision materials for quick access.

### 👩‍🏫 Staff / Admin
- Log in securely with admin credentials.
- Upload new academic resources with file attachments, subject tagging, and category selection.
- Update or delete uploaded materials.
- Manage academic subjects (create new course codes, assign semester and credit details).
- Monitor platform analytics and download statistics.

---

## 7. System Architecture & Workflow

```
            ┌─────────────────────────────────────────┐
            │       Academic Resource Vault Portal    │
            │           (Embedded Java Server)        │
            └────────────────────┬────────────────────┘
                                 │
                   ┌─────────────┴─────────────┐
                   ▼                           ▼
        ┌─────────────────────┐     ┌─────────────────────┐
        │    Student Portal   │     │  Staff/Admin Portal │
        └──────────┬──────────┘     └──────────┬──────────┘
                   │                           │
          Instant Search / Filter          Upload New Material
          (Code, Sem, Category)            (PDF, Notes, Answer Key)
                   │                           │
          In-Browser Preview               Edit / Delete Resources
                   │                           │
          Direct File Download             Manage Subject Catalog
                   │                           │
                   └─────────────┬─────────────┘
                                 ▼
                    ┌─────────────────────────┐
                    │   Java Backend (JDBC)   │
                    │      DAO & Services     │
                    └────────────┬────────────┘
                                 │
                   ┌─────────────┴─────────────┐
                   ▼                           ▼
        ┌─────────────────────┐     ┌─────────────────────┐
        │  SQLite DB Storage  │     │ Stored File Storage │
        │  (database/*.db)    │     │      (uploads/)     │
        └─────────────────────┘     └─────────────────────┘
```

---

## 8. Technology Stack

| Category | Technology | Version |
|---|---|---|
| **Programming Language** | Java (JDK) | OpenJDK 11+ |
| **Backend Architecture** | Pure Java Core + HTTP Server | Java 11 Standard Library |
| **Database Connectivity** | JDBC (`java.sql`) | JDBC 4.2 |
| **Database Engine** | SQLite (Embedded RDBMS) | 3.36.0+ |
| **Frontend / UI** | Modern Vanilla HTML5, CSS3, JavaScript | Responsive SPA |
| **UI Design System** | Plus Jakarta Sans, Glassmorphism, Dark/Light Themes | Modern CSS Variables |
| **Build & Run Tool** | `javac` & `java` / Shell Scripts | Standard JDK |

---

## 9. Modules & Class Structure

### 🔐 Authentication Module (`com.vault.service.AuthService`, `com.vault.dao.UserDAO`)
Handles student and staff authentication, SHA-256 password hashing with salt, and session token management.

### 📤 Resource Upload & Management Module (`com.vault.service.ResourceService`, `com.vault.dao.ResourceDAO`)
Handles file persistence in `uploads/`, assigns metadata (subject code, category, size, timestamps), and allows updates/deletions.

### 🔍 Search & Filtering Module
Performs multi-criteria filtering across subject code, title, description, semester number (1–8), semester type (ODD/EVEN), and category.

### 📖 In-Browser Preview & Download Module (`com.vault.web.WebServer`)
Serves document contents for formatted modal viewing, copies text to clipboard, and sets HTTP `Content-Disposition: attachment` headers for file downloads.

### 🗄️ Subject Catalog Module (`com.vault.service.SubjectService`, `com.vault.dao.SubjectDAO`)
Maintains university subjects, odd/even semester mappings, departments, and credit allocations.

---

## 10. Database Overview

**Engine:** SQLite (Zero-configuration file-based relational database)  
**Location:** `database/academic_vault.db`  
**Schema Script:** `database/schema.sql`

### Tables Schema

**1. users**
| Column | Type | Constraints | Description |
|---|---|---|---|
| `user_id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique user identifier |
| `name` | VARCHAR(120) | NOT NULL | Full user name |
| `email` | VARCHAR(150) | NOT NULL UNIQUE | College email / login identifier |
| `password` | VARCHAR(256) | NOT NULL | SHA-256 hashed password with salt |
| `role` | VARCHAR(20) | NOT NULL | `STUDENT` or `ADMIN` |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Account registration date |

**2. subjects**
| Column | Type | Constraints | Description |
|---|---|---|---|
| `subject_code` | VARCHAR(30) | PRIMARY KEY | Unique subject code (e.g. `CS301`) |
| `subject_name` | VARCHAR(150) | NOT NULL | Course title |
| `semester` | INTEGER | NOT NULL | Semester number (1 to 8) |
| `semester_type` | VARCHAR(10) | NOT NULL | `ODD` or `EVEN` |
| `department` | VARCHAR(80) | DEFAULT 'Computer Science' | Academic department |
| `credits` | INTEGER | DEFAULT 3 | Course credit weight |

**3. resources**
| Column | Type | Constraints | Description |
|---|---|---|---|
| `resource_id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique resource ID |
| `subject_code` | VARCHAR(30) | FOREIGN KEY (subjects) | Subject reference |
| `resource_type` | VARCHAR(30) | NOT NULL | `NOTES`, `PYQ`, `QUESTION_BANK`, `ANSWER_KEY` |
| `title` | VARCHAR(200) | NOT NULL | Descriptive title |
| `description` | TEXT | — | Syllabus coverage and details |
| `file_name` | VARCHAR(255) | NOT NULL | Display file name |
| `file_path` | VARCHAR(500) | NOT NULL | Storage path on disk |
| `file_size` | VARCHAR(50) | DEFAULT '1.2 MB' | Human readable file size |
| `file_extension`| VARCHAR(20) | DEFAULT 'pdf' | File extension |
| `uploaded_by` | INTEGER | FOREIGN KEY (users) | Uploader user ID |
| `upload_date` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Timestamp of upload |
| `downloads_count`| INTEGER | DEFAULT 0 | Real-time download counter |

**4. bookmarks**
| Column | Type | Constraints | Description |
|---|---|---|---|
| `bookmark_id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique bookmark ID |
| `user_id` | INTEGER | FOREIGN KEY (users) | User who bookmarked |
| `resource_id` | INTEGER | FOREIGN KEY (resources) | Bookmarked resource |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Date bookmarked |

---

## 11. Project Directory Structure

```
AcademicResourceVault/
├── src/
│   ├── com/vault/
│   │   ├── model/                  # Data Entity Models
│   │   │   ├── Bookmark.java
│   │   │   ├── Resource.java
│   │   │   ├── Subject.java
│   │   │   └── User.java
│   │   ├── dao/                    # JDBC Data Access Objects
│   │   │   ├── BookmarkDAO.java
│   │   │   ├── ResourceDAO.java
│   │   │   ├── SubjectDAO.java
│   │   │   └── UserDAO.java
│   │   ├── service/                # Business Logic Services
│   │   │   ├── AuthService.java
│   │   │   ├── ResourceService.java
│   │   │   └── SubjectService.java
│   │   ├── web/                    # HTTP Controllers & Static Server
│   │   │   └── WebServer.java
│   │   ├── util/                   # Helpers, Security, JSON, DB
│   │   │   ├── DBConnection.java
│   │   │   ├── JsonUtil.java
│   │   │   └── PasswordUtil.java
│   │   └── Main.java               # Main Entry Point
│
├── resources/
│   ├── db.properties               # Database & Server configuration
│   └── web/                        # Modern Web UI
│       ├── index.html              # Single Page Application HTML
│       ├── styles.css              # Custom Professional CSS & Themes
│       └── app.js                  # Frontend Application Controller
│
├── uploads/                        # Physical Stored Study Material Files
├── database/
│   └── schema.sql                  # Database Schema Definition Script
├── lib/
│   └── sqlite-jdbc-3.36.0.3.jar    # SQLite JDBC Driver JAR
├── README.md
└── .gitignore
```

---

## 12. Pre-configured Demo Accounts

For instant evaluation, the portal includes pre-seeded accounts:

| Role | Email | Password | Name |
|---|---|---|---|
| **Staff / Admin** | `admin@vault.edu` | `admin123` | Prof. Sarah Jenkins (HOD & Admin) |
| **Faculty** | `faculty@vault.edu` | `faculty123` | Dr. Robert Vance (Faculty) |
| **Student** | `student@vault.edu` | `student123` | Alex Chen (3rd Year CSE) |
| **Student** | `priya@vault.edu` | `student123` | Priya Sharma (2nd Year CSE) |

*You can also click the quick demo buttons at the top of the interface to switch between accounts with a single click.*

---

## 13. How to Build and Run

### Prerequisites
- Java JDK 11 or higher
- Terminal or Command Prompt

### Step 1: Compile the Project
```bash
cd AcademicResourceVault
javac -encoding UTF-8 -cp "lib/*" -d bin $(find src -name "*.java")
```

*(On Windows PowerShell or Command Prompt, use `;` separator):*
```powershell
javac -encoding UTF-8 -cp "lib/*" -d bin src\com\vault\model\*.java src\com\vault\dao\*.java src\com\vault\util\*.java src\com\vault\service\*.java src\com\vault\web\*.java src\com\vault\Main.java
```

### Step 2: Run the Application
```bash
java -cp "bin:lib/*" com.vault.Main
```
*(On Windows use `bin;lib/*`)*

### Step 3: Open in Browser
Open `http://localhost:8080` in your web browser.

---

## 14. API Endpoints Reference

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/auth/login` | Authenticate user & return session token | Public |
| `POST` | `/api/auth/register` | Register new student or admin account | Public |
| `GET` | `/api/auth/me` | Get current logged-in user details | Authenticated |
| `POST` | `/api/auth/logout` | Invalidate current session token | Authenticated |
| `GET` | `/api/stats` | Platform counters (total files, PYQs, downloads) | Public |
| `GET` | `/api/subjects` | List subjects (filter by semester or sem_type) | Public |
| `POST` | `/api/subjects` | Add new curriculum subject | Admin only |
| `DELETE`| `/api/subjects?code=...` | Remove subject | Admin only |
| `GET` | `/api/resources` | Query resources (filters: `q`, `semester`, `type`) | Public |
| `GET` | `/api/resources/{id}` | Get single resource details | Public |
| `POST` | `/api/resources` | Upload new academic study material | Admin only |
| `PUT` | `/api/resources/{id}` | Update resource title, category, description | Admin only |
| `DELETE`| `/api/resources/{id}` | Delete resource & stored file | Admin only |
| `GET` | `/api/preview?id=...` | Retrieve document text for in-browser preview | Public |
| `GET` | `/api/download?id=...` | Download file & increment download counter | Public |
| `POST` | `/api/bookmarks` | Toggle bookmark status for a resource | Student |

---

## 15. Advantages

- **Zero Dependency Friction:** Built using Java standard library HTTP server + embedded SQLite JDBC.
- **Exam Readiness:** Students can access semester-wise PYQs and model answer keys within seconds.
- **Faculty Control:** Authenticated faculty members can curate and verify all uploaded materials.
- **Offline & Low Bandwidth Friendly:** In-browser document preview avoids unnecessary large downloads.
- **Professional User Experience:** Responsive design, instant autocomplete search, and dark mode support.
