plugins {
    `java-library`
    `maven-publish`
    signing
    idea
    groovy
    id("com.github.ben-manes.versions")
}

apply(plugin = "io.spring.dependency-management")

repositories {
    mavenCentral()
}

the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

val springCloudDependenciesVersion = "2021.0.4"
val lombokVersion = "1.18.24"
val spockVersion = "2.3-groovy-3.0"
val groovyVersion = "3.0.13"

dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:${springCloudDependenciesVersion}"))

    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")
    testImplementation("org.projectlombok:lombok:${lombokVersion}")
    testAnnotationProcessor("org.projectlombok:lombok:${lombokVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation(platform("org.spockframework:spock-bom:${spockVersion}"))
    testImplementation("org.spockframework:spock-spring")

    testImplementation("org.codehaus.groovy:groovy-all:${groovyVersion}")
}

java {
    group = "net.bretti.openapi-route-definition-locator"
    version = "0.2.1-SNAPSHOT"
    toolchain {
        // Keep the same Java compatibility as Spring Cloud Gateway.
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}
