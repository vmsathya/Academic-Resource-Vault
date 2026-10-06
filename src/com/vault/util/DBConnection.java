package com.vault.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Properties;

public class DBConnection {
    private static Properties properties = new Properties();
    private static String dbUrl = "jdbc:sqlite:database/academic_vault.db";
    private static String dbDriver = "org.sqlite.JDBC";
    private static boolean initialized = false;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        File propFile = new File("resources/db.properties");
        if (!propFile.exists()) {
            propFile = new File("AcademicResourceVault/resources/db.properties");
        }
        if (propFile.exists()) {
            try (InputStream in = new FileInputStream(propFile)) {
                properties.load(in);
                if (properties.containsKey("db.url")) {
                    dbUrl = properties.getProperty("db.url");
                }
                if (properties.containsKey("db.driver")) {
                    dbDriver = properties.getProperty("db.driver");
                }
            } catch (IOException e) {
                System.err.println("Warning: Could not load db.properties, using defaults. " + e.getMessage());
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(dbDriver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Database driver not found: " + dbDriver, e);
        }
        return DriverManager.getConnection(dbUrl);
    }

    public static synchronized void initializeDatabase() {
        if (initialized) return;
        try {
            // Ensure parent directory for sqlite db exists
            if (dbUrl.startsWith("jdbc:sqlite:")) {
                String filePath = dbUrl.substring("jdbc:sqlite:".length());
                File dbFile = new File(filePath);
                if (dbFile.getParentFile() != null) {
                    dbFile.getParentFile().mkdirs();
                }
            }

            // Ensure uploads directory exists
            File uploadDir = new File("uploads");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            try (Connection conn = getConnection()) {
                // Read and execute schema.sql
                String schemaPath = "database/schema.sql";
                if (!new File(schemaPath).exists() && new File("AcademicResourceVault/database/schema.sql").exists()) {
                    schemaPath = "AcademicResourceVault/database/schema.sql";
                }
                if (new File(schemaPath).exists()) {
                    String sql = new String(Files.readAllBytes(Paths.get(schemaPath)), StandardCharsets.UTF_8);
                    String[] statements = sql.split(";");
                    try (Statement stmt = conn.createStatement()) {
                        for (String s : statements) {
                            String trimmed = s.trim();
                            if (!trimmed.isEmpty()) {
                                stmt.execute(trimmed);
                            }
                        }
                    }
                }

                // Check if users exist; if not, seed initial data
                seedInitialData(conn);
            }
            initialized = true;
            System.out.println("Academic Resource Vault database successfully initialized.");
        } catch (Exception e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedInitialData(Connection conn) throws SQLException, IOException {
        // Check if users exist
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Already seeded
            }
        }

        System.out.println("Seeding academic data into database...");

        // 1. Seed Users (1 Admin / Faculty Head, 1 Faculty Member, 2 Students)
        String userSql = "INSERT INTO users (user_id, name, email, password, role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(userSql)) {
            // Admin: admin@vault.edu / admin123
            ps.setInt(1, 1);
            ps.setString(2, "Prof. Sarah Jenkins (HOD & Admin)");
            ps.setString(3, "admin@vault.edu");
            ps.setString(4, PasswordUtil.hashPassword("admin123"));
            ps.setString(5, "ADMIN");
            ps.executeUpdate();

            // Staff: faculty@vault.edu / faculty123
            ps.setInt(1, 2);
            ps.setString(2, "Dr. Robert Vance (Faculty)");
            ps.setString(3, "faculty@vault.edu");
            ps.setString(4, PasswordUtil.hashPassword("faculty123"));
            ps.setString(5, "ADMIN");
            ps.executeUpdate();

            // Student 1: student@vault.edu / student123
            ps.setInt(1, 3);
            ps.setString(2, "Alex Chen (3rd Year CSE)");
            ps.setString(3, "student@vault.edu");
            ps.setString(4, PasswordUtil.hashPassword("student123"));
            ps.setString(5, "STUDENT");
            ps.executeUpdate();

            // Student 2: priya@vault.edu / student123
            ps.setInt(1, 4);
            ps.setString(2, "Priya Sharma (2nd Year CSE)");
            ps.setString(3, "priya@vault.edu");
            ps.setString(4, PasswordUtil.hashPassword("student123"));
            ps.setString(5, "STUDENT");
            ps.executeUpdate();
        }

        // 2. Seed Subjects across Semesters (Odd & Even)
        String subSql = "INSERT INTO subjects (subject_code, subject_name, semester, semester_type, department, credits) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(subSql)) {
            Object[][] subjects = {
                {"CS301", "Data Structures & Algorithms", 3, "ODD", "Computer Science", 4},
                {"CS302", "Object Oriented Programming (Java)", 3, "ODD", "Computer Science", 4},
                {"CS303", "Computer Organization & Architecture", 3, "ODD", "Computer Science", 3},
                {"MA301", "Discrete Mathematics & Graph Theory", 3, "ODD", "Mathematics / CSE", 4},
                {"CS401", "Operating Systems & System Programming", 4, "EVEN", "Computer Science", 4},
                {"CS402", "Database Management Systems (DBMS)", 4, "EVEN", "Computer Science", 4},
                {"CS403", "Design & Analysis of Algorithms", 4, "EVEN", "Computer Science", 4},
                {"CS501", "Computer Networks & Protocols", 5, "ODD", "Computer Science", 4},
                {"CS502", "Software Engineering & Agile Methods", 5, "ODD", "Computer Science", 3},
                {"CS601", "Artificial Intelligence & Machine Learning", 6, "EVEN", "Computer Science", 4},
                {"CS602", "Compiler Design & Automata Theory", 6, "EVEN", "Computer Science", 4},
                {"CS701", "Cloud Computing & DevOps Architecture", 7, "ODD", "Computer Science", 3}
            };
            for (Object[] s : subjects) {
                ps.setString(1, (String) s[0]);
                ps.setString(2, (String) s[1]);
                ps.setInt(3, (Integer) s[2]);
                ps.setString(4, (String) s[3]);
                ps.setString(5, (String) s[4]);
                ps.setInt(6, (Integer) s[5]);
                ps.executeUpdate();
            }
        }

