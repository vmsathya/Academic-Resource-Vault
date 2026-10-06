package com.vault.model;

public class Subject {
    private String subjectCode;
    private String subjectName;
    private int semester;
    private String semesterType; // "ODD" or "EVEN"
    private String department;
    private int credits;
    private int resourceCount;

    public Subject() {}

    public Subject(String subjectCode, String subjectName, int semester, String semesterType, String department, int credits) {
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.semester = semester;
        this.semesterType = semesterType;
        this.department = department;
        this.credits = credits;
    }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getSemesterType() { return semesterType; }
    public void setSemesterType(String semesterType) { this.semesterType = semesterType; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public int getResourceCount() { return resourceCount; }
    public void setResourceCount(int resourceCount) { this.resourceCount = resourceCount; }
}
