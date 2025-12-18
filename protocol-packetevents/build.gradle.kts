plugins {
    id("phantom.library-conventions")
}

description = "Phantom NPC Library - PacketEvents Protocol Adapter"

dependencies {
    api(project(":protocol"))
    api(project(":platform-api"))

    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
}
