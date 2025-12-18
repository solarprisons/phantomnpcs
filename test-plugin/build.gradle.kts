plugins {
    id("phantom.shadow-conventions")
}

dependencies {
    // Phantom platform-paper includes all other modules transitively
    implementation(project(":platform-paper"))

    // Paper API - provided at runtime
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // PacketEvents - must be installed separately as a plugin
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
}

tasks.shadowJar {
    archiveBaseName.set("PhantomTest")

    // Don't relocate - this is a test plugin
    // Relocation causes issues with records and lambdas
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// Disable default jar
tasks.jar {
    enabled = false
}
