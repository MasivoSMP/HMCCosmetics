# HMCCosmetics — Pinac 26.2 migration

## Pre-implementation review (2026-09-08)

Owner /root/hmccosmetics; requested GPT-6 Astra / medium. Isolated root C:/Users/CHESV/Documents/GitHub/Masivo-26.2/HMCCosmetics. Original actual Git root C:/Users/CHESV/Documents/GitHub/Masivo/Cosmetics/HMCCosmetics, remapped tracking origin/remapped at 00547fb0c3233fb847da6b9c68d3e94921684e3c. Origin https://github.com/MasivoSMP/HMCCosmetics.git; upstream https://github.com/HibiscusMC/HMCCosmetics.git read-only. No applicable AGENTS.md, CI build workflow, existing tests, or independent nested deployable found; common is this plugin's implementation/API module.

Git preparation completed before review/implementation: git fetch origin; git ls-remote --symref origin HEAD refs/heads/dev-26.2 refs/heads/remapped confirmed default remapped and no migration branch. Because remapped is occupied by the dirty original checkout, git worktree add --detach C:/Users/CHESV/Documents/GitHub/Masivo-26.2/HMCCosmetics remapped preserved that established base; git pull --ff-only origin remapped succeeded (already current); git switch -c dev-26.2 created a clean migration branch at the pulled SHA. No resets, stashes, discarded changes or base pushes.

Original Database.java has one uncommitted MessagesUtil import, SHA256 5c69b0d38a0fa0ed13f6d5ebed9e021e4349f9c7f7360fcf485dc7de8490044d. It remains untouched and is not imported/committed. The clean baseline independently lacks that import despite its constructor calling MessagesUtil: qualify that existing call if compilation confirms the error, leaving the user's original change alone.

Read shared migration reference/progress, Pinac report and accepted Commons/GUI/Economy handoffs. Target Pinac implementation 36efa7fe8c630737216393ca92cf14f1db2c759b / Canvas ddc374bb25e0f1e3cb6836aff89b538cbc10a919, Java25. Public Pinac API26.2-local SHA2561952d473fb3a4b57a227962759ac8776b8e28eacda000196ef62609f8239b4ee; HMCCosmetics has no direct NMS imports and needs no development bundle.

Applicable paths and evidence:
- Build currently Gradle8.12 / Java21, Paper1.21.4, old Commons0.9.1-8523cbe, GUI4.2.5, Economy0.1.9 composite, Sharding BOM0.4.7, WorldGuard7.0.12. Retarget Java25/compatible Gradle+Shadow+Lombok, actual API and accepted provider artifacts; remove old Economy composite. Preserve HMCC_INCLUDE_HASH/version2.9.1, publication workflow, resources/config/permissions and Folia support. Declare target API floor26.2. Inherited runServer1.21.11 is not target validation and will not run.
- Startup HMCCosmeticsPlugin -> HibiscusPlugin/setup -> Commons NMS adapter; packet interface snapshots and PacketThreadGate route cosmetics inventory/equipment/passenger/input packets. HMCCPacketManager and entity/wardrobe managers delegate builders to Commons. Accepted Commons4330f13d supplies26.2 adapter; consume its hashed shaded JAR, never the stale Maven coordinate. No PacketEvents direct dependency.
- HMCCPacketManager carries raw armorstand/cloud/avatar metadata IDs; inspect against target sources and retain only supported IDs. Version comparisons use Commons MinecraftVersion, including new26.2. Actual channel, wardrobe/NPC/camera/pose/passenger and client rendering checks remain runtime gates.
- Menu.writeInventoryItem -> readMetaMethodAsString reflects public ItemMeta hasItemModel/getItemModel only; inspect target signatures. Messages/MiniMessage/item lore use ordinary Adventure APIs. No direct removed ClickEvent/BookMeta/cube API found.
- Database/MySQLData/SQLData/Data: plugin-owned COSMETICDATABASE with UUID/COSMETICS text, colors/purchases/advancements/hidden reasons; no raw vanilla player/world files or bed block-entity PDC. CosmeticUser stamps item PDC via unchanged keys. Existing SQL failure/default-data and shared-connection behavior needs isolated failure/recovery testing; no schema rewrite authorized or required by26.2.
- PlayerConnectionListener loads SQL asynchronously then schedules at entity; transfers defer absent profiles, reject active wardrobes, await save before ready. EconomyUtil uses accepted Observatory money/sink/idempotence/fallback contracts; commands/menu actions preserve permissions/ownership and payment checks. Retain threading, SQL, monetary and public API behavior.
- WorldGuard onLoad registers existing flags; movement/teleport queries via BukkitAdapter and RegionQuery. Use accepted WorldGuard11815693 artifact and WorldEdit7.4.4; no provider writes or flag/region behavior change. Optional hooks (ModelEngine, BetterHud, HMCColor, Vulcan and Commons item hooks) require present/absent runtime checks.
- No generated packs/recipes/trades, clock logic or world-layout migration identified. Resource-pack models remain client validation, not permission to rewrite packs.

