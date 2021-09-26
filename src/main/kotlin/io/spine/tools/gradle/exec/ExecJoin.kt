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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A gradle task that is linked to an `AbstractExecTask`, that will
 * call `AbstractExecTask.stop()` when this task is run. You should
 * not need to create these tasks, as the `ExecForkPlugin` will create
 * them for any `AbstractExecTask` that has a `stopAfter` task specified.
 */
open class ExecJoin : DefaultTask() {
    private val log: Logger = LoggerFactory.getLogger(ExecJoin::class.java)

    /**
     * The task to call .`stop()` on
     */
    @Internal
    var forkTask: AbstractExecFork? = null

    @TaskAction
    fun exec() {
        log.info("Stopping {} task {}", forkTask!!.javaClass.simpleName, forkTask!!.name)
        forkTask!!.stop()
    }
}

/**
 * Create a human-readable name for an ExecJoin task, given a corresponding
 * AbstractExecFork task.
 *
 * @return a human-readable string for an ExecJoin task
 * e.g.
 * startFoo -> stopFoo
 * runJob -> stopJob
 * execPoodleDaemon -> stopPoodleDaemon
 */
fun createNameFor(startTask: AbstractExecFork):String {
    val taskName:String = startTask.name

    if (hasWord(taskName, "start")) return replaceWord(taskName, "start", "stop")
    if (hasWord(taskName, "Start")) return replaceWord(taskName, "Start", "Stop")
    if (hasWord(taskName, "START")) return replaceWord(taskName, "START", "STOP")

    if (hasWord(taskName, "run")) return replaceWord(taskName, "run", "stop")
    if (hasWord(taskName, "Run")) return replaceWord(taskName, "Run", "Stop")
    if (hasWord(taskName, "RUN")) return replaceWord(taskName, "RUN", "STOP")

    if (hasWord(taskName, "exec")) return replaceWord(taskName, "exec", "stop")
    if (hasWord(taskName, "Exec")) return replaceWord(taskName, "Exec", "Stop")
    if (hasWord(taskName, "EXEC")) return replaceWord(taskName, "EXEC", "STOP")

    return taskName + "_stop"
}

private fun hasWord(input:String, pattern:String):Boolean {
    return input.startsWith(pattern) || input.endsWith(pattern)
}

private fun replaceWord(input:String, pattern:String, replacement:String):String {
    return input.replace(Regex("^$pattern"), replacement).replace(Regex("$pattern$"), replacement)
}
