plugins {
    `java-platform`
    `maven-publish`
    signing
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(project(":openapi-route-definition-locator-core"))
        api(project(":openapi-route-definition-locator-spring-cloud-starter"))
    }
}

javaPlatform {
    group = "net.bretti.openapi-route-definition-locator"
    version = "1.0.2-sc-2023.0"
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "openapi-route-definition-locator-bom"
            from(components["javaPlatform"])
            versionMapping {
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("openapi-route-definition-locator-bom")
                description.set("Bill of materials for the OpenAPI Route Definition Locator")
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
            val releasesRepoUrl = "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://central.sonatype.com/repository/maven-snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