Minimal plan: replace build inputs with accepted read-only artifacts, preserve dependency/plugin contracts, compile against target, fix only evidenced incompatibilities. Run clean full build/check, dependency/archive inspections and relevant existing checks; add one dedicated no-framework check only if nontrivial migration logic changes. Record failures, final commands/logs/artifact hashes and commit/push all owned changes plus this report explicitly to dev-26.2. No production services/data, deployment, sibling build/publication or server edits.

Runtime acceptance is NOT RUN: accepted Pinac startup with provider set, MySQL copied legacy profiles/failure recovery/concurrent save, Sharding transfer completion/rollback, Economy purchase/fallback/idempotence, GUI/optional hooks, Commons packet/reflection/metadata/wardrobe client rendering, PDC/models and region ownership. Compile acceptance alone is not runtime certification.

## Implementation and validation

Changes are limited to build.gradle.kts, common/build.gradle.kts, settings.gradle.kts, wrapper properties, one Database.java logging qualifier and this report. Gradle9.5.1 / Shadow9.2.1 and Java25 compile/package the actual target; Lombok1.18.40 is the first JDK25-supporting release ([official changelog](https://projectlombok.org/changelog)); Shadow9.2.1 is a published compatible build plugin ([official release](https://plugins.gradle.org/plugin/com.gradleup.shadow/9.2.1)). No runtime feature, schema, permission, configuration key, transfer guard, money flow or scheduling policy changed. The original uncommitted import is still untouched; the clean baseline compile failure was fixed by fully qualifying its existing logging call.

Provider JARs are compile-only, explicitly located and SHA256 checked during configuration; missing or changed inputs fail clearly. The old Economy composite is removed, so builds never write sibling outputs. Paths are rooted in this isolated worktree's sibling layout; a fresh checkout needs the accepted handoffs at the documented locations. No provider Maven publication was performed. Existing common publication configuration and HMCC_INCLUDE_HASH suppression remain intact.

Accepted read-only dependency inputs:

| Input | Path / coordinate | SHA256 |
|---|---|---|
| Pinac API | io.canvasmc.pinac:pinac-api:26.2-local, local Maven | 1952d473fb3a4b57a227962759ac8776b8e28eacda000196ef62609f8239b4ee |
| HibiscusCommons0.9.1 / commit4330f13d | ../../Masivo/Cosmetics/HibiscusCommons/output/HibiscusCommons-0.9.1.jar | bf5e810696b7622c3dc9c5376be16881971e04079ddd35b18acded79f5ae642e |
| GUI API4.3.2 / commit ef6efbfc | ../MasivoGUI/build/libs/MasivoGUI-api.jar | a65df5ca5a0e5ca0753eb11ca4279d6d785c3516446d864f237dba7d732f4824 |
| Economy0.2.3 / commit f0c92604 | ../MasivoEconomy/build/libs/MasivoEconomy-0.2.3-plain.jar | d5ba6617bde60fdd7468633ea9af7731d3244f817637696afb749768ab445331 |
| WorldGuard Bukkit7.0.18-SNAPSHOT / commit11815693 | ../../Masivo/WorldGuard/worldguard-bukkit/build/libs/worldguard-bukkit-7.0.18-SNAPSHOT.jar | 0f3d2e90f8191e7b14ddf6593e269b38a15581968e214980c83ae3b02c0c1bed |
| WorldGuard core7.0.18-SNAPSHOT | ../../Masivo/WorldGuard/worldguard-core/build/libs/worldguard-core-7.0.18-SNAPSHOT.jar | 53ffd4370c215022aa71bbf990a03e6e4258213e071885479af8659059610119 |
| Sharding API0.6.58 | gg.masivo.sharding:masivo-sharding-api:0.6.58 | bb37c450df65484e863a84438c9930d8b26f7d48fc392c4d781ef6a4b7f103af |
| Sharding protocol0.6.58 | gg.masivo.sharding:masivo-sharding-protocol:0.6.58 | a39c849ba4ac512e55c842e308062070e6a5b7ad9265c61474d356d99bcfc68c |
| Sharding Paper API0.6.58 | gg.masivo.sharding:masivo-sharding-paper-api:0.6.58 | e5ac2be7609fb852917b028ba14a5a9eda2c113eb597ac6dbc58fbda21ab6fc3 |
| Retained bundled Triumph GUI | dev.triumphteam:triumph-gui:3.2.0-SNAPSHOT | f53bc39ea6cc05917ee5f72d7b003278da57706a0c3879e563a98fa1034eb8d0 |

WorldEdit Bukkit/core resolve official7.4.4; Sharding BOM resolves0.6.58. Compile graph uses only Pinac API26.2-local and Adventure5.2.0, with no stale Paper API/Commons/GUI/Economy artifacts. Existing optional compile pins ModelEngineR4.0.6, BetterHud1.12/BetterCommand1.3, Vault1.7.1, PlaceholderAPI2.11.6 and vendored HMCColor2.4/VulcanAPI remain; these are compile inputs, not assertions of installed runtime builds. Authlib1.5.25 is an unused inherited compile-only declaration and is not bundled.

Static target linkage evidence:
- Target Entity.java declares eight shared metadata fields; LivingEntity.java declares seven; ClassTreeIdRegistry.define increments from the nearest superclass. ArmorStand.DATA_CLIENT_FLAGS is consequently15, AreaEffectCloud.DATA_RADIUS8, Avatar main hand15 and skin customisation16. HMCCPacketManager's existing IDs/masks remain valid by target source inspection. No adapter duplication or constant rewrite needed; wire/client execution NOT RUN.
- javap against the accepted API confirms ItemMeta.hasItemModel()/getItemModel() public signatures. Existing Menu reflection retains those signatures. Actual reflective invocation on server ItemMeta remains NOT RUN.
- Initial packaged `jdeps --multi-release 25 --ignore-missing-deps -verbose:class` exposed the shaded particlehelper1.0.0-SNAPSHOT's obfuscated IRegistry/IRegistryCustom/MinecraftKey and CraftBukkit v1_19_R1 references. `rg -n -i 'particlehelper|com\.owen1212055'` across source/resources found only the dependency and relocation declarations. Removed those two unused lines with orchestrator agreement; no caller or public plugin API was removed. Evidence: migration-logs/packaged-class-dependencies.log.
- Retained Triumph GUI's BookBuilder bytecode uses BookMeta.setAuthor/addPage/setPage/setTitle directly, not Adventure Book inheritance (migration-logs/bundled-bookbuilder.log). Final archive jdeps finds no direct net.minecraft, CraftBukkit, removed BuildableComponent/Adventure MessageType/ClickEvent dependencies (migration-logs/accepted-jdeps.log). This inspection does not prove every optional third-party plugin binary compatible.

Commands ran here with JAVA_HOME=C:/Users/CHESV/.gradle/jdks/eclipse_adoptium-25-amd64-windows.2 and HMCC_INCLUDE_HASH=false:

- `gradlew.bat compileJava :common:compileJava --no-configuration-cache --console=plain`: first Kotlin script probe failed because java extension shadowed the java.security package, corrected by import; next run identified only the pre-existing unresolved Database MessagesUtil reference. Logs migration-logs/compile-1.log and compile-2.log.
- `gradlew.bat clean build --no-configuration-cache --console=plain`: PASS,17s,14 tasks (migration-logs/build-1.log). Common sources/javadocs and plugin shaded assembly succeed; normal root/common test tasks NO-SOURCE.
- Added the small Gradle migrationCheck to normal check: tests accepted SHA256 and rejection of wrong/missing handoffs; verifies plugin class, API floor26.2/Folia metadata and absence of bundled server/provider/old particlehelper classes. No framework or normal test-discovery suppression added. Initial assertion expected single-quoted YAML, while plugin-yml emits double quotes; corrected the assertion, not the descriptor (migration-logs/final-build.log / check-failure.log).
- Final `gradlew.bat clean build migrationDependencies :common:migrationDependencies -I migration-logs/dependencies.init.gradle --no-configuration-cache --console=plain`: PASS,12s,17 tasks all executed; migrationCheck passes; framework tests remain NO-SOURCE. Full resolved artifacts/files are logged in migration-logs/accepted-build.log. Diagnostic init script is now preserved under ignored build/migration-26.2/dependencies.init.gradle; use that path to repeat the diagnostic. Ordinary `gradlew.bat clean build --no-configuration-cache` includes the permanent migrationCheck without an init script.
- `jdeps --multi-release 25 --ignore-missing-deps -verbose:class build/libs/HMCCosmeticsRemapped-2.9.1.jar`: archive linkage inventory; external APIs intentionally report not found because the command inspects the standalone archive. A focused scan for the obsolete/internal classes above returned no matches. `javap -verbose` confirms plugin major version69 (Java25).
- `git diff --check`: PASS. Existing Java deprecation/unchecked/Javadoc warnings (100 Javadoc warnings), native-access and Gradle10 deprecation notices remain; no final build failure or test failure suppressed.

Artifacts from that final build:

| Path relative to isolated root | Bytes | SHA256 |
|---|---:|---|
| build/libs/HMCCosmeticsRemapped-2.9.1.jar | 433170 | 5586f39326fedc0dd13ae1d6dc9658ab5af8ed0f2037c077e47cb2b1a1698901 |
| common/build/libs/common.jar | 348531 | d46cc9982db3a73b1dd7dca772cfffcbee9e5e6d8154a85dd84c045352332c7b |
| run/plugins/HMCCosmeticsRemapped.jar | 433170 | 5586f39326fedc0dd13ae1d6dc9658ab5af8ed0f2037c077e47cb2b1a1698901 |

The run/plugins copy is the inherited shadowJar task's output inside this isolated checkout only; no server started or external plugin directory changed. Use the Remapped shaded JAR as the plugin, not the root plain/Javadoc/source JARs or common library. Resources, dependency names, permission defaults and version2.9.1 are retained; API floor intentionally26.2.

Development build/check/package acceptance PASS; no remaining build blocker. All runtime gates in the pre-review remain NOT RUN, particularly copied legacy SQL/PDC data, SQL failure/default-profile hazards, monetary/transfer concurrency, packet slot/camera/passenger rendering, optional hooks and region ownership. Inherited SQLData.get catches SQLExceptions and can return default data; shared MySQL connection/reconnect behavior also remains an explicit pre-existing runtime risk. No production access, deployment, shared publication, provider source/output writes or Pinac changes occurred.

Git review checkpoint15d72cc7 was committed/pushed before implementation. Implementation/validation checkpoint follows this report update; all owned changes are staged explicitly and pushed only with git push -u origin HEAD:refs/heads/dev-26.2. Final handoff verifies clean status and git ls-remote origin refs/heads/dev-26.2 equals local HEAD; original Database.java hash is rechecked separately.
