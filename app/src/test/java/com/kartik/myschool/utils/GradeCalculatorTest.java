package com.kartik.myschool.utils;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for GradeCalculator — covering all grade boundaries, edge cases, and helper methods.
 *
 * Audit fix #7: Zero unit tests. Grade calculation is critical — one wrong formula = wrong grades
 * for all students. These tests catch regressions in the grade engine.
 *
 * Run with: ./gradlew test
 */
public class GradeCalculatorTest {

    // ─── getGrade() ────────────────────────────────────────────────────────────

    @Test
    public void getGrade_exactBoundary_A_plus() {
        assertEquals("A+", GradeCalculator.getGrade(90.0));
    }

    @Test
    public void getGrade_above90_returns_A_plus() {
        assertEquals("A+", GradeCalculator.getGrade(95.0));
        assertEquals("A+", GradeCalculator.getGrade(100.0));
    }

    @Test
    public void getGrade_exactBoundary_A() {
        assertEquals("A", GradeCalculator.getGrade(80.0));
    }

    @Test
    public void getGrade_midRange_A() {
        assertEquals("A", GradeCalculator.getGrade(85.0));
    }

    @Test
    public void getGrade_exactBoundary_B_plus() {
        assertEquals("B+", GradeCalculator.getGrade(70.0));
    }

    @Test
    public void getGrade_midRange_B_plus() {
        assertEquals("B+", GradeCalculator.getGrade(75.0));
    }

    @Test
    public void getGrade_exactBoundary_B() {
        assertEquals("B", GradeCalculator.getGrade(60.0));
    }

    @Test
    public void getGrade_midRange_B() {
        assertEquals("B", GradeCalculator.getGrade(65.5));
    }

    @Test
    public void getGrade_exactBoundary_C() {
        assertEquals("C", GradeCalculator.getGrade(50.0));
    }

    @Test
    public void getGrade_midRange_C() {
        assertEquals("C", GradeCalculator.getGrade(55.0));
    }

    @Test
    public void getGrade_exactBoundary_D() {
        assertEquals("D", GradeCalculator.getGrade(35.0));
    }

    @Test
    public void getGrade_midRange_D() {
        assertEquals("D", GradeCalculator.getGrade(40.0));
    }

    @Test
    public void getGrade_below35_returns_F() {
        assertEquals("F", GradeCalculator.getGrade(34.9));
        assertEquals("F", GradeCalculator.getGrade(0.0));
        assertEquals("F", GradeCalculator.getGrade(20.0));
    }

    @Test
    public void getGrade_justBelow_A_plus_boundary() {
        assertEquals("A", GradeCalculator.getGrade(89.9));
    }

    @Test
    public void getGrade_justBelow_A_boundary() {
        assertEquals("B+", GradeCalculator.getGrade(79.9));
    }

    @Test
    public void getGrade_justBelow_B_plus_boundary() {
        assertEquals("B", GradeCalculator.getGrade(69.9));
    }

    @Test
    public void getGrade_justBelow_B_boundary() {
        assertEquals("C", GradeCalculator.getGrade(59.9));
    }

    @Test
    public void getGrade_justBelow_C_boundary() {
        assertEquals("D", GradeCalculator.getGrade(49.9));
    }

    // ─── getEduReportGrade() ────────────────────────────────────────────────────

    @Test
    public void getEduReportGrade_zeroMax_returns_dash() {
        assertEquals("—", GradeCalculator.getEduReportGrade(50, 0));
        assertEquals("—", GradeCalculator.getEduReportGrade(0, -1));
    }

    @Test
    public void getEduReportGrade_91pct_returns_a1() {
        // 91/100 = 91% → "अ-1"
        assertEquals("अ-1", GradeCalculator.getEduReportGrade(91, 100));
    }

    @Test
    public void getEduReportGrade_100pct_returns_a1() {
        assertEquals("अ-1", GradeCalculator.getEduReportGrade(100, 100));
    }

    @Test
    public void getEduReportGrade_81to90pct_returns_a2() {
        assertEquals("अ-2", GradeCalculator.getEduReportGrade(81, 100));
        assertEquals("अ-2", GradeCalculator.getEduReportGrade(85, 100));
        assertEquals("अ-2", GradeCalculator.getEduReportGrade(90, 100));
    }

    @Test
    public void getEduReportGrade_71to80pct_returns_b1() {
        assertEquals("ब-1", GradeCalculator.getEduReportGrade(71, 100));
        assertEquals("ब-1", GradeCalculator.getEduReportGrade(75, 100));
    }

    @Test
    public void getEduReportGrade_61to70pct_returns_b2() {
        assertEquals("ब-2", GradeCalculator.getEduReportGrade(61, 100));
        assertEquals("ब-2", GradeCalculator.getEduReportGrade(70, 100));
    }

