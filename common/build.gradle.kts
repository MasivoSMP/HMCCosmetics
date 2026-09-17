plugins {
    id("java")
    id("maven-publish")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))

    withJavadocJar()
    withSourcesJar()
}

publishing {
    val publishData = PublishData(project)
    publications {
        create<MavenPublication>("maven") {
            groupId = "${rootProject.group}"
            artifactId = "${rootProject.name}"
            version = "${rootProject.version}"

            from(components["java"])
        }
    }

    repositories {
        maven {
            authentication {
                credentials(PasswordCredentials::class) {
                    username = System.getenv("REPO_USERNAME")
                    password = System.getenv("REPO_PASSWORD")
                }
            }

            name = "HibiscusMCRepository"
            url = uri(publishData.getRepository())
        }
    }
}

class PublishData(private val project: Project) {
    var type: Type = getReleaseType()
    var hashLength: Int = 7

    private fun getReleaseType(): Type {
        val branch = getCheckedOutBranch()
        return when {
            branch.contentEquals("master") || branch.contentEquals("local") -> Type.RELEASE
            branch.startsWith("dev") -> Type.DEV
            else -> Type.SNAPSHOT
        }
    }

    private fun getCheckedOutGitCommitHash(): String =
        System.getenv("GITHUB_SHA")?.substring(0, hashLength) ?: "local"

    private fun getCheckedOutBranch(): String =
        System.getenv("GITHUB_REF")?.replace("refs/heads/", "") ?: "local"

    fun getVersion(): String = getVersion(false)

    fun getVersion(appendCommit: Boolean): String =
        type.append(getVersionString(), appendCommit, getCheckedOutGitCommitHash())

    private fun getVersionString(): String =
        (rootProject.version as String).replace("-SNAPSHOT", "").replace("-DEV", "")

    fun getRepository(): String = type.repo

    enum class Type(private val append: String, val repo: String, private val addCommit: Boolean) {
        RELEASE("", "https://repo.hibiscusmc.com/releases/", false),
        DEV("-DEV", "https://repo.hibiscusmc.com/development/", true),
        SNAPSHOT("-SNAPSHOT", "https://repo.hibiscusmc.com/snapshots/", true);

        fun append(name: String, appendCommit: Boolean, commitHash: String): String =
            name.plus(append).plus(if (appendCommit && addCommit) "-".plus(commitHash) else "")
    }
}

val guardCheck by sourceSets.creating {
    java.srcDir("src/check/java")
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += compileClasspath
}

val transferGuardCheck by tasks.registering(JavaExec::class) {
    dependsOn(tasks.named(guardCheck.classesTaskName))
    classpath = guardCheck.runtimeClasspath
    mainClass.set("com.hibiscusmc.hmccosmetics.sharding.CosmeticsTransferGuardCheck")
    enableAssertions = true
}
tasks.check { dependsOn(transferGuardCheck) }