        // 3. Create Sample Study Materials and Seed Resources
        File uploadFolder = new File("uploads");
        if (!uploadFolder.exists()) uploadFolder.mkdirs();

        Object[][] resources = {
            {
                "CS301", "PYQ",
                "Nov/Dec 2024 End-Semester Question Paper (Complete)",
                "Official university question paper for Data Structures & Algorithms. Includes Part-A short answers and Part-B comprehensive problem solving questions on AVL trees, Dijkstra's algorithm, and hash collisions.",
                "CS301_PYQ_Nov2024.pdf", "CS301_PYQ_Nov2024.txt", "1.4 MB", "pdf", 1, 42
            },
            {
                "CS301", "ANSWER_KEY",
                "Model Answer Key & Marking Scheme - Nov/Dec 2024 Exam",
                "Official faculty solution manual with step-by-step evaluation rubric, asymptotic time complexity breakdowns, and complete Java code snippets for AVL rotations.",
                "CS301_AnswerKey_Nov2024.pdf", "CS301_AnswerKey_Nov2024.txt", "1.8 MB", "pdf", 1, 67
            },
            {
                "CS301", "NOTES",
                "Module 3 & 4: Trees, Balanced BSTs, and Graph Traversals",
                "Curated professor lecture notes covering Binary Search Trees, AVL Trees, B-Trees, BFS, DFS, Kruskal's and Prim's MST algorithms with visual diagrams.",
                "CS301_LectureNotes_TreesGraphs.pdf", "CS301_LectureNotes_TreesGraphs.txt", "2.6 MB", "pdf", 1, 105
            },
            {
                "CS301", "QUESTION_BANK",
                "Data Structures Question Bank - 120 University Exam Questions",
                "Categorized module-wise question bank with Bloom's taxonomy levels (Remember, Understand, Apply, Analyze) covering units 1 through 5.",
                "CS301_QuestionBank_Module1_5.pdf", "CS301_QuestionBank_Module1_5.txt", "890 KB", "pdf", 2, 53
            },
            {
                "CS401", "PYQ",
                "May/June 2025 Semester IV Examination Paper",
                "Operating Systems end-semester exam paper featuring CPU scheduling algorithms, Banker's deadlock avoidance, semaphores, and virtual memory page replacement.",
                "CS401_PYQ_May2025.pdf", "CS401_PYQ_May2025.txt", "1.1 MB", "pdf", 1, 38
            },
            {
                "CS401", "ANSWER_KEY",
                "Step-by-Step Solutions & Answer Key - May/June 2025 OS Exam",
                "Detailed numerical solutions for Gantt chart CPU scheduling, Peterson's algorithm proof, and FIFO / LRU / Optimal page replacement comparison table.",
                "CS401_AnswerKey_May2025.pdf", "CS401_AnswerKey_May2025.txt", "1.5 MB", "pdf", 2, 81
            },
            {
                "CS401", "NOTES",
                "Complete Lecture Notes: Concurrency, Synchronization & Memory Management",
                "Comprehensive notes authored by Dr. Vance covering Process Synchronization, Mutexes, Monitors, Classical IPC problems, Inverted Page Tables, and TLB.",
                "CS401_Notes_MemoryManagement.pdf", "CS401_Notes_MemoryManagement.txt", "3.2 MB", "pdf", 2, 114
            },
            {
                "CS402", "NOTES",
                "Database Systems Master Notes: Normalization (1NF to BCNF) & Transactions",
                "Deep-dive guide into Functional Dependencies, Armstrong's Axioms, Multi-valued dependencies, ACID properties, 2PL, and Serializability schedules.",
                "CS402_DBMS_Normalization_ACID.pdf", "CS402_DBMS_Normalization_ACID.txt", "2.1 MB", "pdf", 1, 92
            },
            {
                "CS402", "QUESTION_BANK",
                "DBMS Question Bank: SQL Queries, ER Modeling & Indexing",
                "Over 85 practice questions featuring complex nested SQL queries, relational algebra expressions, and B+ tree insertion/deletion exercises.",
                "CS402_DBMS_QuestionBank.pdf", "CS402_DBMS_QuestionBank.txt", "1.3 MB", "pdf", 1, 64
            },
            {
                "CS501", "PYQ",
                "Dec 2024 End-Semester Computer Networks Question Paper",
                "Standard university paper covering OSI vs TCP/IP models, sliding window protocols, IPv4 / IPv6 header structures, subnetting, and Bellman-Ford vs OSPF.",
                "CS501_PYQ_Dec2024.pdf", "CS501_PYQ_Dec2024.txt", "1.2 MB", "pdf", 2, 49
            },
            {
                "CS501", "ANSWER_KEY",
                "Computer Networks Model Answers & Subnetting Calculations",
                "Worked solutions including CIDR notation calculations, VLSM subnetting tables, Go-Back-N window size proofs, and RSA public-key crypto problem.",
                "CS501_AnswerKey_Dec2024.pdf", "CS501_AnswerKey_Dec2024.txt", "1.7 MB", "pdf", 2, 73
            },
            {
                "CS601", "NOTES",
                "AI & Machine Learning: Neural Networks, Backprop & Optimization Notes",
                "Structured lecture slides and notes on gradient descent variants, loss functions, activation functions, convolutional layers, and bias-variance tradeoff.",
                "CS601_AIML_NeuralNetworks.pdf", "CS601_AIML_NeuralNetworks.txt", "3.5 MB", "pdf", 1, 131
            }
        };

