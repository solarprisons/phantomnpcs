plugins {
    id("phantom.shadow-conventions")
}

description = "Phantom NPC Library - Paper Platform"

dependencies {
    api(project(":core"))

    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")

    // PacketEvents - required for EntityLib
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")

    // EntityLib for packet-based entities
    implementation("io.github.tofaa2:spigot:3.0.3-SNAPSHOT")
}

tasks.shadowJar {
    // Relocate EntityLib to avoid conflicts
    relocate("io.github.tofaa2.entitylib", "prisons.solar.npclib.libs.entitylib")
}
