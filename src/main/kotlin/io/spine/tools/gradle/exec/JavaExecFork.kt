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

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.internal.jvm.Jvm
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.process.JavaForkOptions
import org.gradle.process.internal.JavaForkOptionsFactory
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import javax.inject.Inject

/**
 * A task that will run a java class in a separate process, optionally
 * writing stdout and stderr to disk, and waiting for a specified
 * port to be open.
 *
 * @see AbstractExecFork
 */
open class JavaExecFork @Inject constructor(forkOptionsFactory: JavaForkOptionsFactory) :
    AbstractExecFork(),
    JavaForkOptions by forkOptionsFactory.newJavaForkOptions() {

    /** The classpath to call java with. */
    @InputFiles
    var classpath: FileCollection? = null

    /** The fully qualified name of the class to execute (e.g. 'com.foo.bar.MainExecutable'). */
    @Input
    var main: String? = null

    override fun getProcessArgs(): List<String>? {
        val processArgs: MutableList<String> = mutableListOf()
        with(processArgs) {
            add(Jvm.current().javaExecutable.absolutePath)
            add("-cp")
            add((bootstrapClasspath + classpath!!).asPath)
            addAll(allJvmArgs)
            add(main!!)
            addAll(args.map(CharSequence::toString))
        }

        if (hasCommandLineExceedMaxLength(processArgs)) {
            processArgs[processArgs.indexOf("-cp") + 1] =
                writePathingJarFile(bootstrapClasspath + classpath!!).path
        }

        return processArgs
    }

    private fun writePathingJarFile(classPath: FileCollection): File {
        val pathingJarFile = File.createTempFile("gradle-javaexec-classpath", ".jar")
        FileOutputStream(pathingJarFile).use { fileOutputStream ->
            JarOutputStream(fileOutputStream, toManifest(classPath)).use { jarOutputStream ->
                jarOutputStream.putNextEntry(ZipEntry("META-INF/"))
            }
        }
        return pathingJarFile
    }

    private fun toManifest(classPath: FileCollection): Manifest {
        val manifest = Manifest()
        val attributes = manifest.mainAttributes
        attributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        attributes.putValue(
            "Class-Path",
            classPath.files.stream().map(File::toURI).map(URI::toString)
                .collect(Collectors.joining(" "))
        )
        return manifest
    }

    private fun hasCommandLineExceedMaxLength(args: List<String>): Boolean {
        // See http://msdn.microsoft.com/en-us/library/windows/desktop/ms682425(v=vs.85).aspx
        // Derived from MAX_ARG_STRLEN as per http://man7.org/linux/man-pages/man2/execve.2.html
        val maxCommandLineLength =
            if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows()) 32767 else 131072
        return args.joinToString(" ").length > maxCommandLineLength
    }
}
