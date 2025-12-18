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
}
