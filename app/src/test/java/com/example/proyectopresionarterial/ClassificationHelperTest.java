package com.example.proyectopresionarterial;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClassificationHelperTest {

    @Test
    public void classify_normal() {
        assertEquals("normal", ClassificationHelper.classify(118, 76));
        assertEquals("normal", ClassificationHelper.classify(110, 70));
    }

    @Test
    public void classify_elevada() {
        assertEquals("elevada", ClassificationHelper.classify(125, 78)); // 120-129 y <80
        assertEquals("elevada", ClassificationHelper.classify(129, 60));
    }

    @Test
    public void classify_stage1() {
        assertTrue(ClassificationHelper.classify(130, 79).toLowerCase().contains("etapa 1"));
        assertTrue(ClassificationHelper.classify(119, 81).toLowerCase().contains("etapa 1")); // PAD 80-89
    }

    @Test
    public void classify_stage2() {
        assertTrue(ClassificationHelper.classify(140, 70).toLowerCase().contains("etapa 2"));
        assertTrue(ClassificationHelper.classify(110, 90).toLowerCase().contains("etapa 2"));
        assertTrue(ClassificationHelper.classify(160, 100).toLowerCase().contains("etapa 2"));
    }
}

