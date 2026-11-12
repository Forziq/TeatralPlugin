plugins {
    java
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("io.github.toxicity188:bettermodel-bukkit-api:2.2.0")
    testImplementation("org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

val pinnedBetterModel: String = run {
    val row = file("docs/versions.md").readLines().firstOrNull {
        it.trim().startsWith("|") && it.split("|").getOrNull(1)?.trim().equals("BetterModel", ignoreCase = true)
    }
    Regex("""\d+\.\d+\.\d+""").find(row?.split("|")?.getOrNull(2) ?: "")?.value ?: ""
}

tasks.processResources {
    inputs.file("docs/versions.md")
    doLast {
        destinationDir.resolve("versions.properties").writeText("bettermodel=$pinnedBetterModel\n")
    }
    from("assets/music/tracks.yml") {
        into("music")
    }
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.test {
    useJUnitPlatform()
}
