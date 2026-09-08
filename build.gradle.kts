import java.util.zip.ZipFile
import java.security.MessageDigest
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.1"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

group = "com.hibiscusmc"
version = "2.9.1${getGitCommitHash()}"

allprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
        mavenLocal()

        // Paper Repo
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")

        // Jitpack
        maven("https://jitpack.io")

        // Geary
        maven("https://repo.mineinabyss.com/releases/")
        maven("https://repo.mineinabyss.com/snapshots/")

        // PlaceholderAPI
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")

        // Citizens & Denizen
        maven("https://maven.citizensnpcs.co/repo")

        // Worldguard
        maven("https://maven.enginehub.org/repo/")

        // Backup Oraxen repo
        maven("https://repo.skyslycer.de/")

        // MythicMobs
        maven {
            url = uri("https://mvn.lumine.io/repository/maven-public")
            metadataSources {
                artifact()
            }
        }

        // md-5 Repo
        maven("https://repo.md-5.net/content/groups/public/")

        // MMOItems
        maven("https://nexus.phoenixdevt.fr/repository/maven-public/")

        // Eco-Suite/Auxilor Repo
        maven("https://repo.auxilor.io/repository/maven-public/")

        // Triumph GUI, used only by the internal dye menu.
        maven("https://repo.triumphteam.dev/snapshots")

        // Hibiscus Commons
        maven("https://repo.hibiscusmc.com/releases")
    }

    dependencies {
        compileOnly(fileTree("${project.rootDir}/lib") { include("*.jar") })
        compileOnly("com.mojang:authlib:1.5.25")
        compileOnly("io.canvasmc.pinac:pinac-api:26.2-local")
        compileOnly("org.jetbrains:annotations:24.1.0")
        compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
        compileOnly("me.clip:placeholderapi:2.11.6")
        compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.6")
        compileOnly(files(
            acceptedJar("../../Masivo/WorldGuard/worldguard-bukkit/build/libs/worldguard-bukkit-7.0.18-SNAPSHOT.jar", "0f3d2e90f8191e7b14ddf6593e269b38a15581968e214980c83ae3b02c0c1bed"),
            acceptedJar("../../Masivo/WorldGuard/worldguard-core/build/libs/worldguard-core-7.0.18-SNAPSHOT.jar", "53ffd4370c215022aa71bbf990a03e6e4258213e071885479af8659059610119")
        ))
        compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.4") {
            exclude(group = "org.bukkit")
            exclude(group = "io.papermc.paper")
        }
        compileOnly("io.github.toxicity188:BetterHud-standard-api:1.12") //Standard api
        compileOnly("io.github.toxicity188:BetterHud-bukkit-api:1.12") //Platform api
        compileOnly("io.github.toxicity188:BetterCommand:1.3") //BetterCommand library
        //compileOnly("it.unimi.dsi:fastutil:8.5.14")
        compileOnly("org.projectlombok:lombok:1.18.40")
        compileOnly(files(acceptedJar("../../Masivo/Cosmetics/HibiscusCommons/output/HibiscusCommons-0.9.1.jar", "bf5e810696b7622c3dc9c5376be16881971e04079ddd35b18acded79f5ae642e")))

        // Handled by Spigot Library Loader ~ Deprecated as of Dec 16, 2025
        /*
        compileOnly("net.kyori:adventure-api:4.24.0")
        compileOnly("net.kyori:adventure-text-minimessage:4.24.0")
        compileOnly("net.kyori:adventure-platform-bukkit:4.4.1")
         */

        annotationProcessor("org.projectlombok:lombok:1.18.40")
        testCompileOnly("org.projectlombok:lombok:1.18.40")
        testAnnotationProcessor("org.projectlombok:lombok:1.18.40")

        compileOnly(files(acceptedJar("../MasivoGUI/build/libs/MasivoGUI-api.jar", "a65df5ca5a0e5ca0753eb11ca4279d6d785c3516446d864f237dba7d732f4824")))
        compileOnly(files(acceptedJar("../MasivoEconomy/build/libs/MasivoEconomy-0.2.3-plain.jar", "d5ba6617bde60fdd7468633ea9af7731d3244f817637696afb749768ab445331")))
        compileOnly(platform("gg.masivo.sharding:masivo-sharding-bom:0.6.58"))
        compileOnly("gg.masivo.sharding:masivo-sharding-paper-api")
        implementation("dev.triumphteam:triumph-gui:3.2.0-SNAPSHOT") {
            exclude("net.kyori")
        }
    }

    tasks {
        javadoc {
            // javadoc spec has these added.
            (options as StandardJavadocDocletOptions)
                .tags("apiNote:a:API:", "implSpec:a:Implementation Requirements", "implNote:a:Implementation Note:")
        }
    }
}

dependencies {
    implementation(project(path = ":common"))
}

