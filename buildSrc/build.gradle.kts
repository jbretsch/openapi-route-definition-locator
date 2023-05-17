plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:2.7.11")
    implementation("io.spring.gradle:dependency-management-plugin:1.0.11.RELEASE")
    implementation("com.github.ben-manes:gradle-versions-plugin:0.39.0")
}

java {
    toolchain {
        // Keep the same Java compatibility as Spring Cloud Gateway.
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}
