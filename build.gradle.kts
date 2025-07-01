import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "GladkikhSemen"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.lets-plot:lets-plot-common:4.0.0")
    implementation("org.jetbrains.lets-plot:lets-plot-compose:1.0.0")
    implementation("org.jetbrains.lets-plot:lets-plot-kotlin:4.0.0")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "task6"
            packageVersion = "1.0.0"
        }
    }
}
