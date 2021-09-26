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

import com.google.common.flogger.FluentLogger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * A Gradle task that is linked to an `AbstractExecTask`, that will
 * call `AbstractExecTask.stop()` when this task is run.
 *
 * You should * not need to create these tasks, as the `ExecForkPlugin` will create
 * them for any `AbstractExecTask` that has a `stopAfter` task specified.
 */
open class ExecJoin : DefaultTask() {
    private val log: FluentLogger = FluentLogger.forEnclosingClass()

    /** The task to call `stop()` on. */
    @Internal
    var forkTask: AbstractExecFork? = null

    @TaskAction
    fun exec() {
        log.atInfo().log(
            "Stopping `%s` task `%s`.",
            forkTask!!.javaClass.simpleName, forkTask!!.name
        )
        forkTask!!.stop()
    }
}

/**
 * Create a human-readable name for an `ExecJoin` task, given a corresponding
 * `AbstractExecFork` task.
 *
 * @return a human-readable string for an `ExecJoin` task
 * e.g.
 * `startFoo` -> `stopFoo`
 * `runJob` -> `stopJob`
 * `execPoodleDaemon` -> `stopPoodleDaemon`
 */
fun createNameFor(startTask: AbstractExecFork): String {

    val replacement: Map<String, String> = mapOf(
        "start" to "stop",
        "Start" to "Stop",
        "START" to "STOP",

        "run" to "stop",
        "Run" to "Stop",
        "RUN" to "STOP",

        "exec" to "stop",
        "Exec" to "Stop",
        "EXEC" to "STOP"
    )

    val taskName: String = startTask.name

    replacement.forEach { (key, value) ->
        if (taskName.hasWord(key)) {
            return@createNameFor taskName.replaceWord(key, value)
        }
    }

    return taskName + "_stop"
}

private fun String.hasWord(pattern: String): Boolean {
    return startsWith(pattern) || endsWith(pattern)
}

private fun String.replaceWord(pattern: String, replacement: String): String {
    val startReplaced = replace(Regex("^$pattern"), replacement)
    return startReplaced.replace(Regex("$pattern$"), replacement)
}
