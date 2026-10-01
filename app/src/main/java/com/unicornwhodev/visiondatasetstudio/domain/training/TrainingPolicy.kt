package com.unicornwhodev.visiondatasetstudio.domain.training

import com.unicornwhodev.visiondatasetstudio.data.json.StudioJson
import com.unicornwhodev.visiondatasetstudio.data.model.ProjectEntity
import com.unicornwhodev.visiondatasetstudio.data.preferences.ProjectSettings
import com.unicornwhodev.visiondatasetstudio.domain.inference.ModelConfig

/** Training availability never gates inference, export or ordinary batch production. */
object TrainingPolicy {
    /** Serial CPU execution makes live and restored checkpoint comparisons reproducible. */
    fun executionConfig(config:ModelConfig)=config.copy(threads=1)
    /** Functional completion is independent from a measured quality gain on a small corpus. */
    fun executionCompleted(steps:Int,total:Int,loss:Double)=total>0 && steps==total && loss.isFinite()
    fun finished(phase:String?)=phase in setOf("completed","rejected","abandoned")
    fun supported(project:ProjectEntity)=runCatching {
        project.modelConfigJson?.let { StudioJson.moshi.adapter(ModelConfig::class.java).fromJson(it) }?.training!=null
    }.getOrDefault(false)
    fun enabled(project:ProjectEntity)=ProjectSettings.read(project).continuousTraining && supported(project)
    fun settingsForModel(project:ProjectEntity,config:ModelConfig?)=ProjectSettings.write(
        ProjectSettings.read(project).let { if(config?.training==null)it.copy(continuousTraining=false) else it })
}
