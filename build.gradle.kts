@file:Suppress("UnstableApiUsage")

import com.adarshr.gradle.testlogger.theme.ThemeType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm").version(deps.versions.kotlin.asProvider().get()) apply true
    alias(deps.plugins.detekt)
    alias(deps.plugins.test.logger)
    alias(deps.plugins.kotlin.serialization)
    id("maven-publish")
    signing
}

group = "com.github.turbomates"
version = "0.5.13"


repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    api(deps.bundles.turbomates)
    api(deps.bundles.ktor.server)
    api(deps.bundles.ktor.client)
    api(deps.bundles.exposed)
    api(deps.kotlin.reflect)
    api(deps.valiktor.core)
    api(deps.google.guice)
    api(deps.postgresql.jdbc)
    api(deps.hikaricp)
    api(deps.kotlin.serialization)
    api(deps.kotlin.serialization.json)
    api(deps.rabbitmq.amqp.client)
    api(deps.swagger.webjar)
    api(deps.s3)
    api(deps.hoplite)
    api(deps.jedis)
    api(deps.log4j.api)
    api(deps.log4j.slf4j)
    api(deps.email)
    runtimeOnly(deps.log4j.core)

    testImplementation(deps.kotlin.test)
    testImplementation(deps.greenmail)
    testImplementation(deps.ktor.server.test.host) {
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
    testImplementation(deps.embedded.postgres)
    testImplementation(deps.h2.database)
    detektPlugins(deps.detekt.formatting)
}

detekt {
    toolVersion = deps.versions.detekt.get()
    autoCorrect = false
    parallel = true
    config = files("detekt.yml")
}
tasks.named("check").configure {
    this.setDependsOn(this.dependsOn.filterNot {
        it is TaskProvider<*> && it.name == "detekt"
    })
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-opt-in=io.ktor.locations.KtorExperimentalLocationsAPI",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlinx.serialization.InternalSerializationApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.time.ExperimentalTime",
            "-Xskip-prerelease-check"
        )
    }
}


configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

//  ----------------  TEST ----------------  //

testlogger {
    theme = ThemeType.PLAIN
}
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("PASSED", "STARTED", "FAILED", "SKIPPED")
        // showStandardStreams = true
    }
    doFirst {
        System.getProperties().forEach { (k, v) ->
            systemProperty(k.toString(), v.toString())
        }
    }
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "common-lib"
            groupId = "dev.tmsoft.kotlin"
            from(components["java"])
            pom {
                name.set("Kotlin backend common library")
                description.set("This library contains different resolutions with ktor, exposed, serialization")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
//                 developers {
//                     developer {
//                         id.set("shustrik")
//                         name.set("vadim golodko")
//                         email.set("vadim@turbomates.com")
//                     }
//                 }
//                 scm {
//                     connection.set("scm:https://github.com/turbomates/kotlin-back-sdk.git")
//                     developerConnection.set("scm:git@github.com:turbomates/kotlin-back-sdk.git")
//                 }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/harmeetSohi27/kotlin-back-sdk")
            credentials {
                username = "harmeetsohi27@gmail.com"
                password = "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"
            }
        }
    }
}

//publishing {
//    publications {
//        create<MavenPublication>("mavenJava") {
//            from(components["java"])
//        }
//    }
//}
