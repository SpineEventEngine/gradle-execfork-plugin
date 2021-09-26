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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.gradle.api.GradleException

/**
 * Object that will copy the inputStream to the outputStream. You can optionally call waitForPattern()
 * to block until the pattern is seen in the given stream
 *
 * @param inputStream the InputStream to copy to the outputFile
 * @param outputStream the outputStream to copy to
 * @param pattern the optional pattern to wait for when calling waitForPattern()
 */
class InputStreamPipe(
    private val inputStream: InputStream,
    private val outputStream: OutputStream,
    private val pattern: String?
) : AutoCloseable {
    private val log: FluentLogger = FluentLogger.forEnclosingClass()

    private val patternLength: Int = pattern?.toByteArray()?.size ?: 0
    private val patternLatch: CountDownLatch = CountDownLatch(if (pattern != null) 1 else 0)
    private val buffer: LinkedList<Int> = LinkedList()
    private val thread: Thread = Thread {

        var byte: Int = inputStream.safeRead()
        while (byte != -1) {
            outputStream.write(byte)
            outputStream.flush()

            if (patternLength == 0 || patternLatch.count == 0L) {
                log.atFiner().log("skipping pattern checking")
            } else if (buffer.size < patternLength - 1) {
                buffer.addLast(byte)
            } else {
                buffer.addLast(byte)
                val bufferStr = String(buffer.map(Int::toByte).toByteArray())

                log.atFiner().log(
                    "Checking if |${bufferStr.replace("\n", "\\n")}| equals |$pattern|."
                )
                if (bufferStr == pattern) {
                    patternLatch.countDown()
                }
                buffer.removeFirst()
            }

            byte = inputStream.safeRead()
        }
        close()
    }

    init {
        thread.start()
    }

    /**
     * Block until the pattern has been seen in the InputStream
     */
    fun waitForPattern() {
        patternLatch.await()
    }

    /**
     * Block until the pattern has been seen in the InputStream
     *
     * @param timeout the maximum number of TimeUnits to wait
     * @param unit the unit of time to wait
     */
    fun waitForPattern(timeout: Long, unit: TimeUnit) {
        if (!patternLatch.await(timeout, unit)) {
            throw GradleException(
                "The `waitForOutput` pattern did not appear before timeout was reached."
            )
        }
    }

    /**
     * Close the outputFile
     */
    override fun close() {
        log.atFiner().log("Closing given `OutputStream`.")
        outputStream.close()
    }
}

fun InputStream.safeRead(): Int {
    return try {
        read()
    } catch (e: IOException) {
        if (e.message == "Stream closed") {
            -1
        } else {
            throw e
        }
    }
}
