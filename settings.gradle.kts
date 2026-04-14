pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()  // ← 确保这行存在
        mavenCentral()
    }
}
// ⭐ 自動處理 JDK（避免 Java 找不到）
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "SmartDAssistant"
include(":app")