tasks {

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filteringCharset = Charsets.UTF_8.name()
    }

    runServer {
        minecraftVersion("1.21.11")

        downloadPlugins {
            hangar("PlaceholderAPI", "2.12.2")
            hangar("Multiverse-Core", "5.3.4")
            url("https://download.luckperms.net/1624/bukkit/loader/LuckPerms-Bukkit-5.5.36.jar")
            github("Test-Account666", "PlugManX", "2.4.1", "PlugManX-2.4.1.jar")
            github("gecolay", "GSit", "3.2.1", "GSit-3.2.1.jar")
        }
    }

    shadowJar {
        mergeServiceFiles()

        relocate("dev.triumphteam.gui", "com.hibiscusmc.hmccosmetics.shaded.gui")
        archiveFileName.set("HMCCosmeticsRemapped-${project.version}.jar")

        dependencies {
            exclude(dependency("org.yaml:snakeyaml"))
        }

        doLast {
            archiveFile.get().asFile.copyTo(layout.projectDirectory.file("run/plugins/HMCCosmeticsRemapped.jar").asFile, true)
            println("If you use the plugin, consider buying it for: ")
            println("The custom resource pack, Oraxen + ItemAdder configurations, and Discord support!")
            println("Polymart: https://polymart.org/resource/1879")
            println("Spigot: https://www.spigotmc.org/resources/100107/")
        }
    }

    build {
        dependsOn(shadowJar)
    }
}


bukkit {
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin"
    apiVersion = "26.2"
    foliaSupported = true
    authors = listOf("LoJoSho")
    depend = listOf("HibiscusCommons", "MasivoGUI", "MasivoEconomy", "MasivoSharding")
    softDepend = listOf("Vault", "Nexo", "BetterHud", "ModelEngine", "Oraxen", "ItemsAdder", "Geary", "HMCColor", "WorldGuard", "MythicMobs", "PlaceholderAPI", "SuperVanish", "PremiumVanish", "LibsDisguises", "Denizen", "MMOItems", "Eco")
    version = "${project.version}"
    loadBefore = listOf(
        "Cosmin" // Prevents Cosmin from taking /cosmetic first.
    )

    commands {
        register("cosmetic") {
            description = "Base Cosmetic Command"
            aliases = listOf("hmccosmetics", "cosmetics")
        }
    }
    permissions {
        register("hmccosmetics.cmd.default") {
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("hmccosmetics.cmd.apply") {
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("hmccosmetics.cmd.unapply") {
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("hmccosmetics.cmd.dye") {
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("hmccosmetics.cmd.wardrobe") {
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("hmccosmetics.cmd.menu") {
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("hmccosmetics.emote.shiftrun") {
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("hmccosmetics.cmd.emote") {
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("hmccosmetics.cmd.playemote") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.playemote.other") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.emote.other") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.setwardrobesetting") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.dataclear") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.reload") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.apply.other") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.unapply.other") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.hide") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.show") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.toggle") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.hide.other") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.show.other") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.toggle.other") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.wardrobe.other") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.menu.other") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.debug") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.unapplydeath.bypass") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.disableall") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.hiddenreasons") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("hmccosmetics.cmd.clearhiddenreasons") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))

    withJavadocJar()
    withSourcesJar()
}

fun getGitCommitHash(): String {
    var includeHash = true
    val includeHashVariable = System.getenv("HMCC_INCLUDE_HASH")

    if (!includeHashVariable.isNullOrEmpty()) includeHash = includeHashVariable.toBoolean()

    if (includeHash) {
        return try {
            val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().use { "-" + it.readLine().trim() }
        } catch (e: Exception) {
            "-unknown" // Fallback if Git is not available or an error occurs
        }
    }
    return ""
}

// Accepted provider handoffs are read-only; never build or publish sibling projects here.
fun acceptedJar(path: String, sha256: String): File {
    val jar = rootProject.file(path)
    require(jar.isFile) { "Missing accepted provider JAR: $jar (see MIGRATION_26_2.md)" }
    val actual = MessageDigest.getInstance("SHA-256").digest(jar.readBytes())
        .joinToString("") { "%02x".format(it) }
    require(actual == sha256) { "Accepted provider JAR hash mismatch: $jar" }
    return jar
}


tasks.register("migrationCheck") {
    dependsOn(tasks.shadowJar)
    doLast {
        val probe = layout.buildDirectory.file("migration-check/provider.jar").get().asFile
        probe.parentFile.mkdirs()
        probe.writeText("abc")
        check(acceptedJar(probe.path, "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad") == probe)
        check(runCatching { acceptedJar(probe.path, "wrong") }.exceptionOrNull() is IllegalArgumentException)
        check(runCatching { acceptedJar(probe.path + ".missing", "wrong") }.exceptionOrNull() is IllegalArgumentException)
        ZipFile(tasks.shadowJar.get().archiveFile.get().asFile).use { jar ->
            val descriptor = jar.getInputStream(jar.getEntry("plugin.yml")).bufferedReader().readText()
            check(descriptor.contains("api-version: \"26.2\""))
            check(descriptor.contains("folia-supported: true"))
            check(jar.getEntry("com/hibiscusmc/hmccosmetics/HMCCosmeticsPlugin.class") != null)
            check(jar.entries().asSequence().none { entry ->
                listOf("org/bukkit/", "net/minecraft/", "net/kyori/", "gg/masivo/", "me/lojosho/", "com/sk89q/", "com/hibiscusmc/hmccosmetics/shaded/particlehelper/")
                    .any { entry.name.startsWith(it) }
            })
        }
        println("Migration check passed: accepted JAR hashes/rejection and target plugin packaging")
    }
}
tasks.check { dependsOn("migrationCheck") }
