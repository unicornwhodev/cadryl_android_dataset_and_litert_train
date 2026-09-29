# Cadryl 4.2.0-rc8 — source/model stabilization candidate

RC8 is a corrective candidate built from rc7. It does **not** claim release qualification until the Android build, JVM tests and instrumented device suite have passed.

## Corrected in this candidate

### Dataset ingestion
- A Hugging Face Viewer response marked `partial` can now be inspected without being mistaken for an exhaustive source.
- For an unfiltered Viewer split with a row count from `/splits`, Cadryl stores that count and verifies every requested page: contiguous `row_idx`, no early end, and traversal up to the declared total. Existing rc7 projects backfill this count on first import.
- If that exhaustive page contract is unavailable, partial Viewer import is still refused by default. An explicit advanced opt-in allows processing only the exposed portion and is locked as part of source identity after the first batch.
- Hugging Face Viewer rows with truncated or unusable selected image cells no longer abort the whole dataset.
- Source paging now returns both valid entries and the number of upstream rows consumed, so the project cursor keeps exact provenance even when rows are rejected.
- Collaboration paging advances in upstream coordinates and does not silently loop over rejected rows.
- Retry logic refuses a Viewer row that became unusable instead of reusing stale provenance.
- Setup shows usable-image coverage for the preview instead of claiming that every preview row was verified.
- Local/HF source selection is resynchronised from persisted project settings.
- Starting an HF batch from guided setup now requires a preview with a usable selected image column; saving without starting remains possible.

### Model setup
- Raw LiteRT files remain conservative on import: unknown output semantics are never guessed.
- Fixed input shapes are copied from the graph; dynamic graphs retain only safe signature hints (layout, dtype and channels) without inventing spatial dimensions or preprocessing.
- A guided raw-model editor now lets the user select the documented adapter and enter the model's indexed class vocabulary without editing JSON.
- Model/project class compatibility is case- and whitespace-insensitive while preserving the original model labels and indices.
- Saving a configured model also updates the installed profile contract, preventing a later profile re-selection from restoring the old `inspect_only` contract.
- Community catalogs recognise common contract filenames such as `contract.json`, `model_contract.json` and `android_contract.json`.
- Local endpoint profiles can be shown as active even though they intentionally have no model file.
- Automatic preannotation now accepts configured local HTTP endpoints instead of silently requiring a LiteRT file path.

### Interface and review workflow
- Guided setup is now four focused steps: **Images → Annotations → AI model → Review**. Model selection no longer competes with task/class configuration on the same step.
- Home exposes two primary configuration destinations only: **Project setup** and **Models & presets**. The old direct task preset selector was removed from Home.
- The model screen is the single user-facing location for HF catalogs, installed models, configuration presets and manual import. Advanced controls no longer duplicate those selectors.
- The model catalog shows source, approximate LiteRT download size and per-source discovery failures.
- Correcting a model proposal changes it into a human correction (`human_correction`) while retaining model baseline coordinates for audit/adaptive correction.
- Final human validation changes every surviving annotation into a human-reviewed decision (`human_validated` unless already human/corrected).
- Batch, Home and editor surfaces distinguish **AI suggestion**, **manually handled · needs approval**, **manually corrected** and **manually reviewed** states across spatial and non-spatial annotations.

### FireViewer and model catalogs
- `fireviewer/litert-models` and `Charlbi/Lite_rt_prepared_for_android_dataset_builder` are built-in dynamic HF catalog sources; an additional user-defined HF source remains supported.
- The six currently documented FireViewer variants have friendly identities in the catalog, but remain `UNTESTED` in Cadryl until the RC8 Android qualification actually executes them.
- FireViewer installation consumes each published `android_model_config.json` rather than guessing adapters, labels, normalization or training signatures.
- Future single-graph models with supported contract files remain discoverable even when their IDs are not hard-coded.
- Base configuration presets now cover SSD, multiple YOLO layouts, RF-DETR, RTMDet, direct XYXY, points, heatmaps, single/multi-label classification, visual embeddings, TinyCLIP, EfficientViT-SAM, Florence-2 and local HTTP detection/caption/grounding/VQA/classification.
- Presets are templates, not automatic model recognition. The real graph input is still checked before saving when weights are active.

## Invariants kept
- Mesh/model weight bytes are never rewritten by configuration.
- Model label order is not automatically sorted or remapped.
- Unknown LiteRT output semantics are not inferred from filenames.
- Human annotations are not filtered by model vocabulary.
- Source rejection advances provenance only for rows actually scanned.

## Qualification still required
Before publishing rc8:
1. run JVM/Python tests;
2. assemble Debug and minified Release APKs;
3. run the Android core and UI suites;
4. exercise both a complete HF Viewer source and a `partial=true` source, verifying the default refusal and explicit partial opt-in;
5. run the four-step setup UI test and inspect the compact Home/Models navigation on phone width;
6. verify correction → human provenance and final validation across boxes, points, masks, tags, captions, VQA, grounding and counts;
7. import a raw detector, configure its adapter/classes, switch to another profile and back, then confirm the configured contract persists;
8. discover/install the FireViewer catalog and run real-image inference for each supported FireViewer variant that fits the test device;
9. run inference on a real image for each generic adapter/preset family affected by the change.

This document describes the candidate implementation, not a completed release qualification.

## Known boundary

RC8 does not add a native Parquet reader to Android. A large Parquet-backed HF dataset can still be traversed exhaustively through `/rows` when HF exposes continuous pages all the way to the `/splits` row count; Cadryl now verifies that invariant page by page. If HF cannot satisfy it and no exhaustive JSONL manifest exists, Cadryl either refuses the source (safe default) or processes only the exposed Viewer portion after explicit opt-in. The latter must not be described as a full-dataset import.
