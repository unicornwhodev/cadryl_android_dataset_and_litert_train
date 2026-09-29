package com.unicornwhodev.visiondatasetstudio.domain.batch

import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import com.unicornwhodev.visiondatasetstudio.data.model.unreviewedCount
import android.graphics.BitmapFactory
import androidx.room.withTransaction
import com.unicornwhodev.visiondatasetstudio.core.geometry.HashUtils
import com.unicornwhodev.visiondatasetstudio.core.storage.StorageManager
import com.unicornwhodev.visiondatasetstudio.data.db.AppDatabase
import com.unicornwhodev.visiondatasetstudio.data.hf.HfApiClient
import com.unicornwhodev.visiondatasetstudio.data.hf.RemoteReceipt
import com.unicornwhodev.visiondatasetstudio.data.hf.RemoteFileDigest
import com.unicornwhodev.visiondatasetstudio.core.workflow.PublicationSafety
import com.unicornwhodev.visiondatasetstudio.core.workflow.ResumeDecision
import com.unicornwhodev.visiondatasetstudio.core.storage.DurableFiles
import com.unicornwhodev.visiondatasetstudio.data.model.AcquisitionStatus
import com.unicornwhodev.visiondatasetstudio.data.model.AnnotationRecord
import com.unicornwhodev.visiondatasetstudio.data.model.AnnotationStatus
import com.unicornwhodev.visiondatasetstudio.data.model.AuditLogEntity
import com.unicornwhodev.visiondatasetstudio.data.model.BatchEntity
import com.unicornwhodev.visiondatasetstudio.data.model.BoxTarget
import com.unicornwhodev.visiondatasetstudio.data.model.PointTarget
import com.unicornwhodev.visiondatasetstudio.data.model.ProjectEntity
import com.unicornwhodev.visiondatasetstudio.data.model.SampleAnnotations
import com.unicornwhodev.visiondatasetstudio.data.model.SampleEntity
import com.unicornwhodev.visiondatasetstudio.data.model.SourceEntryEntity
import com.unicornwhodev.visiondatasetstudio.data.model.SyncStatus
import com.unicornwhodev.visiondatasetstudio.data.model.TagTarget
import com.unicornwhodev.visiondatasetstudio.domain.export.DatasetExporters
import com.unicornwhodev.visiondatasetstudio.domain.inference.LiteRtEngine
import com.unicornwhodev.visiondatasetstudio.domain.inference.*
import com.unicornwhodev.visiondatasetstudio.domain.validation.HumanAnnotationReview
import com.unicornwhodev.visiondatasetstudio.data.preferences.ProjectSettings
import com.unicornwhodev.visiondatasetstudio.data.source.SourceCatalog
import com.unicornwhodev.visiondatasetstudio.core.storage.ImageNormalizer
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext
import com.unicornwhodev.visiondatasetstudio.core.workflow.ProcessingSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class BatchEngine(
    private val db: AppDatabase,
    private val storageManager: StorageManager,
    private val hfApiClient: HfApiClient,
    private val liteRtEngine: LiteRtEngine,
    private val exporters: DatasetExporters
) {
    companion object { private val discoveryMutex=Mutex(); private val acquisitionMutex=Mutex(); private val preparationMutex=Mutex() }
    private val moshi = com.unicornwhodev.visiondatasetstudio.data.json.StudioJson.moshi
    private val annotAdapter = moshi.adapter(SampleAnnotations::class.java)
    val sourceCatalog=SourceCatalog(storageManager.context,db,hfApiClient)
    private val workClaims=WorkClaimCoordinator(storageManager.context,hfApiClient)
    fun checkNetwork(settings:ProcessingSettings) {
        if(!settings.allowMetered) {
            val cm=storageManager.context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            check(!cm.isActiveNetworkMetered) { tr("Réseau limité refusé par vos réglages. Connectez un réseau non facturé ou autorisez les données mobiles.", "Metered network blocked by your settings. Connect to an unmetered network or allow mobile data.") }
        }
    }

    fun observeBatchSamples(projectId: Long, batchNumber: Int): Flow<List<SampleEntity>> {
        return db.sampleDao().getSamplesForBatch(projectId, batchNumber)
    }

    suspend fun getBatchEntity(projectId: Long, batchNumber: Int): BatchEntity? {
        return db.batchDao().getBatchSync(projectId, batchNumber)
    }

    /**
     * Discovers a configurable batch from the selected source and persists its metadata transactionally.
     */
    suspend fun discoverViewerBatch(requestedProject:ProjectEntity,batchNumber:Int,count:Int=100,append:Boolean=false):BatchDiscoveryResult = discoveryMutex.withLock { withContext(Dispatchers.IO) {
        try {
            require(count in 1..1000)
            var project=db.projectDao().getProjectSync(requestedProject.id) ?: error(tr("Projet absent", "Project not found"))
            val batch=db.batchDao().getBatchSync(project.id,batchNumber)
            check(if(append)batch!=null && batch.status !in PublicationSafety.lockedStates else batch==null) { tr("Lot déjà découvert ou verrouillé", "Batch already discovered or locked") }
            var settings=ProjectSettings.read(project)
            if(settings.sourceMode=="HF_VIEWER") {
                checkNetwork(settings)
                hfApiClient.configureTimeout(settings.timeoutSeconds)
                if(settings.viewerExpectedRows==null && settings.filterExpression.isBlank() && settings.orderBy.isBlank()) {
                    val splits=hfApiClient.fetchViewerSplits(project.hfSourceRepo)
                    val total=splits.splits.firstOrNull { it.config==project.sourceConfig && it.split==project.sourceSplit }?.numRows
                    if(total!=null) {
                        settings=settings.copy(viewerExpectedRows=total)
                        project=project.copy(settingsJson=ProjectSettings.write(settings),updatedAt=System.currentTimeMillis())
                        db.projectDao().saveProject(project)
                    }
                }
            } else hfApiClient.configureTimeout(settings.timeoutSeconds)
            val claimSelection=if(settings.collaborationEnabled) {
                val selected=mutableListOf<SourceEntryEntity>();var consumed=0;var skipped=0;var rounds=0
                while(selected.size<count && rounds++<12) {
                    val wanted=count-selected.size
                    val scanCount=minOf(1000,maxOf(wanted,wanted*5))
                    val startOffset=project.lastRowCursor+consumed
                    val page=sourceCatalog.page(project,startOffset,scanCount)
                    if(page.consumed==0)break
                    if(page.entries.isEmpty()) {
                        consumed+=page.consumed
                        skipped+=page.rejected
                        continue
                    }
                    val part=workClaims.claim(project,settings,page.entries,wanted)
                    selected+=part.entries
                    // Advance in upstream coordinates, including only rejected rows that occur
                    // before the last valid candidate actually inspected by the claim coordinator.
                    val advance=SourceWindowProgress.advance(startOffset,page,part.consumed)
                    skipped+=part.skipped+SourceWindowProgress.rejectedInPrefix(advance,part.consumed)
                    consumed+=advance
                }
                ClaimSelection(selected,consumed,skipped)
            } else {
                val selected=mutableListOf<SourceEntryEntity>();var consumed=0;var skipped=0;var rounds=0
                while(selected.size<count && rounds++<12) {
                    val wanted=count-selected.size
                    val scanCount=minOf(1000,maxOf(wanted,wanted*5))
                    val startOffset=project.lastRowCursor+consumed
                    val page=sourceCatalog.page(project,startOffset,scanCount)
                    if(page.consumed==0)break
                    if(page.entries.isEmpty()) {
                        consumed+=page.consumed
                        skipped+=page.rejected
                        continue
                    }
                    val taken=page.entries.take(wanted)
                    selected+=taken
                    val advance=if(taken.size>=wanted) SourceWindowProgress.advance(startOffset,page,taken.size) else page.consumed
                    skipped+=SourceWindowProgress.rejectedInPrefix(advance,taken.size)
                    consumed+=advance
                }
                ClaimSelection(selected,consumed,skipped)
            }
            val entries=claimSelection.entries
            if(entries.isEmpty()) {
                if(claimSelection.consumed==0) return@withContext BatchDiscoveryResult(true,endOfSource=true)
                db.withTransaction {
                    val current=db.projectDao().getProjectSync(project.id) ?: error(tr("Projet absent", "Project not found"))
                    check(current.lastRowCursor==project.lastRowCursor){tr("La source a avancé; relancez l’import", "The source has advanced; restart import")}
                    db.projectDao().saveProject(current.copy(lastRowCursor=project.lastRowCursor+claimSelection.consumed,updatedAt=System.currentTimeMillis()))
                    db.auditDao().insertLog(AuditLogEntity(sampleId=null,batchNumber=batchNumber,action="SKIP_SOURCE_ROWS",
                        details=tr("${claimSelection.consumed} ligne(s) parcourue(s), aucune exploitable; curseur avancé sans créer de lot.",
                            "${claimSelection.consumed} row(s) scanned, none usable; cursor advanced without creating a batch."),
                        projectId=project.id))
                }
                return@withContext BatchDiscoveryResult(true,0)
            }
            val samples=entries.map { row ->
                val digest=UUID.nameUUIDFromBytes(row.assetId.toByteArray()).toString().take(12)
                SampleEntity(sampleId="p${project.id}_b${batchNumber}_r${row.ordinal}_$digest",projectId=project.id,batchNumber=batchNumber,
                    assetId=row.assetId,sourceRowIndex=row.sourceRowIndex ?: row.ordinal,sourceOrdinal=row.ordinal,sourceFileUrl=row.imageRef,localImagePath=null,
                    groupId=row.groupId,split=project.targetSplit,acquisitionStatus="DISCOVERED",annotationStatus=if(row.annotationJson==null) "PENDING" else "DRAFTS_AVAILABLE",syncStatus="NOT_EXPORTED")
            }
            db.withTransaction {
                val current=db.projectDao().getProjectSync(project.id) ?: error(tr("Projet absent", "Project not found"))
                check(current.lastRowCursor==project.lastRowCursor){tr("La source a avancé; relancez l’import", "The source has advanced; restart import")}
                if(batch==null)db.batchDao().insertOrReplace(BatchEntity(project.id,batchNumber,"DISCOVERED",samples.size))
                else db.batchDao().updateBatch(batch.copy(totalCases=batch.totalCases+samples.size))
                db.sampleDao().insertNewSamples(samples)
                samples.zip(entries).forEach { (sample,row)->row.annotationJson?.let{db.annotationDao().insertOrReplace(AnnotationRecord(sample.sampleId,it))} }
                db.projectDao().saveProject(current.copy(lastRowCursor=project.lastRowCursor+claimSelection.consumed,updatedAt=System.currentTimeMillis()))
                db.auditDao().insertLog(AuditLogEntity(sampleId=null,batchNumber=batchNumber,action="DISCOVER_BATCH",details=tr("${entries.size} cas; source ${settings.sourceMode}; curseur ${project.lastRowCursor}; coordination=${settings.collaborationEnabled}; ignorés=${claimSelection.skipped}", "${entries.size} samples; source ${settings.sourceMode}; cursor ${project.lastRowCursor}; coordination=${settings.collaborationEnabled}; skipped=${claimSelection.skipped}"),projectId=project.id))
            }
            BatchDiscoveryResult(true,samples.size)
        } catch(e:CancellationException){throw e} catch(e:Exception){BatchDiscoveryResult(false,error=e.message)}
    } }

    /**
     * Downloads images for discovered cases with bounded concurrency and disk checking.
     */
    suspend fun acquireBatchImages(projectId:Long,batchNumber:Int,onProgress:(Int,Int)->Unit):Boolean = acquisitionMutex.withLock { withContext(Dispatchers.IO) {
        val project=db.projectDao().getProjectSync(projectId) ?: error(tr("Projet absent", "Project not found"))
        check(db.batchDao().getBatchSync(projectId,batchNumber)?.status !in PublicationSafety.lockedStates) { tr("Lot verrouillé; récupération refusée", "Batch locked; download refused") }
        val settings=ProjectSettings.read(project);hfApiClient.configureTimeout(settings.timeoutSeconds)
        val samples=db.sampleDao().getSamplesForBatchSync(projectId,batchNumber)
        db.batchDao().updateStatus(projectId,batchNumber,"DOWNLOADING")
        val decodePermit=Semaphore(1)
        val permits=Semaphore(settings.downloadConcurrency);val completed=AtomicInteger();val failures=AtomicInteger()
        val budgetLock=Any();var reserved=0L
        coroutineScope {
            samples.map { original->async {
                permits.withPermit {
                    if(original.localImagePath?.let{path -> val file=storageManager.ownedImage(path); file.isFile && original.sha256!=null && HashUtils.computeSha256(file)==original.sha256}==true || original.annotationStatus in setOf("REJECTED","DUPLICATE") || original.syncStatus in setOf("VERIFIED","PURGED")) {
                        onProgress(completed.incrementAndGet(),samples.size);return@withPermit
                    }
                    var sample=original;var reservation=0L;var actual:File?=null
                    try {
                        coroutineContext.ensureActive()
                        reservation=synchronized(budgetLock) {
                            val available=minOf(project.diskBudgetMb*1048576-storageManager.getUsedSpaceBytes()-reserved,
                                storageManager.getFreeSpaceBytes()-settings.reserveFreeMb*1048576L-reserved)
                            val bytes=minOf(settings.maxImageMb*1048576L,available/2)
                            check(bytes>0) { tr("Budget disque atteint; les autres lots et modèles restent conservés", "Storage budget reached; other batches and models are preserved") }
                            reserved+=bytes*2;bytes*2
                        }
                        val file=storageManager.getImageFile(sample.sampleId)
                        db.sampleDao().updateSample(sample.copy(acquisitionStatus="DOWNLOADING"))
                        var ok=false;var lastFailure:String?=null
                        for(attempt in 0..settings.retryCount) {
                            coroutineContext.ensureActive()
                            try {
                                val ref=sample.sourceFileUrl ?: error(tr("Référence image absente", "Image reference missing"))
                                if(ref.startsWith("https://"))checkNetwork(settings)
                                ok=sourceCatalog.copyAsset(ref,file,reservation/2)
                                if(ok)break
                            } catch(e:CancellationException){throw e} catch(e:Exception){lastFailure=e.message}
                            if(attempt<settings.retryCount) {
                                if(settings.sourceMode=="HF_VIEWER") {
                                    val freshPage=sourceCatalog.page(project,sample.sourceOrdinal ?: sample.sourceRowIndex,1)
                                    val fresh=freshPage.entries.singleOrNull()
                                    check(freshPage.consumed==0 || fresh!=null) {
                                        tr("La ligne source n’est plus exploitable; reprise suspendue pour préserver la provenance", "The source row is no longer usable; resumption suspended to preserve provenance")
                                    }
                                    check(fresh==null || fresh.assetId==sample.assetId){tr("La source Viewer a changé; reprise suspendue pour préserver la provenance", "The Viewer source changed; resumption suspended to preserve provenance")}
                                    if(fresh!=null)sample=sample.copy(sourceFileUrl=fresh.imageRef)
                                }
                                delay(500L*(1L shl attempt))
                            }
                        }
                        check(ok && file.length()>0){lastFailure ?: tr("Acquisition échouée ou limite de taille dépassée", "Acquisition failed or size limit exceeded")}
                        decodePermit.withPermit {
                        val before=storageManager.readImageMetadata(file)
                        require(before.width>0 && before.height>0 && before.width.toLong()*before.height<=settings.sourceMaxPixels){tr("Dimensions image invalides ou plafond pixels dépassé", "Invalid image dimensions or pixel limit exceeded")}
                        val sourceHash=HashUtils.computeSha256(file)
                        val transform=ImageNormalizer.normalize(file,settings.normalizeExif,db.annotationDao().getAnnotationSync(sample.sampleId)!=null)
                        check(file.length()<=reservation){tr("Image normalisée trop volumineuse", "Normalized image too large")}
                        val meta=storageManager.readImageMetadata(file)
                        val ext=when(meta.mimeType){"image/jpeg"->"jpg";"image/png"->"png";"image/webp"->"webp";else->error(tr("Format image non pris en charge", "Unsupported image format"))}
                        actual=storageManager.getImageFile(sample.sampleId,ext)
                        if(actual!=file)check(file.renameTo(actual))
                        var factor=1;while(maxOf(meta.width,meta.height)/factor>256)factor*=2
                        val bitmap=BitmapFactory.decodeFile(actual!!.path,BitmapFactory.Options().apply{inSampleSize=factor}) ?: error(tr("Image non décodable", "Image could not be decoded"))
                        val dhash=try{HashUtils.computeDHash(bitmap)}finally{bitmap.recycle()}
                        val accepted=sample.copy(localImagePath=actual!!.path,imageWidth=meta.width,imageHeight=meta.height,
                            sha256=HashUtils.computeSha256(actual!!),phash=dhash,sourceSha256=sourceHash,imageTransform=transform,acquisitionStatus="AVAILABLE",auditReason=null)
                        val duplicate=ImageIdentity.accept(db,accepted,ImageIdentity.pixelSha256(actual!!))
                        if(duplicate!=null) {
                            check(actual!!.delete()) { tr("Nettoyage du doublon interrompu", "Duplicate cleanup interrupted") }
                            db.auditDao().insertLog(AuditLogEntity(sampleId=sample.sampleId,batchNumber=batchNumber,projectId=projectId,
                                action="DUPLICATE_SKIPPED",details=tr("Lot original ${duplicate.firstBatchNumber}; cas ${duplicate.firstSampleId}", "Original batch ${duplicate.firstBatchNumber}; sample ${duplicate.firstSampleId}")))
                        }
                        }
                    } catch(e:CancellationException) {
                        withContext(NonCancellable){if(db.sampleDao().getSampleSync(sample.sampleId)?.annotationStatus!="DUPLICATE")db.sampleDao().updateSample(sample.copy(acquisitionStatus="ERROR_RETRYABLE",auditReason=tr("Opération arrêtée; reprise disponible", "Operation stopped; can resume")))}
                        throw e
                    } catch(e:Exception) {
                        failures.incrementAndGet();actual?.delete();storageManager.getImageFile(sample.sampleId).delete()
                        if(db.sampleDao().getSampleSync(sample.sampleId)?.annotationStatus!="DUPLICATE")db.sampleDao().updateSample(sample.copy(acquisitionStatus="ERROR_RETRYABLE",auditReason=e.message ?: tr("Acquisition interrompue", "Acquisition interrupted")))
                    } finally { synchronized(budgetLock){reserved-=reservation};onProgress(completed.incrementAndGet(),samples.size) }
                }
            } }.awaitAll()
        }
        val batch=db.batchDao().getBatchSync(projectId,batchNumber) ?: error(tr("Lot absent", "Batch not found"))
        val unique=db.sampleDao().getSamplesForBatchSync(projectId,batchNumber).count{it.annotationStatus!="DUPLICATE"}
        db.batchDao().updateBatch(batch.copy(status="READY",totalCases=unique))
        failures.get()==0
    } }

    /** Fills slots with unique images. Network failures stay visible and retryable, never skipped. */
    suspend fun prepareUniqueBatch(projectId:Long,batchNumber:Int,count:Int,onProgress:(Int,Int)->Unit):Boolean = preparationMutex.withLock {
        require(count in 1..1000)
        var exhausted=false
        var allAcquired=true
        // Bound one action for duplicate-heavy sources; cursor and exclusions persist for the next retry.
        repeat(20) {
            val existing=db.batchDao().getBatchSync(projectId,batchNumber)
            if(existing!=null) {
                allAcquired=acquireBatchImages(projectId,batchNumber,onProgress)
                val size=db.sampleDao().getSamplesForBatchSync(projectId,batchNumber).count{it.annotationStatus!="DUPLICATE"}
                if(size>=count || !allAcquired || exhausted) return@withLock exhausted
            }
            val size=db.sampleDao().getSamplesForBatchSync(projectId,batchNumber).count{it.annotationStatus!="DUPLICATE"}
            val project=db.projectDao().getProjectSync(projectId) ?: error(tr("Projet absent", "Project not found"))
            val discovered=discoverViewerBatch(project,batchNumber,count-size,append=existing!=null)
            check(discovered.success){discovered.error ?: tr("Import interrompu", "Import interrupted")}
            if(discovered.totalDiscovered==0 && !discovered.endOfSource) {
                if(existing==null) error(tr("Aucune image exploitable dans la fenêtre de source parcourue. Le curseur a avancé; relancez l’import pour poursuivre ou vérifiez la colonne image.",
                    "No usable image in the scanned source window. The cursor advanced; retry to continue or check the image column."))
                acquireBatchImages(projectId,batchNumber,onProgress)
                return@withLock false
            }
            if(discovered.endOfSource) {
                exhausted=true
                check(existing!=null){tr("Fin de la source : aucune nouvelle image", "End of source: no new images")}
                if(size==0)db.batchDao().updateStatus(projectId,batchNumber,"EMPTY")
                return@withLock true
            }
        }
        // Last appended window must be acquired before exposing its images.
        val finalBatch=db.batchDao().getBatchSync(projectId,batchNumber)
        if(finalBatch!=null) acquireBatchImages(projectId,batchNumber,onProgress)
        else error(tr("Aucune image exploitable trouvée dans les fenêtres de source parcourues. Vérifiez la colonne image ou relancez pour poursuivre le scan.",
            "No usable image was found in the scanned source windows. Check the image column or retry to continue scanning."))
        exhausted
    }

    suspend fun requireUniqueExport(samples:List<SampleEntity>)=ImageIdentity.requireCanonical(db,samples)

    /**
     * Executes LiteRT pre-annotations on all available images in the batch if configured.
     */
    /** Processes one bounded bitmap at a time. Existing records (including imported or empty
     * records) are untouched by default, regardless of stale PENDING status. Replacement
     * is reserved for an explicit user action; human work and final decisions stay protected. */
    suspend fun runBatchInference(
        projectId: Long,
        batchNumber: Int,
        config: ModelConfig,
        replaceExistingProposals: Boolean = false,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.Default) {
        val projectPolicy=db.projectDao().getProjectSync(projectId) ?: error(tr("Projet absent", "Project not found"))
        // This guard deliberately precedes sample selection and every mutation. UI, automatic
        // preparation and workflows therefore share exactly the same fail-closed behaviour.
        ModelContract.requireTaskCompatibility(config,projectPolicy.activeTasksCsv)
        check(db.batchDao().getBatchSync(projectId, batchNumber)?.status !in PublicationSafety.lockedStates) {
            tr("Ce lot est verrouillé par sa publication.", "This batch is locked by publication.")
        }
        val samples = db.sampleDao().getSamplesForBatchSync(projectId, batchNumber).filter {
            com.unicornwhodev.visiondatasetstudio.core.workflow.StudioWorkflow.canEdit(it.acquisitionStatus, it.syncStatus, it.localImagePath != null) &&
                (if(replaceExistingProposals) com.unicornwhodev.visiondatasetstudio.core.workflow.StudioWorkflow.isPending(it.annotationStatus)
                else it.annotationStatus=="PENDING" && db.annotationDao().getAnnotationSync(it.sampleId)==null)
        }
        if (samples.isEmpty()) return@withContext 0
        com.unicornwhodev.visiondatasetstudio.domain.inference.ModelClassCompatibility.requireAssistance(config, projectPolicy.activeTasksCsv, projectPolicy.classesCsv)
        val ledger=if(ProjectSettings.read(projectPolicy).adaptiveCorrection)AdaptiveCorrectionStore(storageManager.context).read(projectId) else CorrectionLedger()
        var processed = 0
        onProgress(0, samples.size)
        for (sample in samples) {
            coroutineContext.ensureActive()
            val recordBefore=db.annotationDao().getAnnotationSync(sample.sampleId)
            if(!replaceExistingProposals && recordBefore!=null) continue
            val path = sample.localImagePath ?: continue
            check(File(path).isFile) { tr("Image locale absente : ${sample.assetId}", "Local image missing: ${sample.assetId}") }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            check(bounds.outWidth > 0 && bounds.outHeight > 0) { tr("Image non décodable : ${sample.assetId}", "Image could not be decoded: ${sample.assetId}") }
            var factor = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / factor > 2048) factor *= 2
            val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = factor })
                ?: error(tr("Image non décodable : ${sample.assetId}", "Image could not be decoded: ${sample.assetId}"))
            val inference = try { liteRtEngine.runInference(bitmap, config) } finally { bitmap.recycle() }
            InferenceReceiptStore(storageManager.context.filesDir).write(projectId,sample.sampleId,batchNumber,inference)
            val proposals = AdaptiveCorrection.apply(when(inference) {
                is InferenceResult.Failure -> error(tr("Inférence interrompue à ${sample.assetId} : ${inference.error}. Les résultats précédents sont conservés.", "Inference interrupted at ${sample.assetId}: ${inference.error}. Previous results are preserved."))
                else -> inference.orThrow()
            },ledger)
            liteRtEngine.lastEmbedding?.let { vector ->
                com.unicornwhodev.visiondatasetstudio.domain.inference.EmbeddingIndex(storageManager.context,projectId).put(sample.sampleId,sample.sha256 ?: com.unicornwhodev.visiondatasetstudio.core.geometry.HashUtils.computeSha256(File(path)),liteRtEngine.embeddingSpaceHash,vector)
            }
            val existing = getSampleAnnotations(sample.sampleId)
            val project=db.projectDao().getProjectSync(projectId) ?: error(tr("Projet absent", "Project not found"))
            val selectedProposals=com.unicornwhodev.visiondatasetstudio.domain.inference.ModelClassCompatibility.forProject(proposals,config,project.classesCsv)
            val updated=ProposalMerger.merge(existing,selectedProposals,project.activeTasksCsv,config.captionLanguage,ModelContract.compatibility(config,project.activeTasksCsv).usableOutputs)
            val status=HumanAnnotationReview.pendingStatus(updated)
            val applied=db.withTransaction {
                // A correction or decision made while inference was running wins over its result.
                if(db.sampleDao().getSampleSync(sample.sampleId)!=sample ||
                    db.annotationDao().getAnnotationSync(sample.sampleId)!=recordBefore) return@withTransaction false
                saveSampleAnnotations(sample.sampleId, updated)
                db.sampleDao().updateSample(sample.copy(annotationStatus = status, updatedAt = System.currentTimeMillis()))
                db.auditDao().insertLog(AuditLogEntity(sampleId = sample.sampleId, batchNumber = batchNumber,
                    projectId=projectId, action = "MODEL_PREANNOTATION", details = tr("${proposals.size} proposition(s); aucune validation automatique.", "${proposals.size} proposal(s); no automatic approval.")))
                true
            }
            if(!applied) continue
            processed++
            onProgress(processed, samples.size)
        }
        processed
    }

    /**
     * Saves user modifications for a single sample.
     */
    suspend fun saveSampleAnnotations(sampleId: String, annotations: SampleAnnotations) = withContext(Dispatchers.IO) {
        val s=db.sampleDao().getSampleSync(sampleId) ?: error(tr("Cas absent", "Sample not found"))
        check(db.batchDao().getBatchSync(s.projectId,s.batchNumber)?.status !in PublicationSafety.lockedStates) { tr("Lot verrouillé : annotations conservées inchangées", "Batch locked: annotations preserved unchanged") }
        db.annotationDao().insertOrReplace(
            AnnotationRecord(
                sampleId = sampleId,
                dataJson = annotAdapter.toJson(annotations)
            )
        )
    }

    suspend fun getSampleAnnotations(sampleId: String): SampleAnnotations = withContext(Dispatchers.IO) {
        val record = db.annotationDao().getAnnotationSync(sampleId)
        if (record != null) {
            annotAdapter.fromJson(record.dataJson) ?: SampleAnnotations()
        } else {
            SampleAnnotations()
        }
    }

    suspend fun validateSample(sampleId: String, batchNumber: Int) = withContext(Dispatchers.IO) {
        val sample = db.sampleDao().getSampleSync(sampleId) ?: return@withContext
        check(db.batchDao().getBatchSync(sample.projectId,sample.batchNumber)?.status !in PublicationSafety.lockedStates) { tr("Lot verrouillé; décision conservée", "Batch locked; decision preserved") }
        db.sampleDao().updateSample(
            sample.copy(
                annotationStatus = AnnotationStatus.VALIDATED.name,
                updatedAt = System.currentTimeMillis()
            )
        )
        updateBatchCounts(sample.projectId, batchNumber)
        db.auditDao().insertLog(
            AuditLogEntity(sampleId = sampleId, batchNumber = batchNumber, projectId=sample.projectId, action = "VALIDATE", details = tr("Cas validé", "Sample approved"))
        )
    }

    suspend fun rejectSample(sampleId: String, batchNumber: Int, reason: String) = withContext(Dispatchers.IO) {
        val sample = db.sampleDao().getSampleSync(sampleId) ?: return@withContext
        check(db.batchDao().getBatchSync(sample.projectId,sample.batchNumber)?.status !in PublicationSafety.lockedStates) { tr("Lot verrouillé; décision conservée", "Batch locked; decision preserved") }
        db.sampleDao().updateSample(
            sample.copy(
                annotationStatus = AnnotationStatus.REJECTED.name,
                auditReason = reason,
                updatedAt = System.currentTimeMillis()
            )
        )
        updateBatchCounts(sample.projectId, batchNumber)
        db.auditDao().insertLog(
            AuditLogEntity(sampleId = sampleId, batchNumber = batchNumber, projectId=sample.projectId, action = "REJECT", details = tr("Motif: $reason", "Reason: $reason"))
        )
    }

    suspend fun deferSample(sampleId: String, batchNumber: Int) = withContext(Dispatchers.IO) {
        val sample = db.sampleDao().getSampleSync(sampleId) ?: return@withContext
        check(db.batchDao().getBatchSync(sample.projectId,sample.batchNumber)?.status !in PublicationSafety.lockedStates) { tr("Lot verrouillé; décision conservée", "Batch locked; decision preserved") }
        db.sampleDao().updateSample(
            sample.copy(
                annotationStatus = AnnotationStatus.DEFERRED.name,
                updatedAt = System.currentTimeMillis()
            )
        )
        updateBatchCounts(sample.projectId, batchNumber)
        db.auditDao().insertLog(
            AuditLogEntity(sampleId = sampleId, batchNumber = batchNumber, projectId=sample.projectId, action = "DEFER", details = tr("Cas différé", "Sample deferred"))
        )
    }

    private suspend fun updateBatchCounts(projectId: Long, batchNumber: Int) {
        val validated = db.sampleDao().countByAnnotationStatus(projectId, batchNumber, AnnotationStatus.VALIDATED.name)
        val rejected = db.sampleDao().countByAnnotationStatus(projectId, batchNumber, AnnotationStatus.REJECTED.name)
        val deferred = db.sampleDao().countByAnnotationStatus(projectId, batchNumber, AnnotationStatus.DEFERRED.name)

        val batch = db.batchDao().getBatchSync(projectId, batchNumber) ?: return
        db.batchDao().updateBatch(
            batch.copy(
                validatedCases = validated,
                rejectedCases = rejected,
                deferredCases = deferred,
                status = if (validated + rejected >= batch.totalCases) "VALIDATED" else "IN_PROGRESS",
                updatedAt = System.currentTimeMillis()
            )
        )
        if (validated + rejected >= batch.totalCases) {
            val project=db.projectDao().getProjectSync(projectId)
            if(project!=null) {
                val settings=ProjectSettings.read(project)
                if(settings.collaborationEnabled) {
                    runCatching { workClaims.markDone(project,settings,db.sampleDao().getSamplesForBatchSync(projectId,batchNumber)) }
                        .onFailure { db.auditDao().insertLog(AuditLogEntity(sampleId=null,batchNumber=batchNumber,projectId=projectId,action="COORDINATION_SYNC_FAILED",details=it.message ?: tr("Erreur inconnue", "Unknown error"))) }
                }
            }
        }
    }

    suspend fun snapshot(projectId:Long,batchNumber:Int):String = BatchSnapshot.compute(db,projectId,batchNumber)
    suspend fun recordLocalArchive(projectId:Long,batchNumber:Int,file:File) {
        val b=db.batchDao().getBatchSync(projectId,batchNumber) ?: error(tr("Lot absent", "Batch not found"))
        check(b.status !in PublicationSafety.lockedStates) { tr("Lot verrouillé", "Batch locked") }
        db.batchDao().updateBatch(b.copy(archivePath=file.path,archiveSizeBytes=file.length(),archiveSnapshot=snapshot(projectId,batchNumber),verifiedArchiveUri=null,verifiedArchiveSha256=null,verificationKind=null))
    }
    suspend fun verifyLocalArchive(projectId:Long,batchNumber:Int,uri:String) = withContext(Dispatchers.IO) {
        val b=db.batchDao().getBatchSync(projectId,batchNumber) ?: error(tr("Lot absent", "Batch not found"))
        check(b.status !in setOf("PREPARED","PUBLISHING","PUBLISHED","CONFLICT","PURGING","PURGED")){tr("Terminez le transfert incertain avant de clôturer une copie locale", "Complete the uncertain transfer before closing a local copy")}
        check(b.archiveSnapshot==snapshot(projectId,batchNumber)){tr("Les annotations ont changé depuis l’export. Préparez une nouvelle archive.", "Annotations changed since export. Prepare a new archive.")}
        val file=b.archivePath?.let(::File) ?: error(tr("Archive absente", "Archive missing"))
        check(file.isFile)
        val expected=HashUtils.computeSha256(file)
        check(hashUri(uri,file.length())==expected) { tr("La copie externe ne correspond pas à l’archive", "The external copy does not match the archive") }
        val samples=db.sampleDao().getSamplesForBatchSync(projectId,batchNumber)
        check(samples.all{it.annotationStatus in setOf("VALIDATED","REJECTED","DUPLICATE")}) { tr("Copie vérifiée, mais lot incomplet : terminez les cas avant de clôturer le lot", "Copy verified, but batch incomplete: finish all samples before closing the batch") }
        db.withTransaction {
            db.batchDao().updateBatch(b.copy(status="VERIFIED",verificationKind=if(b.hfCommitSha==null)"local" else "both",verifiedArchiveUri=uri,verifiedArchiveSha256=expected,archiveSizeBytes=file.length()))
            samples.filter{it.annotationStatus=="VALIDATED"}.forEach{db.sampleDao().updateSample(it.copy(syncStatus="VERIFIED"))}
            db.auditDao().insertLog(AuditLogEntity(sampleId=null,batchNumber=batchNumber,action="LOCAL_ARCHIVE_VERIFIED",details=tr("Archive relue et SHA-256 comparé; URI conservée en base, pas dans l’export", "Archive read back and SHA-256 compared; URI stored in the database, not the export"),projectId=projectId))
        }
    }
    private fun hashUri(uri:String,maxBytes:Long):String {
        check(maxBytes > 0) { tr("Taille de copie inconnue; préparez et vérifiez à nouveau une archive", "Unknown copy size; prepare and verify an archive again") }
        return storageManager.context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use { input ->
            DurableFiles.hash(input,maxBytes)
        } ?: error(tr("La copie externe n’est plus accessible; purge refusée", "The external copy is no longer accessible; cleanup refused"))
    }
    private val receiptAdapter = moshi.adapter(RemoteReceipt::class.java)
    private fun receipt(files:List<Pair<String,File>>) = RemoteReceipt(files=files.map{(path,file)->
        RemoteFileDigest(path,file.length(),HashUtils.computeSha256(file))
    })
    private fun manifestHash(root:File):String {
        check(exporters.verifyPreparedPackage(root)) { tr("Paquet incomplet ou altéré; aucune reconstruction automatique d’un envoi incertain", "Package incomplete or modified; an uncertain upload is never rebuilt automatically") }
        return HashUtils.computeSha256(root.walkTopDown().filter{it.isFile && it.name=="manifest.json"}.single())
    }
    private fun remoteFiles(projectId:Long,batchNumber:Int,prefix:String):List<Pair<String,File>> {
        val root=storageManager.batchExportDir(projectId,batchNumber)
        return root.walkTopDown().filter{it.isFile}.map{"$prefix/${it.relativeTo(root).invariantSeparatorsPath}" to it}.sortedBy{it.first}.toList()
    }
    /** A read-only recovery path for a failed or uncertain upload. Does not authorize cleanup. */
    suspend fun recoverPendingArchive(projectId: Long, batchNumber: Int): File = withContext(Dispatchers.IO) {
        val project = db.projectDao().getProjectSync(projectId) ?: error(tr("Projet absent", "Project not found"))
        val batch = db.batchDao().getBatchSync(projectId, batchNumber) ?: error(tr("Lot absent", "Batch not found"))
        check(batch.status in setOf("PREPARED", "PUBLISHING", "PUBLISHED", "CONFLICT"))
        check(batch.archiveSnapshot == snapshot(projectId, batchNumber)) { tr("Annotations modifiées après préparation.", "Annotations changed after preparation.") }
        val root = storageManager.batchExportDir(projectId, batchNumber)
        check(batch.preparedManifestSha256 == manifestHash(root)) { tr("Manifest modifié après préparation", "Manifest changed after preparation") }
        val prefix = requireNotNull(batch.remotePrefix)
        val recorded = batch.remoteReceiptJson?.let(receiptAdapter::fromJson) ?: error(tr("Reçu des fichiers absent", "Missing file receipt"))
        check(receipt(remoteFiles(projectId, batchNumber, prefix)) == recorded) { tr("Contenus modifiés après préparation", "Contents changed after preparation") }
        exporters.recoveryZip(project, batchNumber)
    }

    /** Durable upload intent. A lost response never changes parent, repo, paths or bytes. */
    suspend fun publishAndVerifyBatch(projectId:Long,batchNumber:Int):BatchPublishResult = withContext(Dispatchers.IO) {
        val project=db.projectDao().getProjectSync(projectId) ?: return@withContext BatchPublishResult(false,tr("Projet absent", "Project not found"))
        var batch=db.batchDao().getBatchSync(projectId,batchNumber) ?: return@withContext BatchPublishResult(false,tr("Lot absent", "Batch not found"))
        try {
            val settings=ProjectSettings.read(project); checkNetwork(settings); hfApiClient.configureTimeout(settings.timeoutSeconds)
            val samples=db.sampleDao().getSamplesForBatchSync(projectId,batchNumber)
            check(samples.isNotEmpty() && samples.all{it.annotationStatus in setOf("VALIDATED","REJECTED","DUPLICATE")}) { tr("Terminez ou rejetez chaque cas avant publication", "Finish or reject every sample before publication") }
            requireUniqueExport(samples.filter{it.annotationStatus=="VALIDATED"})
            if(settings.collaborationEnabled) workClaims.markDone(project,settings,samples)
            check(batch.status !in setOf("PURGING","PURGED")) { tr("Les médias de ce lot sont en cours de purge ou déjà purgés", "This batch's media is being purged or already purged") }
            val pairs=samples.filter{it.annotationStatus=="VALIDATED"}.map{it to getSampleAnnotations(it.sampleId)}
            check(pairs.isNotEmpty()){ tr("Aucun cas validé à publier", "No approved samples to publish") }
            val currentSnapshot=snapshot(projectId,batchNumber)
            val root=storageManager.batchExportDir(projectId,batchNumber)
            if(batch.remoteParentCommit==null && batch.hfCommitSha==null) {
                check(batch.status !in setOf("PUBLISHING","PREPARED","CONFLICT")) { tr("Ancien transfert sans parent connu. Choisissez explicitement un nouvel emplacement isolé.", "Old transfer with unknown parent. Explicitly choose a new isolated location.") }
                if(batch.verificationKind=="local") check(batch.archiveSnapshot==currentSnapshot) { tr("Copie locale obsolète", "Local copy outdated") }
                val repo=com.unicornwhodev.visiondatasetstudio.core.workflow.StudioWorkflow.normalizeRepo(project.hfDestRepo,destination=true) ?: error(tr("Destination HF invalide", "Invalid HF destination"))
                val prefix=batch.remotePrefix ?: "${settings.destPrefix}/${storageManager.publicationNamespace(projectId)}"
                val packaged=exporters.packageBatchForHf(project,batchNumber,pairs,settings.hfWebDataset,true,settings.hfCoco,settings.hfYolo,settings.hfVl)
                check(packaged.success){packaged.error ?: tr("Préparation impossible", "Preparation unavailable")}
                val files=remoteFiles(projectId,batchNumber,prefix)
                val parent=hfApiClient.resolveRevision(repo,settings.destBranch)
                hfApiClient.requirePathsAbsent(repo,parent,files.map{it.first})
                batch=batch.copy(status="PREPARED",remotePrefix=prefix,remoteBranch=settings.destBranch,remoteRepoId=repo,
                    remoteParentCommit=parent,archiveSnapshot=currentSnapshot,preparedManifestSha256=manifestHash(root),
                    remoteReceiptJson=receiptAdapter.toJson(receipt(files)),lastTransferError=null)
                // Commit intent before the first external write (including LFS).
                db.batchDao().updateBatch(batch)
            }
            check(batch.archiveSnapshot==currentSnapshot) { tr("Annotations modifiées après préparation; transfert suspendu", "Annotations changed after preparation; transfer suspended") }
            val repo=batch.remoteRepoId ?: error(tr("Reçu ancien sans dépôt épinglé; vérification manuelle nécessaire", "Old receipt without a pinned repository; manual verification required"))
            val prefix=batch.remotePrefix ?: error(tr("Préfixe absent", "Missing prefix"))
            val expected=batch.remoteParentCommit
            val recorded=batch.remoteReceiptJson?.let(receiptAdapter::fromJson) ?: error(tr("Reçu des fichiers absent", "Missing file receipt"))
            check(batch.preparedManifestSha256==manifestHash(root)) { tr("Manifest modifié après préparation", "Manifest changed after preparation") }
            val files=remoteFiles(projectId,batchNumber,prefix)
            check(receipt(files)==recorded) { tr("Contenus modifiés après préparation", "Contents changed after preparation") }
            val knownCommit=batch.hfCommitSha
            val sha=if(knownCommit!=null) knownCommit else {
                check(expected!=null) { tr("Parent absent", "Parent missing") }
                val head=hfApiClient.resolveRevision(repo,batch.remoteBranch ?: error(tr("Branche absente", "Branch missing")))
                when(PublicationSafety.decide(expected,head,hfApiClient.verifyRemoteDigests(repo,head,recorded.files))) {
                    ResumeDecision.COMMITTED -> head
                    ResumeDecision.CONFLICT -> {
                        batch=batch.copy(status="CONFLICT",lastTransferError=tr("La branche a changé; aucun rebase ou écrasement automatique", "The branch changed; no automatic rebase or overwrite"))
                        db.batchDao().updateBatch(batch);error(batch.lastTransferError!!)
                    }
                    ResumeDecision.RETRY_SAME_PARENT -> {
                        batch=batch.copy(status="PUBLISHING",lastTransferError=null);db.batchDao().updateBatch(batch)
                        val result=hfApiClient.uploadBatchFiles(repo,batch.remoteBranch!!,
                            "Studio project $projectId / batch $batchNumber (${pairs.size} reviewed)",files,expected)
                        if(result.conflict) { batch=batch.copy(status="CONFLICT",lastTransferError=result.message);db.batchDao().updateBatch(batch) }
                        check(result.success){result.message}
                        result.commitSha ?: error(tr("Réponse sans SHA; relancez la réconciliation", "Response has no SHA; run reconciliation again"))
                    }
                }
            }
            batch=batch.copy(status="PUBLISHED",hfCommitSha=sha,lastTransferError=null)
            // Persist receipt even if the following remote reads fail or process dies.
            db.batchDao().updateBatch(batch)
            check(hfApiClient.verifyRemoteDigests(repo,sha,recorded.files)){ tr("Commit reçu; vérification incomplète. Les copies locales restent conservées.", "Commit received; verification incomplete. Local copies are preserved.") }
            db.withTransaction {
                db.batchDao().updateBatch(batch.copy(status="VERIFIED",verificationKind=if(batch.verificationKind in setOf("local","both"))"both" else "hf"))
                pairs.forEach{(sample,_)->db.sampleDao().updateSample(sample.copy(syncStatus="VERIFIED"))}
                db.auditDao().insertLog(AuditLogEntity(sampleId=null,batchNumber=batchNumber,action="REMOTE_CONTENT_VERIFIED",details=tr("SHA $sha; purge distincte", "SHA $sha; cleanup separate"),projectId=projectId))
            }
            BatchPublishResult(true,tr("Contenus vérifiés. Conservation ou purge explicite disponible.", "Contents verified. Explicit retention or cleanup is available."),sha)
        } catch(e:CancellationException) {
            withContext(NonCancellable){ db.batchDao().getBatchSync(projectId,batchNumber)?.let {
                db.batchDao().updateBatch(it.copy(lastTransferError=tr("Interruption; réconciliation de l’intention persistée requise", "Interrupted; the persisted intent needs reconciliation")))
            } };throw e
        } catch(e:Exception) {
            db.batchDao().getBatchSync(projectId,batchNumber)?.let { db.batchDao().updateBatch(it.copy(lastTransferError=e.message?.take(1000))) }
            BatchPublishResult(false,e.message ?: tr("Transfert interrompu; copies conservées", "Transfer interrupted; copies preserved"))
        }
    }

    /** Explicit conflict recovery changes only the NEW destination; never deletes or overwrites the old one. */
    suspend fun isolateConflictedUpload(projectId:Long,batchNumber:Int) = withContext(Dispatchers.IO) {
        val batch=db.batchDao().getBatchSync(projectId,batchNumber) ?: error(tr("Lot absent", "Batch not found"))
        check(batch.status=="CONFLICT" && batch.hfCommitSha==null) { tr("Seul un conflit sans commit confirmé peut être réémis", "Only a conflict without a confirmed commit can be retried") }
        val project=db.projectDao().getProjectSync(projectId) ?: error(tr("Projet absent", "Project not found"))
        val prefix="${ProjectSettings.read(project).destPrefix}/${storageManager.publicationNamespace(projectId)}/retry-${UUID.randomUUID()}"
        db.withTransaction {
            db.auditDao().insertLog(AuditLogEntity(sampleId=null,batchNumber=batchNumber,projectId=projectId,
                action="CONFLICT_ISOLATED",details=tr("Ancienne intention conservée pour audit (aucun nettoyage distant): repo=${batch.remoteRepoId}; branch=${batch.remoteBranch}; parent=${batch.remoteParentCommit}; prefix=${batch.remotePrefix}", "Previous intent preserved for audit (no remote cleanup): repo=${batch.remoteRepoId}; branch=${batch.remoteBranch}; parent=${batch.remoteParentCommit}; prefix=${batch.remotePrefix}")))
            db.batchDao().updateBatch(batch.copy(status="VALIDATED",remoteRepoId=null,remoteParentCommit=null,remoteBranch=null,
                remotePrefix=prefix,preparedManifestSha256=null,remoteReceiptJson=null,lastTransferError=null))
        }
    }
    suspend fun closeRejectedBatch(projectId:Long,batchNumber:Int) {
        val batch=db.batchDao().getBatchSync(projectId,batchNumber) ?: error(tr("Lot absent", "Batch not found"))
        check(batch.status !in PublicationSafety.lockedStates) { tr("Lot déjà clôturé ou en transfert", "Batch already closed or being transferred") }
        val samples=db.sampleDao().getSamplesForBatchSync(projectId,batchNumber)
        check(samples.isNotEmpty() && samples.all{it.annotationStatus in setOf("REJECTED","DUPLICATE")}) { tr("La clôture sans copie est réservée aux lots intégralement rejetés explicitement", "Closing without a copy is reserved for batches where every sample was explicitly rejected") }
        db.withTransaction {
            db.batchDao().updateBatch(batch.copy(status="VERIFIED",verificationKind="rejection_only",archiveSnapshot=snapshot(projectId,batchNumber)))
            db.auditDao().insertLog(AuditLogEntity(sampleId=null,batchNumber=batchNumber,action="REJECTED_BATCH_CLOSED",details=tr("Tous les cas ont été rejetés explicitement. Aucune donnée acceptée à sauvegarder; purge distincte.", "Every sample was explicitly rejected. No accepted data to save; cleanup remains separate."),projectId=projectId))
        }
    }
    suspend fun purgeReviewedBatch(projectId:Long,batchNumber:Int,discardRejectedConfirmed:Boolean):Int = withContext(Dispatchers.IO) {
        val batch=db.batchDao().getBatchSync(projectId,batchNumber) ?: error(tr("Lot absent", "Batch not found"))
        val samples=db.sampleDao().getSamplesForBatchSync(projectId,batchNumber)
        check(batch.status in setOf("VERIFIED","PURGING")) { tr("Copie vérifiée requise", "A verified copy is required") }
        val project=db.projectDao().getProjectSync(projectId) ?: error(tr("Projet absent", "Project not found"))
        val learning=com.unicornwhodev.visiondatasetstudio.domain.training.OnDeviceTraining(storageManager.context)
        learning.requireCleanupAllowed(project,batchNumber,samples.any{it.annotationStatus=="VALIDATED"},batch.archiveSnapshot)
        check(batch.archiveSnapshot!=null && batch.archiveSnapshot==snapshot(projectId,batchNumber)) { tr("Copie obsolète; purge refusée", "Copy outdated; cleanup refused") }
        check(samples.all{(it.annotationStatus=="VALIDATED" && it.syncStatus in setOf("VERIFIED","PURGED")) || (it.annotationStatus=="REJECTED" && discardRejectedConfirmed) || it.annotationStatus=="DUPLICATE"}) { tr("Des cas restent à traiter", "Some samples remain unfinished") }
        // A corrupt/untrusted DB path must not delete any source, model or other project's file.
        for(sample in samples) sample.localImagePath?.let { path ->
            val owned=storageManager.ownedImage(path)
            check(owned.name in listOf("jpg","png","webp").map{storageManager.getImageFile(sample.sampleId,it).name}) { tr("Chemin média étranger au cas", "Media path does not belong to this sample") }
        }
        var proofValid=batch.verificationKind=="rejection_only" && samples.all{it.annotationStatus in setOf("REJECTED","DUPLICATE")}
        if(batch.verificationKind in setOf("local","both")) {
            proofValid=try {
                val limit=batch.archiveSizeBytes ?: batch.archivePath?.let(::File)?.takeIf{it.isFile}?.length() ?: 0
                hashUri(batch.verifiedArchiveUri ?: error(tr("URI absente", "URI missing")),limit)==batch.verifiedArchiveSha256
            } catch(e:CancellationException){throw e} catch(_:Exception){false}
        }
        if(!proofValid && batch.verificationKind in setOf("hf","both")) {
            val repo=batch.remoteRepoId ?: error(tr("Ancien reçu sans dépôt vérifiable; originaux conservés", "Old receipt without a verifiable repository; originals preserved"))
            val commit=batch.hfCommitSha ?: error(tr("Commit absent", "Commit missing"))
            val record=batch.remoteReceiptJson?.let(receiptAdapter::fromJson) ?: error(tr("Reçu distant absent", "Remote receipt missing"))
            proofValid=hfApiClient.verifyRemoteDigests(repo,commit,record.files)
        }
        check(proofValid) { tr("Copie de sauvegarde indisponible ou altérée; purge refusée", "Backup copy unavailable or modified; cleanup refused") }
        db.batchDao().updateStatus(projectId,batchNumber,"PURGING")
        var deleted=0
        for(sample in samples) {
            coroutineContext.ensureActive()
            val file=sample.localImagePath?.let(storageManager::ownedImage)
            if(file?.exists()==true) { check(file.delete()) { tr("Suppression impossible; reprise de purge disponible", "Deletion failed; cleanup can resume") };deleted++ }
            storageManager.removeImageCheckpoints(sample.sampleId)
            db.sampleDao().updateSample(sample.copy(syncStatus="PURGED",localImagePath=null))
        }
        val root=storageManager.batchExportDir(projectId,batchNumber)
        val paths=mutableListOf(root,storageManager.batchArchiveFile(projectId,batchNumber),File(root.path+".building"),File(root.path+".previous"))
        if(projectId==1L) { val legacy="batch-%06d".format(java.util.Locale.US,batchNumber)
            paths+=File(storageManager.exportsDir,legacy);paths+=File(storageManager.exportsDir,"$legacy.zip") }
        for(path in paths) {
            val owned=DurableFiles.ownedFile(storageManager.exportsDir,path.path)
            check(!owned.exists() || owned.deleteRecursively()) { tr("Nettoyage incomplet; relancez la purge", "Cleanup incomplete; retry cleanup") }
        }
        learning.releaseBatchImages(projectId,batchNumber)
        // PURGED only after ALL deletions succeed. Repeated calls are safe after a crash between steps.
        db.withTransaction {
            db.batchDao().updateStatus(projectId,batchNumber,"PURGED")
            db.auditDao().insertLog(AuditLogEntity(sampleId=null,batchNumber=batchNumber,action="EXPLICIT_PURGE",details=tr("$deleted copies cache supprimées; sources intactes; reçus et annotations conservés", "$deleted cached copies deleted; sources intact; receipts and annotations preserved"),projectId=projectId))
        }
        deleted
    }




}

data class BatchDiscoveryResult(
    val success: Boolean,
    val totalDiscovered: Int = 0,
    val error: String? = null,
    val endOfSource: Boolean = false
)

object SourceWindowProgress {
    fun advance(startOffset:Long,page:com.unicornwhodev.visiondatasetstudio.data.source.SourcePageResult,validInspected:Int):Int {
        require(validInspected in 0..page.entries.size)
        if(page.consumed==0)return 0
        if(validInspected==0)return page.consumed
        val ordinal=page.entries[validInspected-1].ordinal
        return ((ordinal-startOffset)+1).toInt().coerceIn(1,page.consumed)
    }
    fun rejectedInPrefix(advance:Int,validInspected:Int):Int=(advance-validInspected).coerceAtLeast(0)
}

data class BatchPublishResult(
    val success: Boolean,
    val message: String,
    val commitSha: String? = null
)
