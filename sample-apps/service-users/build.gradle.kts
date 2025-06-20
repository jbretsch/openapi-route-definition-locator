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
    archiveFileName.set("service-users")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    testImplementation("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
    environment.set(mapOf("BP_JVM_VERSION" to "17"))
    imageName.set("bretti.net/sample-service-users:latest")
}
