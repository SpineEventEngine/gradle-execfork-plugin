/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

@file:Suppress("RemoveRedundantQualifierName") // Cannot use imports in some places.

import io.spine.internal.dependency.CheckerFramework
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Flogger
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.JavaX
import io.spine.internal.dependency.Kotlin
import io.spine.internal.dependency.Truth
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.forceVersions
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

buildscript {
    apply(from = "$rootDir/version.gradle.kts")
    io.spine.internal.gradle.doApplyStandard(repositories)
    io.spine.internal.gradle.doForceVersions(configurations)
}
repositories {
    mavenCentral()
}

repositories.applyStandard()

plugins {
    id("com.gradle.plugin-publish").version("0.16.0")
    kotlin("jvm") version io.spine.internal.dependency.Kotlin.version
    idea
    id("maven-publish")
    id("java-gradle-plugin")
    `force-jacoco`
}

apply(from = "$rootDir/version.gradle.kts")

group = "io.spine.tools"
version = extra["versionToPublish"]!!

java {
    toolchain {
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib-jdk8", Kotlin.version))
    implementation(Kotlin.reflect)

    api(Flogger.lib)
    api(Guava.lib)
    api(CheckerFramework.annotations)
    api(JavaX.annotations)
    ErrorProne.annotations.forEach { api(it) }

    testImplementation(Guava.testLib)
    testImplementation(JUnit.runner)
    testImplementation(JUnit.pioneer)
    JUnit.api.forEach { testImplementation(it) }
    Truth.libs.forEach { testImplementation(it) }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest-all:1.3")

    runtimeOnly(Flogger.Runtime.systemBackend)
}

configurations.forceVersions()

tasks.test {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
}

pluginBundle {
    website = "http://spine.io"
    vcsUrl = "https://github.com/SpineEventEngine/gradle-execfork-plugin"
    description = "Execute Java or shell processes in the background during a build"
    tags = listOf("java", "exec", "background", "process")

    (plugins) {
        create("execForkPlugin") {
            id = "io.spine.execfork"
            displayName = "Gradle Exec Fork Plugin"
        }
    }
}

tasks {
    val sampleProjects by creating(GradleBuild::class) {
        dir = File("${project.rootDir}/tests")
        tasks = listOf("clean", "build")
    }
    sampleProjects.dependsOn("publishToMavenLocal")
    "test" { finalizedBy(sampleProjects) }
    named<Test>("test") {
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
}

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from("javadoc")
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

artifacts {
    add("archives", javadocJar)
    add("archives", sourcesJar)
}
