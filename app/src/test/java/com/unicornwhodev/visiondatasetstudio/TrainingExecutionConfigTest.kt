package com.unicornwhodev.visiondatasetstudio

import com.unicornwhodev.visiondatasetstudio.domain.inference.ModelConfig
import com.unicornwhodev.visiondatasetstudio.domain.training.TrainingPolicy
import org.junit.Assert.*
import org.junit.Test

class TrainingExecutionConfigTest {
    @Test fun completionDoesNotRequireAQualityGain() {
        assertTrue(TrainingPolicy.executionCompleted(111,111,.2467))
        assertTrue(TrainingPolicy.executionCompleted(111,111,.1493))
        assertFalse(TrainingPolicy.executionCompleted(110,111,.12))
        assertFalse(TrainingPolicy.executionCompleted(0,0,.12))
        assertFalse(TrainingPolicy.executionCompleted(111,111,Double.NaN))
        assertFalse(TrainingPolicy.executionCompleted(111,111,Double.POSITIVE_INFINITY))
    }
    @Test fun serialExecutionPreservesTheOriginalContract() {
        val original=ModelConfig(threads=8,inputWidth=704,inputHeight=704,adapter="yolo",
            coordinates="pixels",labels=listOf("smoke","flame"),trainingCheckpoint="checkpoint.json")
        val execution=TrainingPolicy.executionConfig(original)
        assertEquals(1,execution.threads)
        assertEquals(original.copy(threads=1),execution)
        assertEquals(8,original.threads)
        assertEquals(execution,TrainingPolicy.executionConfig(execution))
    }
}
