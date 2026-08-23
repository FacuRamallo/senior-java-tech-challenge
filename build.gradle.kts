@file:Suppress("UnstableApiUsage")

plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "0.11.1"
    id("com.diffplug.spotless") version "8.10.0"
    jacoco
}

group = "com.mango"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
        val integrationTest by registering(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(project())
            }
            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
val integrationTestRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

dependencies {
    // Production dependencies
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    
    // UUIDv7 Generator
    implementation("com.fasterxml.uuid:java-uuid-generator:5.1.0")

    // Test dependencies (unit tests & integration tests)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.5")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

spotless {
    java {
        targetExclude("build/**")
        googleJavaFormat("1.30.0")
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test, tasks.named("integrationTest"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

springBoot {
    mainClass.set("com.mango.products.ProductsApplication")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("app")
            mainClass.set("com.mango.products.ProductsApplication")
        }
    }
}
