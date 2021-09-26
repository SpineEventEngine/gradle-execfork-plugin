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

import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.After
import java.io.*
import java.util.concurrent.CountDownLatch

class InputStreamPipeTest {
    val outputStream: PipedOutputStream = PipedOutputStream()
    val outputBuffer: BufferedWriter = outputStream.bufferedWriter()
    val inputStream: InputStream = PipedInputStream(outputStream)
    val pipeOutput = ByteArrayOutputStream()
    val waitForPattern = "Server Started!"
    val logger = InputStreamPipe(inputStream, pipeOutput, waitForPattern)
    val latch: CountDownLatch = CountDownLatch(1)

    @Test
    fun shouldCopyInputStreamToOutputStream() {
        shouldFindPatternFromLines("Line One", "Line Two", "Line Three", "Line Four", "Server Started!", "Line Five", "Line Six")
    }

    @Test
    fun shouldFindInLastLine() {
        shouldFindPatternFromLines("Line One", "Line Two", "Server Started!")
    }

    @Test
    fun shouldFindInFirstLine() {
        shouldFindPatternFromLines("Server Started!","Line Two","Line Three")
    }

    private fun shouldFindPatternFromLines(vararg lines: String) {
        Thread({
            lines.forEach { line -> writeLine(line, 100) }
            latch.countDown()
        }).start()
        logger.waitForPattern()

        val outputFileContents:List<String> = splitAndRemoveExtraEmptyString()
        val msg = "outputFileContents: ${outputFileContents.joinToString(separator = System.lineSeparator())}"

        assertThat(msg, outputFileContents, `is`(allLinesUntilPattern(lines)))

        latch.await()

        val outputFileContentsTwo:List<String> = splitAndRemoveExtraEmptyString()
        val msgTwo = "outputFileContents: ${outputFileContents.joinToString(separator = System.lineSeparator())}"
        assertThat(msgTwo, outputFileContentsTwo, contains(*lines))
    }

    private fun allLinesUntilPattern(lines: Array<out String>): List<String> {
        val takeWhile: MutableList<String> = lines.takeWhile { i -> i != "Server Started!" }.toMutableList()
        takeWhile.add("Server Started!")
        return takeWhile.toList()
    }

    private fun splitAndRemoveExtraEmptyString() = String(pipeOutput.toByteArray()).split(System.lineSeparator()).filter { i -> i != "" }

    @After
    fun cleanup() {
        outputBuffer.close()
        outputStream.close()
    }

    private fun writeLine(output:String, postDelay:Long) {
        outputBuffer.appendLine(output)
        outputBuffer.flush()
        Thread.sleep(postDelay)
    }
}
