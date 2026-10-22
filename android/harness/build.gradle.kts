import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Laptop command-line tool that runs :core without a phone.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

application {
    mainClass.set("oyoon.harness.MainKt")
}

dependencies {
    implementation(project(":core"))
}