        String resSql = "INSERT INTO resources (subject_code, resource_type, title, description, file_name, file_path, file_size, file_extension, uploaded_by, downloads_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(resSql)) {
            for (Object[] r : resources) {
                String subCode = (String) r[0];
                String resType = (String) r[1];
                String title = (String) r[2];
                String desc = (String) r[3];
                String displayFileName = (String) r[4];
                String physicalFileName = (String) r[5];
                String size = (String) r[6];
                String ext = (String) r[7];
                int uploadedBy = (Integer) r[8];
                int downloads = (Integer) r[9];

                // Write authentic document content to file in uploads directory
                File file = new File(uploadFolder, physicalFileName);
                if (!file.exists()) {
                    writeSampleDocument(file, title, subCode, resType, desc);
                }

                ps.setString(1, subCode);
                ps.setString(2, resType);
                ps.setString(3, title);
                ps.setString(4, desc);
                ps.setString(5, displayFileName);
                ps.setString(6, file.getPath());
                ps.setString(7, size);
                ps.setString(8, ext);
                ps.setInt(9, uploadedBy);
                ps.setInt(10, downloads);
                ps.executeUpdate();
            }
        }

        // Seed 1-2 initial bookmarks for student (user_id = 3)
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO bookmarks (user_id, resource_id) VALUES (?, ?)")) {
            ps.setInt(1, 3);
            ps.setInt(2, 1);
            ps.executeUpdate();
            ps.setInt(1, 3);
            ps.setInt(2, 3);
            ps.executeUpdate();
        }

        System.out.println("Academic data successfully seeded.");
    }

    private static void writeSampleDocument(File file, String title, String subjectCode, String type, String desc) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("================================================================================\n");
        content.append("                  ACADEMIC RESOURCE VAULT - OFFICIAL STUDY MATERIAL            \n");
        content.append("================================================================================\n\n");
        content.append("SUBJECT CODE:  ").append(subjectCode).append("\n");
        content.append("CATEGORY:      ").append(type).append("\n");
        content.append("DOCUMENT:      ").append(title).append("\n");
        content.append("VERIFIED BY:   Department of Computer Science & Engineering Academic Council\n");
        content.append("DATE:          Academic Year 2025 - 2026\n");
        content.append("STATUS:        Verified & Released for Examination Preparation\n\n");
        content.append("--------------------------------------------------------------------------------\n");
        content.append("OVERVIEW & INSTRUCTIONS:\n");
        content.append(desc).append("\n");
        content.append("--------------------------------------------------------------------------------\n\n");

        if ("PYQ".equals(type)) {
            content.append("PART A: SHORT ANSWER QUESTIONS (10 x 2 = 20 Marks)\n");
            content.append("--------------------------------------------------\n");
            content.append("1. Define Big-O asymptotic notation and state its formal mathematical definition.\n");
            content.append("2. Differentiate between an AVL Tree and a Red-Black Tree in terms of rotation bounds.\n");
            content.append("3. What is the minimum number of nodes in a complete binary tree of height 5?\n");
            content.append("4. Explain the concept of open addressing with quadratic probing in hash tables.\n");
            content.append("5. Write down the recurrence relation for Merge Sort and solve it using Master's Theorem.\n");
            content.append("6. State the properties of an Adjacency Matrix versus an Adjacency List for sparse graphs.\n");
            content.append("7. Define Topological Sorting and state the prerequisite graph condition.\n");
            content.append("8. What is the worst-case time complexity of Quick Sort, and how can randomized pivot selection avoid it?\n");
            content.append("9. Distinguish between internal fragmentation and external fragmentation.\n");
            content.append("10. State Dijkstra's algorithm greedy choice property and explain why it fails for negative edge weights.\n\n");
            content.append("PART B: DESCRIPTIVE & PROBLEM SOLVING QUESTIONS (5 x 16 = 80 Marks)\n");
            content.append("------------------------------------------------------------------\n");
            content.append("11. (a) Construct an AVL Tree by inserting the following sequence of keys:\n");
            content.append("        {42, 15, 88, 6, 27, 63, 94, 20, 31, 75}.\n");
            content.append("        Show every rotation (LL, RR, LR, RL) and the balance factor of every node after each step. (10 Marks)\n");
            content.append("    (b) Provide the complete Java implementation of the double left-right rotation algorithm. (6 Marks)\n\n");
            content.append("12. (a) Execute Dijkstra's Single Source Shortest Path Algorithm on the directed weighted graph with vertex set V={A, B, C, D, E, F}\n");
            content.append("        and starting vertex A. Tabulate the distance vector updates for each relaxation step. (12 Marks)\n");
            content.append("    (b) Analyze the time complexity when implemented with an Adjacency List and Min-Heap priority queue. (4 Marks)\n");
        } else if ("ANSWER_KEY".equals(type)) {
            content.append("SOLUTION MANUAL & DETAILED RUBRIC:\n\n");
            content.append("QUESTION 1: Formal Definition of Big-O Notation\n");
            content.append("Model Answer: Let f(n) and g(n) be functions from non-negative integers to real numbers.\n");
            content.append("We write f(n) = O(g(n)) if and only if there exist positive constants c and n0 such that:\n");
            content.append("    0 <= f(n) <= c * g(n)  for all n >= n0.\n");
            content.append("Marking Rubric: Definition formula (1 Mark) + Graphical interpretation & threshold n0 (1 Mark).\n\n");
            content.append("QUESTION 11: AVL Tree Construction Step-by-Step\n");
            content.append("Step 1: Insert 42 -> Root node (BF = 0)\n");
            content.append("Step 2: Insert 15 -> Left of 42 (BF of 42 = +1)\n");
            content.append("Step 3: Insert 88 -> Right of 42 (Balanced, BF = 0)\n");
            content.append("Step 4: Insert 6  -> Left of 15 (BF of 15 = +1, BF of 42 = +1)\n");
            content.append("Step 5: Insert 27 -> Right of 15 (Balanced subtree)\n");
            content.append("Step 6: Insert 20 -> Trigger LR Imbalance at node 15 -> Perform Left-Right Double Rotation.\n");
            content.append("Resulting Tree Height: 4 | All nodes satisfy |BF| <= 1.\n\n");
            content.append("Code Verification:\n");
            content.append("Node rotateRight(Node y) {\n");
            content.append("    Node x = y.left;\n");
            content.append("    Node T2 = x.right;\n");
            content.append("    x.right = y;\n");
            content.append("    y.left = T2;\n");
            content.append("    y.height = Math.max(height(y.left), height(y.right)) + 1;\n");
            content.append("    x.height = Math.max(height(x.left), height(x.right)) + 1;\n");
            content.append("    return x;\n");
            content.append("}\n");
        } else if ("NOTES".equals(type)) {
            content.append("CHAPTER SUMMARY & KEY CONCEPTS:\n\n");
            content.append("1. FUNDAMENTALS OF TREE STRUCTURES:\n");
            content.append("   - Root, internal nodes, leaves, depth, height.\n");
            content.append("   - Maximum nodes at depth d: 2^d.\n");
            content.append("   - Maximum nodes in binary tree of height h: 2^(h+1) - 1.\n\n");
            content.append("2. BALANCED BINARY SEARCH TREES:\n");
            content.append("   - AVL Condition: For every node, |height(left) - height(right)| <= 1.\n");
            content.append("   - Search time: O(log n) guaranteed.\n");
            content.append("   - Rebalancing operations: Single Left (RR), Single Right (LL), Left-Right (LR), Right-Left (RL).\n\n");
            content.append("3. GRAPH TRAVERSAL ALGORITHMS:\n");
            content.append("   - Breadth-First Search (BFS): Uses Queue FIFO data structure. Finds shortest path in unweighted graph.\n");
            content.append("   - Depth-First Search (DFS): Uses Stack LIFO / Recursion. Applications: Cycle detection, topological sort.\n");
            content.append("   - Asymptotic Complexity: O(V + E) for adjacency list representation.\n");
        } else {
            content.append("UNIVERSITY QUESTION BANK - BLOOM'S TAXONOMY CLASSIFICATION:\n\n");
            content.append("[LEVEL 1: REMEMBER & RECALL]\n");
            content.append("1. List the four essential properties of an AVL tree.\n");
            content.append("2. Define minimum spanning tree (MST) and state Cut property.\n");
            content.append("3. What is collision resolution? List two open addressing techniques.\n\n");
            content.append("[LEVEL 2: UNDERSTAND & EXPLAIN]\n");
            content.append("4. Explain how heap-sort operates in-place without auxiliary arrays.\n");
            content.append("5. Compare Prim's and Kruskal's algorithms in terms of dense vs sparse graph performance.\n\n");
            content.append("[LEVEL 3: APPLY & DESIGN]\n");
            content.append("6. Design an algorithm to detect whether a directed graph contains a cycle in O(V + E) time.\n");
            content.append("7. Implement a circular queue using an array with wrap-around modulo arithmetic.\n");
        }
        content.append("\n================================================================================\n");
        content.append("End of Study Material | Stored in Academic Resource Vault\n");

        Files.write(file.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));
    }
}
