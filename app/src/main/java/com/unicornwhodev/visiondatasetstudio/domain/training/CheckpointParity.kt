package com.unicornwhodev.visiondatasetstudio.domain.training

import com.unicornwhodev.visiondatasetstudio.domain.inference.TensorValues
import kotlin.math.abs

/** Float32 inference can vary between invocations; shape and finite values remain mandatory. */
object CheckpointParity {
    private const val ABSOLUTE_TOLERANCE=1e-5f
    private const val RELATIVE_TOLERANCE=1e-5f
    fun matches(expected:List<TensorValues>,restored:List<TensorValues>):Boolean {
        if(expected.size!=restored.size || expected.isEmpty())return false
        return expected.zip(restored).all { (a,b) ->
            a.shape==b.shape && a.values.size==b.values.size && a.values.isNotEmpty() &&
                a.values.indices.all { i ->
                    val x=a.values[i];val y=b.values[i]
                    x.isFinite() && y.isFinite() && abs(x-y)<=ABSOLUTE_TOLERANCE+RELATIVE_TOLERANCE*maxOf(abs(x),abs(y))
                }
        }
    }
}
