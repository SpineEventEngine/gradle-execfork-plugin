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
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ProcessForkOptions

/**
 * An abstract task that will launch an executable as a background process, optionally
 * waiting until a specific port is opened. The task will also stop the process if given
 * a `stopAfter` or `joinTask`.
 *
 * @see ExecFork
 * @see JavaExecFork
 * @see ProcessForkOptions
 */
abstract class AbstractExecFork : DefaultTask(), ProcessForkOptions {

    private val log: FluentLogger = FluentLogger.forEnclosingClass()

    @Input
    abstract override fun getEnvironment(): MutableMap<String, Any>

    @InputFile
    abstract override fun getExecutable(): String?

    /** The arguments to give the executable. */
    @Input
    var args: MutableList<CharSequence> = mutableListOf()

    @Internal
    abstract override fun getWorkingDir(): File

    /** The name of the file to write the process's standard output to. */
    @OutputFile
    @Optional
    var standardOutput: String? = null

    /** The name of the file to write the process's error output to. */
    @OutputFile
    @Optional
    var errorOutput: String? = null

    /** If specified, block the task from completing until the given port is open locally. */
    @Input
    @Optional
    var waitForPort: Int? = null

    @Input
    @Optional
    var waitForOutput: String? = null

    @Input
    @Optional
    var waitForError: String? = null

    @Input
    var forceKill: Boolean = false

    @Input
    var killDescendants: Boolean = true

    @Internal
    var process: Process? = null

    /**
     * The length of time in seconds that the task will wait for the port to be opened,
     * before failing.
     */
    @Input
    var timeout: Long = 60

    /**
     * If specified, this task will stop the running process after the `stopAfter`
     * task has been completed.
     */
    @get:Internal
    var stopAfter: Task? = null
        set(value) {
            val joinTaskVal: ExecJoin? = joinTask
            if (joinTaskVal != null) {
                log.atInfo().log(
                    "Adding '%s' as a finalizing task to '%s'.",
                    joinTaskVal.name, value?.name
                )
                value?.finalizedBy(joinTask)
            }
            field = value
        }

    @get:Internal
    var joinTask: ExecJoin? = null
        set(value) {
            val stopAfterVal: Task? = stopAfter
            if (stopAfterVal != null) {
                log.atInfo().log(
                    "Adding `%s` as a finalizing task to `%s`.",
                    value?.name, stopAfterVal.name
                )
                stopAfterVal.finalizedBy(value)
            }
            field = value
        }

    init {
        // The exec fork task should be executed in any case if not manually specified otherwise.
        // By default, this is the case as the task has only inputs defined, but e.g. jacoco
        // attaches a jvm argument provider, which in turn contributes an output property,
        // which causes the task to be considered up-to-date.
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun exec() {
        joinTask
                ?: throw GradleException(
                    "${javaClass.simpleName} task $name did not have" +
                        " a `joinTask` associated. Make sure you have \"" +
                            "apply plugin: 'io.spine.execfork'\"" +
                            " somewhere in your gradle file."
                )

        val processBuilder = ProcessBuilder(getProcessArgs())
        redirectStreams(processBuilder)

        val processWorkingDir: File = workingDir
        processWorkingDir.mkdirs()
        processBuilder.directory(processWorkingDir)

        environment.forEach { processBuilder.environment()[it.key] = it.value.toString() }

        log.atInfo().log(
            "Running process: `%s`.",
            processBuilder.command().joinToString(separator = " ")
        )

        this.process = processBuilder.start()
        installPipesAndWait(this.process!!)

        val waitForPortVal: Int? = waitForPort
        if (waitForPortVal != null)
            waitForPortOpen(waitForPortVal, timeout, TimeUnit.SECONDS, process!!)

        val task: AbstractExecFork = this
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                task.stop()
            }
        })
    }

    @Input
    abstract fun getProcessArgs(): List<String>?

    private fun installPipesAndWait(process: Process) {
        val processOut: OutputStream = outputStream()
        val outPipe = outPipe(process, processOut)
        outPipe.waitForPattern(timeout, TimeUnit.SECONDS)
    }

    private fun outputStream() =
        if (!standardOutput.isNullOrBlank()) {
            val stdOut = standardOutput!!
            project.file(stdOut).parentFile.mkdirs()
            FileOutputStream(stdOut)
        } else {
            OutputStreamLogger(project.logger)
        }

    private fun outPipe(process: Process, outputStream: OutputStream): InputStreamPipe {
        val outPipe = InputStreamPipe(process.inputStream, outputStream, waitForOutput)
        if (errorOutput != null) {
            val errorOut = errorOutput!!
            project.file(errorOut).parentFile.mkdirs()
            val errPipe =
                InputStreamPipe(process.errorStream, FileOutputStream(errorOut), waitForError)
            errPipe.waitForPattern(timeout, TimeUnit.SECONDS)
        }
        return outPipe
    }

    private fun redirectStreams(processBuilder: ProcessBuilder) {
        if (errorOutput == null) {
            processBuilder.redirectErrorStream(true)
        }
    }

    /**
     * Stop the process that this task has spawned
     */
    fun stop() {
        try {
            if (killDescendants) {
                stopDescendants()
            }
        } catch(e: Exception) {
            log.atWarning().log("Failed to stop descendants.", e)
        }

        stopRootProcess()
    }

    private fun stopRootProcess() {
        val process: Process = process ?: return
        if (process.isAlive && !forceKill) {
            process.destroy()
            process.waitFor(15, TimeUnit.SECONDS)
        }
        if (process.isAlive) {
            process.destroyForcibly().waitFor(15, TimeUnit.SECONDS)
        }
    }

    private fun stopDescendants() {
        val process: Process = process ?: return
        if (!process.isAlive) {
            return
        }

        val toHandle = process::class.memberFunctions.singleOrNull { it.name == "toHandle" }
        if (toHandle == null) {
            log.atSevere().log("Could not load `Process.toHandle()`." +
                    " The `killDescendants` flag requires Java 9+." +
                    " Please set `killDescendants=false`, or upgrade to Java 9+.")
            return // not supported, pre java 9?
        }

        toHandle.isAccessible = true
        val handle = toHandle.call(process)
        if (handle == null) {
            log.atWarning().log(
                "Could not get process handle. Process descendants may not be stopped."
            )
            return
        }
        val descendants = handle::class.memberFunctions.single { it.name == "descendants" }
        descendants.isAccessible = true
        val children: Stream<*> = descendants.call(handle) as Stream<*>
        val destroy = handle::class.memberFunctions.single {
            it.name == if (forceKill) "destroyForcibly" else "destroy"
        }
        destroy.isAccessible = true

        children.forEach {
            destroy.call(it)
        }
    }

    @Suppress("unused")
    fun <T : Task> setStopAfter(taskProvider: TaskProvider<T>) {
        stopAfter = taskProvider.get()
    }
}
