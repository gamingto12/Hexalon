import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("java-library")
    id("de.eldoria.plugin-yml.paper") version "0.9.0"
    id("com.gradleup.shadow") version "9.4.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    compileOnly(libs.paper.api)
    implementation("org.projectlombok:lombok:1.18.46")
    compileOnly("org.apache.logging.log4j:log4j-core:2.24.1")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    implementation("org.reflections:reflections:0.10.2")
    implementation("net.dv8tion:JDA:6.5.0")
}

paper {
    name = "Hexalon"
    version = project.version.toString()
    main = "me.gamingto12.Hexalon"
    apiVersion = "26.2"
    author = "gamingto12"
    description = "Discord bridge plugin"
    website = "https://gamingto12.is-a.dev"
    permissions {
        register("hexalon.admin") {
            description = "Plugin for using administrative features"
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

val buildNumberFile = layout.projectDirectory.file("build-number.properties").asFile

fun readBuildNumber(): Int {
    if (!buildNumberFile.exists()) return 1
    return buildNumberFile.readLines()
        .firstOrNull { it.startsWith("buildNumber=") }
        ?.substringAfter("=")
        ?.trim()
        ?.toIntOrNull()
        ?.plus(1)
        ?: 1
}

val buildAuthor: String = (findProperty("buildAuthor") ?: "gamingto12").toString()
val buildNumber: Int    = readBuildNumber()
val buildDate: String   = SimpleDateFormat("M/dd/yyyy 'at' h:mm:ss aa zzz").format(Date())
val buildVersion: String = version.toString()
val buildPropertiesMap = mapOf(
    "buildAuthor" to buildAuthor,
    "buildNumber" to buildNumber,
    "buildVersion" to buildVersion,
    "buildDate" to buildDate
)

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
        finalizedBy("incrementBuildNumber")
    }

    assemble {
        dependsOn(shadowJar)
    }

    processResources {
        inputs.properties(buildPropertiesMap)

        filesMatching("build.properties") {
            expand(buildPropertiesMap)
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

tasks.register("incrementBuildNumber") {
    description = "Writes the incremented build number back to build-number.properties for the next build"

    // Capture everything needed as local vals so the doLast lambda is a pure
    // closure over plain values and never references `project`.
    val outputFile = buildNumberFile
    val nextNumber = buildNumber

    outputs.file(outputFile)

    doLast {
        outputFile.parentFile.mkdirs()
        outputFile.writeText("buildNumber=$nextNumber\n")
    }
}