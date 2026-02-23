/**
 * NOTE: This is entirely optional and basics can be done in `settings.gradle.kts`
 */

import groovy.json.JsonSlurper

val manifestVersion: String by lazy {
    val manifestFile = file("src/main/resources/manifest.json")
    val json = JsonSlurper().parseText(manifestFile.readText()) as Map<*, *>
    json["Version"]?.toString() ?: error("manifest.json missing Version")
}

repositories {
    // Any external repositories besides: MavenLocal, MavenCentral, HytaleMaven, and CurseMaven
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.jspecify)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

tasks.withType<Jar> {
    archiveFileName.set("MobFarmingTools-$manifestVersion.jar")
}