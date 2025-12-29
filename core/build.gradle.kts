plugins {
    id("phantom.library-conventions")
}

description = "Phantom NPC Library - Core Implementation"

dependencies {
    api(project(":api"))
    api(project(":platform-api"))

    // JSON serialization for persistence
    implementation("com.google.code.gson:gson:2.11.0")

    // TOML configuration
    implementation("io.github.wasabithumb:jtoml:1.3.0")

    // Caffeine configuration - exposed in CollisionManager API
    api("com.github.ben-manes.caffeine:caffeine:3.2.3")
}
