plugins {
    java
    idea
    id("org.springframework.boot")
    id("com.github.ben-manes.versions")
}

apply(plugin = "io.spring.dependency-management")

repositories {
    mavenCentral()
}

tasks.jar {
    archiveFileName.set("api-gateway.jar")
}

dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2022.0.2"))
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation(project(":openapi-route-definition-locator-spring-cloud-starter"))
    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}

java {
    toolchain {
        // Keep the same Java compatibility as Spring Cloud Gateway.
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
    environment.set(mapOf("BP_JVM_VERSION" to "17"))
    imageName.set("bretti.net/sample-api-gateway:latest")
}
