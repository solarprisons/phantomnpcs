plugins {
    id("phantom.shadow-conventions")
}

description = "Phantom NPC Library - Paper Platform"

// Generate version.properties at build time with version and GitHub info from git
tasks.register("generateVersionProperties") {
    val outputDir = layout.buildDirectory.dir("generated/resources")
    outputs.dir(outputDir)

    doLast {
        val propsFile = outputDir.get().file("phantom-version.properties").asFile
        propsFile.parentFile.mkdirs()

        // Extract GitHub owner/repo from git remote
        var githubOwner = ""
        var githubRepo = ""
        try {
            val remoteUrl = providers.exec {
                commandLine("git", "config", "--get", "remote.origin.url")
            }.standardOutput.asText.get().trim()

            // Parse GitHub URL (supports https and ssh formats)
            val regex = Regex("""(?:github\.com[:/])([^/]+)/([^/.]+)(?:\.git)?$""")
            regex.find(remoteUrl)?.let { match ->
                githubOwner = match.groupValues[1]
                githubRepo = match.groupValues[2]
            }
        } catch (e: Exception) {
            logger.warn("Could not determine GitHub repository from git remote: ${e.message}")
        }

        propsFile.writeText("""
            version=${project.version}
            github.owner=$githubOwner
            github.repo=$githubRepo
        """.trimIndent() + "\n")
    }
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.dir("generated/resources"))
        }
    }
}

tasks.processResources {
    dependsOn("generateVersionProperties")
}

tasks.named("sourcesJar") {
    dependsOn("generateVersionProperties")
}

dependencies {
    api(project(":core"))

    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")

    // PacketEvents - required for EntityLib
    compileOnly("com.github.retrooper:packetevents-spigot:2.11.0")

    // EntityLib for packet-based entities
    implementation("io.github.tofaa2:spigot:3.0.3-SNAPSHOT")
}

tasks.shadowJar {
    // Relocate EntityLib to avoid conflicts
    relocate("io.github.tofaa2.entitylib", "prisons.solar.npclib.libs.entitylib")
}
