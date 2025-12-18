plugins {
    id("phantom.library-conventions")
    id("com.gradleup.shadow")
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}
