package com.kartik.myschool.repository;

import org.junit.Test;
import java.util.HashMap;
import com.kartik.myschool.model.MarksRecord;
import static org.junit.Assert.*;

public class FirebaseRepositoryMergeTest {

    @Test
    public void testMerge_NullTargetOrSource_NoCrash() {
        MarksRecord target = new MarksRecord();
        MarksRecord source = new MarksRecord();

        // Should not crash
        FirebaseRepository.mergeRecords(null, source);
        FirebaseRepository.mergeRecords(target, null);
        FirebaseRepository.mergeRecords(null, null);
    }

    @Test
    public void testMerge_TargetMissingDetailedMarks_SourceHasIt_Copied() {
        MarksRecord target = new MarksRecord();
        target.detailedMarks = null;

        MarksRecord source = new MarksRecord();
        source.detailedMarks = new HashMap<>();
        MarksRecord.SubjectMarksDetail detail = new MarksRecord.SubjectMarksDetail();
        detail.grandTotal = 80;
        source.detailedMarks.put("Math", detail);

        FirebaseRepository.mergeRecords(target, source);

        assertNotNull(target.detailedMarks);
        assertTrue(target.detailedMarks.containsKey("Math"));
        assertEquals(80, target.detailedMarks.get("Math").grandTotal);
    }

    @Test
    public void testMerge_TargetAndSourceHaveDetailedMarks_NoOverwrite() {
        MarksRecord target = new MarksRecord();
        target.detailedMarks = new HashMap<>();
        MarksRecord.SubjectMarksDetail targetMath = new MarksRecord.SubjectMarksDetail();
        targetMath.grandTotal = 90;
        target.detailedMarks.put("Math", targetMath);

        MarksRecord source = new MarksRecord();
        source.detailedMarks = new HashMap<>();
        MarksRecord.SubjectMarksDetail sourceMath = new MarksRecord.SubjectMarksDetail();
        sourceMath.grandTotal = 70;
        source.detailedMarks.put("Math", sourceMath);

        MarksRecord.SubjectMarksDetail sourceScience = new MarksRecord.SubjectMarksDetail();
        sourceScience.grandTotal = 85;
        source.detailedMarks.put("Science", sourceScience);

        FirebaseRepository.mergeRecords(target, source);

        assertEquals(90, target.detailedMarks.get("Math").grandTotal);
        assertTrue(target.detailedMarks.containsKey("Science"));
        assertEquals(85, target.detailedMarks.get("Science").grandTotal);
    }

    @Test
    public void testMerge_TargetAndSourceHaveDetailedMarks_NullDetailInSource_Ignored() {
        MarksRecord target = new MarksRecord();
        target.detailedMarks = new HashMap<>();

        MarksRecord source = new MarksRecord();
        source.detailedMarks = new HashMap<>();
        source.detailedMarks.put("Math", null);

        FirebaseRepository.mergeRecords(target, source);

        assertFalse(target.detailedMarks.containsKey("Math"));
    }

    @Test
    public void testMerge_PresentDays_TargetZero_SourceNonZero_Copied() {
        MarksRecord target = new MarksRecord();
        target.presentDays = 0;

        MarksRecord source = new MarksRecord();
        source.presentDays = 45;

        FirebaseRepository.mergeRecords(target, source);

        assertEquals(45, target.presentDays);
    }

    @Test
    public void testMerge_PresentDays_TargetNonZero_SourceNonZero_NoOverwrite() {
        MarksRecord target = new MarksRecord();
        target.presentDays = 50;

        MarksRecord source = new MarksRecord();
        source.presentDays = 45;

        FirebaseRepository.mergeRecords(target, source);

        assertEquals(50, target.presentDays);
    }

    @Test
    public void testMerge_TotalDays_TargetZero_SourceNonZero_Copied() {
        MarksRecord target = new MarksRecord();
        target.totalDays = 0;

        MarksRecord source = new MarksRecord();
        source.totalDays = 60;

        FirebaseRepository.mergeRecords(target, source);

        assertEquals(60, target.totalDays);
    }

    @Test
    public void testMerge_TotalDays_TargetNonZero_SourceNonZero_NoOverwrite() {
        MarksRecord target = new MarksRecord();
        target.totalDays = 55;

        MarksRecord source = new MarksRecord();
        source.totalDays = 60;

        FirebaseRepository.mergeRecords(target, source);

        assertEquals(55, target.totalDays);
    }

    @Test
    public void testMerge_SubjectMarks_TargetNull_SourceHasIt_Copied() {
        MarksRecord target = new MarksRecord();
        target.subjectMarks = null;

        MarksRecord source = new MarksRecord();
        source.subjectMarks = new HashMap<>();
        source.subjectMarks.put("Math", 85.0);

        FirebaseRepository.mergeRecords(target, source);

        assertNotNull(target.subjectMarks);
        assertEquals(85.0, target.subjectMarks.get("Math"), 0.0);
    }

