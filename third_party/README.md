# Runtime dependency notices

Generated on 2026-09-30 UTC from the resolved `releaseRuntimeClasspath`: 103 artifacts
in the default source edition, without Play Billing, AdMob or UMP.
`runtime-dependencies.json` records artifact hashes, license declarations and their
POM provenance (including inherited metadata). `NOTICES.runtime.txt` retains the
embedded LICENSE/NOTICE/COPYING/COPYRIGHT text found in the resolved JAR/AAR files.

This repository contains only the public edition. Its first-party code stays
Apache-2.0; third-party licenses and original notices are retained.

Flex uses the local `2.16.1-vds16k1` rebuild for Android 16 KB pages. The original
TensorFlow licence and embedded notices are retained alongside `NOTICE.vds-16k`;
the Java classes and 32-bit libraries are unchanged. The source recipe and pinned
inputs are documented in [FLEX_16K.md](../docs/FLEX_16K.md).

Graphics Path uses `1.0.1-vds16k1`. Its pinned AndroidX C++ sources are rebuilt
for ARM64 and x86_64, preserving the official Java classes, resources, 32-bit
libraries and notices. The AAR also contains `NOTICE.vds-16k` identifying the
modification; see [GRAPHICS_PATH_16K.md](../docs/GRAPHICS_PATH_16K.md).

LiteRT uses `2.2.0-vds16k2`: the complete classic Java API and all four JNI ABIs
are built from the same pinned public LiteRT commit. The CPUinfo ARM L2 counting
patch, its BSD licence and modification notice are retained in
[patches](patches/README.md) and in the AAR. See
[LITERT_16K_STATUS.md](../docs/LITERT_16K_STATUS.md) for sources and build inputs.
In the resolved inventory, the POM for `javax.inject:javax.inject:1` lacks licence metadata. Its attached
Maven source JAR retains the Apache-2.0 declaration and JSR-330 copyright;
[supplemental evidence](supplemental/javax.inject-1-evidence.json) records the
exact JAR and copyright-text hashes. This is a source supplement, not invented POM metadata.

[Native notices](native-notices/inventory.json) identify the repositories found
in retained linker inputs: four LiteRT ABIs and two rebuilt Flex ABIs match
the packaged AAR byte for byte after the original strip operation. Original
notice bytes are retained alongside the assembled text. Header-only dependency
review, unchanged official 32-bit Flex, Graphics Path and DataStore closure
remain open; this evidence does not establish complete legal clearance.

The 30 September audit adds libjpeg-turbo's original `README.ijg`, referenced
by its `LICENSE.md`, to the native collection and offline application notices.
It was copied from the retained exact Flex build source; the six native binary
bindings were checked again. The collector fails if this delegated licence is absent.

Regenerate with `tools/export_runtime_dependencies.gradle` and
`tools/collect_dependency_notices.py`; see the Windows qualification document.
No model weights, datasets, private sources or signing material are included.

This inventory does not establish legal clearance. Native libraries can include
additional third-party components whose notices are not recoverable from their
Maven POM. That transitive native review remains open. Preserve the project's
existing LICENSE and NOTICE and the original license terms of each dependency.
