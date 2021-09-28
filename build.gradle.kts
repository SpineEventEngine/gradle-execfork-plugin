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
import io.spine.internal.gradle.IncrementGuard
import io.spine.internal.gradle.PublishingRepos
import io.spine.internal.gradle.Scripts
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.forceVersions
import io.spine.internal.gradle.spinePublishing
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    apply(from = "$rootDir/version.gradle.kts")
    io.spine.internal.gradle.doApplyStandard(repositories)
    io.spine.internal.gradle.doForceVersions(configurations)
}

repositories.applyStandard()

plugins {
    id("com.gradle.plugin-publish").version("0.16.0")
    `java-gradle-plugin`
    kotlin("jvm") version io.spine.internal.dependency.Kotlin.version
    idea
    jacoco
    `force-jacoco`
}
apply<IncrementGuard>()

apply(from = "$rootDir/version.gradle.kts")

group = "io.spine.tools"
version = extra["versionToPublish"]!!

spinePublishing {
    with(PublishingRepos) {
        targetRepositories.addAll(
            gitHub("gradle-execfork-plugin"),
            cloudArtifactRegistry
        )
    }
    spinePrefix.set(false)
    publish(project)
}

val javaVersion = JavaVersion.VERSION_1_8

java {
    toolchain {
        targetCompatibility = javaVersion
    }
}
dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib-jdk8", Kotlin.version))
    implementation(Kotlin.reflect)

    implementation(Flogger.lib)
    implementation(Guava.lib)
    implementation(CheckerFramework.annotations)
    implementation(JavaX.annotations)
    ErrorProne.annotations.forEach { implementation(it) }

    testImplementation(Guava.testLib)
    testImplementation(JUnit.runner)
    testImplementation(JUnit.pioneer)
    JUnit.api.forEach { testImplementation(it) }
    Truth.libs.forEach { testImplementation(it) }

    runtimeOnly(Flogger.Runtime.systemBackend)
}

configurations.forceVersions()

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = javaVersion.toString()
        freeCompilerArgs = listOf("-Xskip-prerelease-check")
    }
}

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
    val integrationTests by creating(GradleBuild::class) {
        dir = File("${project.rootDir}/tests")
        tasks = listOf("clean", "build")
    }
    integrationTests.dependsOn("publishToMavenLocal")
    "test" { finalizedBy(integrationTests) }
    named<Test>("test") {
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
}

apply {
    with(Scripts) {
        // Aggregated coverage report across all subprojects.
        from(jacoco(project))
        // Generate a repository-wide report of 3rd-party dependencies and their licenses.
        from(repoLicenseReport(project))
        // Generate a `pom.xml` file containing first-level dependency of all projects
        // in the repository.
        from(generatePom(project))
    }
}
