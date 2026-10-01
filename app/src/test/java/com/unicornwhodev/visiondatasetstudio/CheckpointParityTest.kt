package com.unicornwhodev.visiondatasetstudio

import com.unicornwhodev.visiondatasetstudio.domain.inference.TensorValues
import com.unicornwhodev.visiondatasetstudio.domain.training.CheckpointParity
import org.junit.Assert.*
import org.junit.Test

class CheckpointParityTest {
    private fun tensor(vararg values:Float)=listOf(TensorValues(listOf(1,values.size),values))
    @Test fun float32PixelRoundingDoesNotMasqueradeAsBrokenSerialization() {
        assertTrue(CheckpointParity.matches(tensor(704f,21f,.99f),tensor(704.0003f,21.000017f,.9900005f)))
        assertTrue(CheckpointParity.matches(tensor(0f,-704f),tensor(.000009f,-704.0003f)))
        assertTrue(CheckpointParity.matches(tensor(1f),tensor(1f)))
    }
    @Test fun changedPredictionsAreRejected() {
        assertFalse(CheckpointParity.matches(tensor(704f),tensor(704.02f)))
        assertFalse(CheckpointParity.matches(tensor(.99f),tensor(.9901f)))
        assertFalse(CheckpointParity.matches(tensor(0f),tensor(.001f)))
    }
    @Test fun shapeAndFiniteValuesAreRequired() {
        assertFalse(CheckpointParity.matches(tensor(1f,2f),listOf(TensorValues(listOf(2,1),floatArrayOf(1f,2f)))))
        assertFalse(CheckpointParity.matches(tensor(1f,2f),tensor(1f)))
        assertFalse(CheckpointParity.matches(tensor(Float.NaN),tensor(Float.NaN)))
        assertFalse(CheckpointParity.matches(tensor(Float.POSITIVE_INFINITY),tensor(Float.POSITIVE_INFINITY)))
        assertFalse(CheckpointParity.matches(emptyList(),emptyList()))
        assertTrue("Empty tensors are rejected by their constructor",runCatching{tensor()}.isFailure)
    }
}