    @Test
    public void testMerge_SubjectMarks_TargetAndSourceHaveIt_NoOverwrite() {
        MarksRecord target = new MarksRecord();
        target.subjectMarks = new HashMap<>();
        target.subjectMarks.put("Math", 90.0);

        MarksRecord source = new MarksRecord();
        source.subjectMarks = new HashMap<>();
        source.subjectMarks.put("Math", 75.0);
        source.subjectMarks.put("Science", 80.0);

        FirebaseRepository.mergeRecords(target, source);

        assertEquals(90.0, target.subjectMarks.get("Math"), 0.0);
        assertEquals(80.0, target.subjectMarks.get("Science"), 0.0);
    }

    @Test
    public void testMerge_SubjectMax_TargetNull_SourceHasIt_Copied() {
        MarksRecord target = new MarksRecord();
        target.subjectMax = null;

        MarksRecord source = new MarksRecord();
        source.subjectMax = new HashMap<>();
        source.subjectMax.put("Math", 100);

        FirebaseRepository.mergeRecords(target, source);

        assertNotNull(target.subjectMax);
        assertEquals(100, target.subjectMax.get("Math").intValue());
    }

    @Test
    public void testMerge_SubjectMax_TargetAndSourceHaveIt_NoOverwrite() {
        MarksRecord target = new MarksRecord();
        target.subjectMax = new HashMap<>();
        target.subjectMax.put("Math", 100);

        MarksRecord source = new MarksRecord();
        source.subjectMax = new HashMap<>();
        source.subjectMax.put("Math", 50);
        source.subjectMax.put("Science", 100);

        FirebaseRepository.mergeRecords(target, source);

        assertEquals(100, target.subjectMax.get("Math").intValue());
        assertEquals(100, target.subjectMax.get("Science").intValue());
    }

    @Test
    public void testMerge_TotalObtained_TargetZero_SourceNonZero_Copied() {
        MarksRecord target = new MarksRecord();
        target.totalObtained = 0.0;

        MarksRecord source = new MarksRecord();
        source.totalObtained = 350.5;

        FirebaseRepository.mergeRecords(target, source);

        assertEquals(350.5, target.totalObtained, 0.0);
    }

    @Test
    public void testMerge_TotalObtained_TargetNonZero_SourceNonZero_NoOverwrite() {
        MarksRecord target = new MarksRecord();
        target.totalObtained = 380.0;

        MarksRecord source = new MarksRecord();
        source.totalObtained = 350.5;

        FirebaseRepository.mergeRecords(target, source);

        assertEquals(380.0, target.totalObtained, 0.0);
    }

    @Test
    public void testMerge_TotalMax_TargetZero_SourceNonZero_Copied() {
        MarksRecord target = new MarksRecord();
        target.totalMax = 0;

        MarksRecord source = new MarksRecord();
        source.totalMax = 500;

        FirebaseRepository.mergeRecords(target, source);

        assertEquals(500, target.totalMax);
    }

    @Test
    public void testMerge_TotalMax_TargetNonZero_SourceNonZero_NoOverwrite() {
        MarksRecord target = new MarksRecord();
        target.totalMax = 600;

        MarksRecord source = new MarksRecord();
        source.totalMax = 500;

        FirebaseRepository.mergeRecords(target, source);

        assertEquals(600, target.totalMax);
    }

    @Test
    public void testMerge_Percentage_TargetZero_SourceNonZero_Copied() {
        MarksRecord target = new MarksRecord();
        target.percentage = 0.0;

        MarksRecord source = new MarksRecord();
        source.percentage = 78.5;

        FirebaseRepository.mergeRecords(target, source);

        assertEquals(78.5, target.percentage, 0.0);
    }

    @Test
    public void testMerge_Percentage_TargetNonZero_SourceNonZero_NoOverwrite() {
        MarksRecord target = new MarksRecord();
        target.percentage = 82.0;

        MarksRecord source = new MarksRecord();
        source.percentage = 78.5;

        FirebaseRepository.mergeRecords(target, source);

        assertEquals(82.0, target.percentage, 0.0);
    }

    @Test
    public void testMerge_Grade_TargetNullOrEmpty_SourceHasIt_Copied() {
        MarksRecord target = new MarksRecord();
        target.grade = "";

        MarksRecord source = new MarksRecord();
        source.grade = "A+";

        FirebaseRepository.mergeRecords(target, source);

        assertEquals("A+", target.grade);
    }

    @Test
    public void testMerge_Grade_TargetHasIt_SourceHasIt_NoOverwrite() {
        MarksRecord target = new MarksRecord();
        target.grade = "A";

        MarksRecord source = new MarksRecord();
        source.grade = "A+";

        FirebaseRepository.mergeRecords(target, source);

        assertEquals("A", target.grade);
    }

    @Test
    public void testMerge_Result_TargetNullOrEmpty_SourceHasIt_Copied() {
        MarksRecord target = new MarksRecord();
        target.result = null;

        MarksRecord source = new MarksRecord();
        source.result = "PASS";

        FirebaseRepository.mergeRecords(target, source);

        assertEquals("PASS", target.result);
    }

    @Test
    public void testMerge_Result_TargetHasIt_SourceHasIt_NoOverwrite() {
        MarksRecord target = new MarksRecord();
        target.result = "FAIL";

        MarksRecord source = new MarksRecord();
        source.result = "PASS";

        FirebaseRepository.mergeRecords(target, source);

        assertEquals("FAIL", target.result);
    }
}
