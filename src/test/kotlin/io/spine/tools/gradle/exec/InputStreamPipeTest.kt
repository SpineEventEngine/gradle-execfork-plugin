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

import com.google.common.truth.Truth.assertThat
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.CountDownLatch
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class `'InputStreamPipe' should` {
    private val outputStream: PipedOutputStream = PipedOutputStream()
    private val outputBuffer: BufferedWriter = outputStream.bufferedWriter()
    private val inputStream: InputStream = PipedInputStream(outputStream)
    private val pipeOutput = ByteArrayOutputStream()
    private val waitForPattern = "Server Started!"
    private val logger = InputStreamPipe(inputStream, pipeOutput, waitForPattern)
    private val latch: CountDownLatch = CountDownLatch(1)

    @AfterEach
    fun cleanup() {
        outputBuffer.close()
        outputStream.close()
    }

    @Test
    fun `copy 'InputStream' to 'OutputStream'`() {
        shouldFindPatternFromLines(
            "Line One",
            "Line Two",
            "Line Three",
            "Line Four",
            "Server Started!",
            "Line Five",
            "Line Six"
        )
    }

    @Test
    fun `find in last line`() {
        shouldFindPatternFromLines("Line One", "Line Two", "Server Started!")
    }

    @Test
    fun `find in first line`() {
        shouldFindPatternFromLines("Server Started!", "Line Two", "Line Three")
    }

    private fun shouldFindPatternFromLines(vararg lines: String) {
        Thread {
            lines.forEach { line -> writeLine(line, 100) }
            latch.countDown()
        }.start()
        logger.waitForPattern()

        val outputFileContents: List<String> = splitAndRemoveExtraEmptyString()
        assertThat(outputFileContents)
            .isEqualTo(allLinesUntilPattern(lines))

        latch.await()

        val outputFileContentsTwo: List<String> = splitAndRemoveExtraEmptyString()
        assertThat(outputFileContentsTwo)
            .containsExactlyElementsIn(lines)
    }

    private fun allLinesUntilPattern(lines: Array<out String>): List<String> {
        val takeWhile: MutableList<String> =
            lines.takeWhile { i -> i != "Server Started!" }
                .toMutableList()
        takeWhile.add("Server Started!")
        return takeWhile.toList()
    }

    private fun splitAndRemoveExtraEmptyString() =
        String(pipeOutput.toByteArray())
            .split(System.lineSeparator())
            .filter { i -> i != "" }

    private fun writeLine(output: String, postDelay: Long) {
        with(outputBuffer) {
            append(output)
            append(System.lineSeparator())
            flush()
        }
        Thread.sleep(postDelay)
    }
}
