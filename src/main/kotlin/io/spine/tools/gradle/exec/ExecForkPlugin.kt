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

package io.spine.tools.gradle.exec

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.util.GradleVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Gradle plugin that will allow for 'com.github.psxpaul.ExecFork' and 'com.github.psxpaul.JavaExecFork' task
 * types. This plugin will make sure all of those tasks are stopped when a build completes, and optionally
 * create stop tasks for each one that specifies a 'stopAfter' task.
 *
 * Note: it is important to apply this plugin to your project for your ExecFork and JavaExecFork tasks to
 * work. E.g.:
 *          apply plugin: 'gradle-execfork-plugin'
 */
class ExecForkPlugin : Plugin<Project> {
    val log: Logger = LoggerFactory.getLogger(ExecForkPlugin::class.java)

    override fun apply(project: Project) {
        if (GradleVersion.current() < GradleVersion.version("5.3")) {
            throw GradleException("This version of the plugin is incompatible with gradle < 5.3! Please use execfork version 0.1.9, or upgrade gradle.")
        }

        val forkTasks: ArrayList<AbstractExecFork> = ArrayList()
        project.tasks.whenTaskAdded { task: Task ->
            if (task is AbstractExecFork) {
                val forkTask: AbstractExecFork = task
                val joinTask: ExecJoin = project.tasks.create(createNameFor(forkTask), ExecJoin::class.java)
                joinTask.forkTask = forkTask
                forkTask.joinTask = joinTask

                forkTasks.add(forkTask)
            }
        }

        project.gradle.addBuildListener(object: BuildAdapter() {
            override fun buildFinished(result: BuildResult) {
                for (forkTask: AbstractExecFork in forkTasks) {
                    try {
                        forkTask.stop()
                    } catch (e: InterruptedException) {
                        log.error("Error stopping daemon for {} task '{}'", forkTask.javaClass.simpleName, forkTask.name, e)
                    }
                }
            }
        })
    }
}
