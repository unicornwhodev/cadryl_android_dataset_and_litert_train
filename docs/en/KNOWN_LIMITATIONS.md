# What still needs work

[Project home](../../README.en.md) · [Français](../../KNOWN_LIMITATIONS.md)

Cadryl **4.2.0-rc8** is a prerelease. The [report](../RC8_QUALIFICATION_2026_09.md) records exact APK/device scopes.

## Devices and stability

Release core 53/53 passes on physical Honor/API 36/4 KB, Oppo/API 33/4 KB and native x86/API 36/16 KB. Independent UI 4/4 passes on Oppo and the emulator. Physical ARM 16 KB remains unavailable. Ten-minute RepViT M1 CPU inference on Honor passes on the published ARM APK, without an accuracy or background-endurance claim. Historical ART crashes retain no confirmed root cause.

Real authorized private HF publication/conflict/lost-response passes on Debug/AVD using identical production source files. Genuine SAF revocation and 1,000 CC0-image ingestion pass on Debug/Oppo; dedicated removable-volume loss passes on Debug/AVD. These are separate scopes and do not cover all storage providers, Viewer sources or background restrictions.

## Models and quality

Catalogue coverage is partial and needs another pass with the new runtime. The public Charlbi campaign of 22 September recorded **13 of 25 variants passing**, including eight trainable variants, one timeout and eleven not run. These are execution results, not accuracy measurements.

The supplied trainable conversions adjust output heads or adapters with a frozen encoder. The internal-layer synthetic test is separate. Accuracy, error patterns, forgetting, RAM and latency still need measurement on representative data and ARM hardware.

Some bundles need multiple graphs, processors and tokenizers. A single `.tflite` file is not a replacement for a complete bundle.

Class checks use the active contract and its declared vocabulary. They do not measure accuracy or validate unknown labels in a raw model file. Free text models still need an image trial.

## Data and transfers

rc7 adds input shape checks, actionable HF diagnostics and verified recovery of a frozen export. It cannot grant repository access or bypass data protection checks.

Earlier campaigns injected real SAF, storage, HF and cleanup failures. Coverage does not include every storage provider, large multipart transfer or interruption. A real local agent server and some cloud integrations are also unqualified.

Duplicate tracking covers identical files or decoded pixels inside one project, not every crop or lossy recompression. **A dataset export is not a full project backup.** Room migrations do not recover another application’s data or an uninstalled app’s private storage.

## Distribution

rc8 uses the durable signing key. Differently signed rc4/rc5 Debug installations cannot be updated directly; preserve their data. The current key and backup have been checked on two disks in the same PC. An off-machine backup is still needed.

Remote CI is prepared but has not run. The 109-dependency inventory is available; native transitive notice review remains open. [Signing](../SIGNING.md) · [Licensing](../../LICENSING_STATUS.md) · [Roadmap](ROADMAP.md).
