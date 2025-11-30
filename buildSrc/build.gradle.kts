plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:4.0.0")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
    implementation("com.github.ben-manes:gradle-versions-plugin:0.53.0")
}

java {
    toolchain {
        // Keep the same Java compatibility as Spring Cloud Gateway.
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
