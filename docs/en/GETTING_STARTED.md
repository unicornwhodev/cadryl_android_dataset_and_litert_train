# Your first batch with Cadryl

[Documentation](README.md) · [Français](../GETTING_STARTED.md)

Start with a few images and no model. Add AI assistance once you are comfortable with the workflow.

## Install Cadryl

Get **`vision-dataset-studio.apk`** from the [rc7 release](https://github.com/unicornwhodev/vision-dataset-studio/releases/tag/v4.2.0-rc7). This is the signed Release app for Android 9+ on ARM64. Model weights are downloaded separately.

Check the download against `SHA256SUMS`. In PowerShell:

```powershell
Get-FileHash ./vision-dataset-studio.apk -Algorithm SHA256
```

The qualification ZIP also contains test APKs and an x86_64 build. The Flex, Graphics Path and LiteRT ZIPs are build dependencies; you do not need them to install the app.

**Using rc6?** rc7 keeps its certificate and increases the version code to support updates. **Already have rc4 or rc5?** Those builds use different certificates. Keep their data and installation. [Durable signing key](../SIGNING.md). Phone acceptance for this candidate is still pending.

## 1. Create a project

Create a project through **Studio → Projects**, then open **Setup**. Follow three steps:

1. **Images**: choose a folder on this device or a Hugging Face dataset. For HF, inspect the source to check access and its image column.
2. **Task**: choose boxes, image labels or captions. Add the classes you need. With a selected model, search its classes and tap to add exact names. Names are not translated automatically.
3. **Review**: check the summary. Manual work with local images needs no model or HF account. Choose an output folder later through **Export**.

Each project keeps its own source, batches, annotations and history.

## 2. Prepare images

Index the source and prepare a batch. Exact copies already processed in the project are recognised even after cleanup. Crops and edited images may remain separate cases.

Retry failed acquisitions or exclude them with a reason. A failed download never counts as a reviewed image.

## 3. Add a model if it helps

In **Models**, import a compatible file or choose an authorised Hugging Face source. [Charlbi’s conversions](https://huggingface.co/Charlbi/Lite_rt_prepared_for_android_dataset_builder) have individual contracts and test results. Keep every required graph, processor and tokenizer in a bundle.

In **Models → Settings**, check supported tasks and classes, then try one image. Input dimensions are checked against the actual file before saving. Fixed vocabularies show matching classes; models with free text outputs cannot guarantee a closed vocabulary. TinyCLIP can use project classes as text candidates, while SAM needs a mask label.

Partial compatibility limits suggestions to supported classes; complete the others manually. Full incompatibility is reported before inference. Inference-only models are supported. Changing a prompt or setting does not rewrite saved annotations.

## 4. Review and correct

Adjust boxes, points and masks. Approve usable images and reject others with a reason. Model suggestions need your review. An image with no known annotation is not automatically a negative example.

## 5. Export and read back

Keep the **canonical JSONL** and required images. COCO, YOLO, WebDataset and vision-language exports cover different needs; see the [data schema](../../DATA_SCHEMA.md).

Choose formats in **Export**, then use **Destination & storage** for the folder or HF repository. If an annotated class is missing from the COCO or YOLO vocabulary, add it using the preflight action. Create the archive and wait for readback before cleanup.

For HF access failures or pending transfers, follow the diagnostic. You can recover a local copy of the frozen package if its files and receipt remain intact. This does not close the HF transfer or independently allow cleanup. Conflicts retain their parent commit until explicitly resolved.

## 6. Train a copy, optionally

Training starts disabled. It needs a compatible model and a reviewed batch with a verified export. The deterministic split must contain **at least 32 training and eight validation images**; 40 images in total do not automatically meet that split.

The first run creates a separate trained version. Later runs continue from its last validated weights even while the original is still selected for inference. Activating new weights remains your choice. [Versions and checkpoints](MODEL_LINEAGE.md).

## 7. Clean up and continue

Confirm cleanup after the copy is verified. Complete or explicitly abandon any unfinished training first. Receipts, duplicate history and referenced model versions remain available. Then prepare the next batch.

## If something gets stuck

| Problem | Check |
|---|---|
| Installation refused | Android version, storage and the existing signing certificate |
| Inference works but training does not | This variant’s contract and signatures; `_learning` variants are separate |
| Bundle fails to load | Missing graphs or supporting files |
| Training refused | Verified export, actual split sizes, contract and checkpoint |
| Export folder unavailable | Storage availability and Android document permission |
| Poor predictions | Classes, preprocessing and whether the model fits your images |
| No compatible classes | Use exact model class names, select another model or annotate manually |
| HF access refused | Authentication, repository permissions, access terms and write permission for publication |
| Viewer offers no usable image column | Use a local folder or JSONL manifest through advanced source options |

[Known limits](KNOWN_LIMITATIONS.md) · [Workflows](WORKFLOWS.md) · [Report a problem](https://github.com/unicornwhodev/vision-dataset-studio/issues).