    @Test
    public void getEduReportGrade_51to60pct_returns_k1() {
        assertEquals("क-1", GradeCalculator.getEduReportGrade(51, 100));
        assertEquals("क-1", GradeCalculator.getEduReportGrade(60, 100));
    }

    @Test
    public void getEduReportGrade_41to50pct_returns_k2() {
        assertEquals("क-2", GradeCalculator.getEduReportGrade(41, 100));
        assertEquals("क-2", GradeCalculator.getEduReportGrade(50, 100));
    }

    @Test
    public void getEduReportGrade_below41pct_returns_D() {
        assertEquals("ड", GradeCalculator.getEduReportGrade(40, 100));
        assertEquals("ड", GradeCalculator.getEduReportGrade(0, 100));
    }

    @Test
    public void getEduReportGrade_fractionalMax() {
        // 45.5/50 = 91% → "अ-1"
        assertEquals("अ-1", GradeCalculator.getEduReportGrade(45.5, 50));
    }

    // ─── getResult() ───────────────────────────────────────────────────────────

    @Test
    public void getResult_above35_PASS() {
        assertEquals("PASS", GradeCalculator.getResult(35.0));
        assertEquals("PASS", GradeCalculator.getResult(50.0));
        assertEquals("PASS", GradeCalculator.getResult(100.0));
    }

    @Test
    public void getResult_below35_FAIL() {
        assertEquals("FAIL", GradeCalculator.getResult(34.9));
        assertEquals("FAIL", GradeCalculator.getResult(0.0));
    }

    // ─── getPercentage() ───────────────────────────────────────────────────────

    @Test
    public void getPercentage_normal() {
        assertEquals(75.0, GradeCalculator.getPercentage(75, 100), 0.001);
    }

    @Test
    public void getPercentage_zeroMax_returns0() {
        assertEquals(0.0, GradeCalculator.getPercentage(50, 0), 0.001);
    }

    @Test
    public void getPercentage_zeroObtained() {
        assertEquals(0.0, GradeCalculator.getPercentage(0, 100), 0.001);
    }

    @Test
    public void getPercentage_fullMarks() {
        assertEquals(100.0, GradeCalculator.getPercentage(100, 100), 0.001);
    }

    @Test
    public void getPercentage_fraction() {
        assertEquals(33.333, GradeCalculator.getPercentage(100, 300), 0.001);
    }

    // ─── isPassInAll() ─────────────────────────────────────────────────────────

    @Test
    public void isPassInAll_allPassing_returnsTrue() {
        Map<String, Double> obtained = new HashMap<>();
        obtained.put("Math", 40.0);
        obtained.put("Science", 60.0);

        Map<String, Integer> max = new HashMap<>();
        max.put("Math", 100);
        max.put("Science", 100);

        assertTrue(GradeCalculator.isPassInAll(obtained, max));
    }

    @Test
    public void isPassInAll_oneSubjectFailing_returnsFalse() {
        Map<String, Double> obtained = new HashMap<>();
        obtained.put("Math", 20.0); // 20% < 35% → FAIL
        obtained.put("Science", 80.0);

        Map<String, Integer> max = new HashMap<>();
        max.put("Math", 100);
        max.put("Science", 100);

        assertFalse(GradeCalculator.isPassInAll(obtained, max));
    }

    @Test
    public void isPassInAll_exactPassBoundary_returnsTrue() {
        Map<String, Double> obtained = new HashMap<>();
        obtained.put("Math", 35.0); // exactly 35% → borderline PASS

        Map<String, Integer> max = new HashMap<>();
        max.put("Math", 100);

        assertTrue(GradeCalculator.isPassInAll(obtained, max));
    }

    @Test
    public void isPassInAll_zeroMaxSubject_skippedFromFail() {
        // Subject with max=0 should not cause a fail (division by zero guard)
        Map<String, Double> obtained = new HashMap<>();
        obtained.put("Art", 0.0); // max = 0, should be skipped

        Map<String, Integer> max = new HashMap<>();
        max.put("Art", 0);

        assertTrue(GradeCalculator.isPassInAll(obtained, max));
    }

    @Test
    public void isPassInAll_subjectWithNoMaxEntry_usesDefault100() {
        // Subject not in max map → defaults to 100
        Map<String, Double> obtained = new HashMap<>();
        obtained.put("Math", 50.0); // 50/100 = 50% → PASS

        Map<String, Integer> max = new HashMap<>();
        // No "Math" entry — defaults to 100

        assertTrue(GradeCalculator.isPassInAll(obtained, max));
    }

    @Test
    public void isPassInAll_emptySubjects_returnsTrue() {
        Map<String, Double> obtained = new HashMap<>();
        Map<String, Integer> max = new HashMap<>();
        assertTrue(GradeCalculator.isPassInAll(obtained, max));
    }
}
