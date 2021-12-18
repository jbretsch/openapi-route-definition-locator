plugins {
    id("openapi-route-definition-locator.common-java-library")
}

java {
    registerFeature("metrics") {
        usingSourceSet(sourceSets["main"])
    }
}

dependencies {
    api(project(":openapi-route-definition-locator-core"))
    "metricsApi"("io.micrometer:micrometer-core")
    "metricsImplementation"("org.springframework.boot:spring-boot-actuator-autoconfigure")
    implementation("org.springframework.cloud:spring-cloud-gateway-server")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")

    testImplementation("org.springframework.cloud:spring-cloud-starter-gateway")
    testRuntimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    testRuntimeOnly("io.micrometer:micrometer-registry-prometheus")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.33.2")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "openapi-route-definition-locator-spring-cloud-starter"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("openapi-route-definition-locator-spring-cloud-starter")
                description.set("Spring Cloud starter for the OpenAPI Route Definition Locator")
                url.set("https://github.com/jbretsch/openapi-route-definition-locator")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/jbretsch/openapi-route-definition-locator/blob/master/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("jbretsch")
                        name.set("Jan Bretschneider")
                        email.set("mail@jan-bretschneider.de")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/jbretsch/openapi-route-definition-locator.git")
                    developerConnection.set("scm:git:ssh://github.com/jbretsch/openapi-route-definition-locator.git")
                    url.set("https://github.com/jbretsch/openapi-route-definition-locator")
                }
            }
        }
    }
    repositories {
        maven {
            name = "ossrh"
            credentials(PasswordCredentials::class)
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

// Remove <dependencyManagement> entries from published POM.
// Inspired by <https://github.com/spring-gradle-plugins/dependency-management-plugin/issues/257#issuecomment-895790557>.
tasks.withType<GenerateMavenPom>().all {
    doLast {
        val file = File("$buildDir/publications/mavenJava/pom-default.xml")
        var text = file.readText()
        val regex = "(?s)(<dependencyManagement>.+?<dependencies>)(.+?)(</dependencies>.+?</dependencyManagement>)".toRegex()
        val matcher = regex.find(text)
        if (matcher != null) {
            text = regex.replace(text, "")
        }
        file.writeText(text)
    }
}
