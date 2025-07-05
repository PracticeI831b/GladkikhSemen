import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "gladkikh.semen.task6"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://jitpack.io")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.lets-plot:lets-plot-common:4.6.0")
    implementation("org.jetbrains.lets-plot:lets-plot-compose:2.2.1")
    implementation("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:4.10.0")

    implementation("org.jetbrains.lets-plot:platf-awt:4.6.2")

    implementation("org.slf4j:slf4j-simple:2.0.12")

    // material-icons-extended (может вызывать конфликты)
    // implementation("androidx.compose.material:material-icons-extended:1.7.6")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            packageName = "EquationSolver"
            packageVersion = "1.1.9"
            includeAllModules = true

            targetFormats(
                TargetFormat.Exe,
            )

            windows {
                iconFile.set(project.file("icons/gorilla.ico"))
            }
        }
    }
}