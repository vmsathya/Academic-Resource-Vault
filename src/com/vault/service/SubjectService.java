package com.vault.service;

import com.vault.dao.SubjectDAO;
import com.vault.model.Subject;

import java.util.List;

public class SubjectService {
    private final SubjectDAO subjectDAO = new SubjectDAO();

    public List<Subject> getSubjects(Integer semester, String semesterType) {
        return subjectDAO.getAll(semester, semesterType);
    }

    public Subject getSubjectByCode(String code) {
        return subjectDAO.getByCode(code);
    }

    public boolean createSubject(Subject subject) {
        if (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
            return false;
        }
        if (subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
            return false;
        }
        return subjectDAO.create(subject);
    }

    public boolean updateSubject(Subject subject) {
        return subjectDAO.update(subject);
    }

    public boolean deleteSubject(String code) {
        return subjectDAO.delete(code);
    }
}
