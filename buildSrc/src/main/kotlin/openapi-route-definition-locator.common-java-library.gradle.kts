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

val springCloudDependenciesVersion = "2021.0.9"
val lombokVersion = "1.18.32"
val spockVersion = "2.3-groovy-3.0"

dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:${springCloudDependenciesVersion}"))

    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")
    testImplementation("org.projectlombok:lombok:${lombokVersion}")
    testAnnotationProcessor("org.projectlombok:lombok:${lombokVersion}")
}

java {
    group = "net.bretti.openapi-route-definition-locator"
    version = "1.0.1-sc-2021.0-SNAPSHOT"
    toolchain {
        // Keep the same Java compatibility as Spring Cloud Gateway.
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    withJavadocJar()
    withSourcesJar()
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()

            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-test")
                implementation(platform("org.spockframework:spock-bom:${spockVersion}"))
                implementation("org.spockframework:spock-spring")
                implementation("org.codehaus.groovy:groovy-json")
            }
        }
    }
}
