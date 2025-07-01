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
    maven("https://jitpack.io") // Добавлен новый репозиторий
}

dependencies {
    implementation(compose.desktop.currentOs)
    // Обновленные зависимости Lets-Plot
    implementation("org.jetbrains.lets-plot:lets-plot-common:4.6.0")
    implementation("org.jetbrains.lets-plot:lets-plot-compose:2.2.1")
    implementation("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:4.10.0")
    implementation("org.jetbrains.lets-plot:platf-awt:4.5.2")
    // Библиотека для работы с SVG
    implementation("com.github.jiconfont:jiconfont-swing:1.0.0")
    implementation("com.github.jiconfont:jiconfont-font_awesome:4.7.0.1")
    implementation("org.slf4j:slf4j-simple:2.0.12")
}
compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "task6"
            packageVersion = "1.0.0"
        }
    }
}