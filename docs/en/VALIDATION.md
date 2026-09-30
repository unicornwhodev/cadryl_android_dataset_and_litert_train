# Cadryl test results

[Project home](../../README.en.md) · [Français](../../TEST_REPORT.md)

## Public release 0.0.1

117 JVM / 113 Python, zero lint errors and 83 warnings, 101 KSP outputs. Android core passes 62/62 on Debug emulator and signed minified ARM64 phone Release. Independent UI passes 4/4; portrait/landscape matrices contain 288 captures. Installed database tables are preserved. The initial launcher-test R8 failure is retained; the corrected full suite passes. [Exact evidence](../RELEASE_001_EVIDENCE.json). The following reports are historical.

## Historical prerelease: 4.2.0-rc8

30 September 2026: 106 JVM/93 Python; lint 0 errors/148 warnings; 101 KSP files. Core 53/53 on physical Honor/Oppo ARM64/4 KB and native x86/API 36/16 KB. Independent UI 4/4 on Oppo and the emulator. Real private authorized HF publication/conflict/lost-response, SAF revocation and volume loss, 1,000 CC0 photos and ten-minute physical inference passed within their recorded scopes.

[Exact artifacts and scope](../RC8_QUALIFICATION_2026_09.md) · [Public receipts](../../test-results/rc8-release/README.md) · [Machine-readable status](../../QUALIFICATION_STATUS.json).

Physical ARM 16 KB remains unavailable: both phones use 4 KB. Model accuracy, real complete/partial Viewer import, background endurance, native transitive notices remain open.

## Previous prerelease: 4.2.0-rc7

27 September 2026. Tests use the actual minified, durable-signed Release APKs, version code 12.

| Check | Result |
|---|---|
| JVM / Python | 84/84 / 87/87, no failures or skips |
| Android lint | 0 errors, 124 warnings |
| Signed ARM64 Release | 45/45 core and 5/5 UI on API 36 / 16 KB emulator, ARM translated |
| Signed x86_64 Release | 45/45 core and 5/5 UI on the same emulator |
| Native alignment | All four libraries per APK pass strict ELF and ZIP checks |
| Model continuity | Original preserved, Save/Restore and continued training verified |

[Release note](../RC7_RELEASE.md) · [Evidence](../../test-results/rc7-release/README.md) · [Summary](../../test-results/rc7-release/summary.json) · [Machine-readable status](../../QUALIFICATION_STATUS.json).

Coverage includes guided setup, fixed and open class vocabularies, image dimensions, export preflight and recovery without changing human annotations or the HF parent. The independent UI driver checks five real app journeys.

The initial 43/45 core result is retained: two navigation tags depended on R8-renamed class names. Destinations now have explicit identifiers. The UI driver was also adapted to repeated Studio labels and signed with its existing test key to preserve the emulator installation. Initial failures remain alongside the complete final passes. The partial R8 trace retains missing framework/test references; reviewed app API rules are checked by real Release builds and execution.

ARM64 uses `libndk_translation` on an x86_64 host. **This is not phone acceptance.** No ART crash was found in the final runs. Real HF publication, fault campaigns and catalogue accuracy remain separate.

## Earlier evidence

[rc6](../RC6_RELEASE.md) retains its 70 JVM, 40 core and four UI results. The [26 September UX work](../UX_CONFIGURATION_2026_09.md) includes earlier Debug checks and phone observations. Older [native fixes](../NATIVE_FIX_2026_09.md), [Release/Honor](../RELEASE_CLOSURE_2026_09.md), [data and interruptions](../P1_QUALIFICATION_2026_09.md) and [catalogue tests](LITERT_QUALIFICATION.md) keep their original scope.

## Still pending

Physical ARM 16 KB, multi-model/background endurance, model quality, real complete/partial Viewer import, remote native transitive notices. The historical ART crash has no confirmed cause. [Known limits](KNOWN_LIMITATIONS.md) · [Release testing](../RELEASE_TESTING.md).
