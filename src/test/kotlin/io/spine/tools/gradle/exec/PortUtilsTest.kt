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
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import org.gradle.api.GradleException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail

class `'PortUtils' should` {

    var port: Int = 0

    @BeforeEach
    fun findPort() {
        port = findOpenPort()
    }

    @Test
    fun `find open port`() {
        assertThat(port).isIn(IntRange(1024, 65535))

        try {
            Socket(InetAddress.getLoopbackAddress(), port).use {
                fail("Socket should not have been in use already!")
            }
        } catch (e: ConnectException) {
            assertThat(e.message).contains("Connection refused")
        }
    }

    @Nested
    inner class `wait for open port` {

        private var stubProcess: Process? = null

        @BeforeEach
        fun createProcess() {
            stubProcess = StubProcess()
        }

        @Test
        @Timeout(value = 5, unit = SECONDS) // Allow longer timeout for Windows
        fun `failing on timeout`() {
            val exception = assertThrows<GradleException> {
                waitForPortOpen(port, 1, SECONDS, stubProcess!!)
            }
            assertThat(exception)
                .hasMessageThat()
                .isEqualTo("Timed out waiting for port $port to be opened.")
        }

        @Test
        @Timeout(value = 2000, unit = MILLISECONDS)
        fun `failing when process died`() {
            stubProcess = StubProcess(false)

            val exception = assertThrows<GradleException> {
                waitForPortOpen(port, 1, MINUTES, stubProcess!!)
            }
            assertThat(exception)
                .hasMessageThat()
                .isEqualTo("Process died before port $port was opened.")
        }

        @Test
        @Timeout(value = 2000, unit = MILLISECONDS)
        fun successfully() {
            val latch = CountDownLatch(1)

            Thread {
                ServerSocket(port, 1, InetAddress.getLoopbackAddress()).use {
                    it.accept()
                    latch.countDown()
                }
            }.start()

            waitForPortOpen(port, 1, MINUTES, stubProcess!!)
            latch.await(1, SECONDS)
            assertThat(latch.count)
                .isEqualTo(0L)
        }
    }

    class StubProcess(private val alive: Boolean = true) : Process() {
        override fun destroy() {}
        override fun exitValue(): Int = 0
        override fun getOutputStream(): OutputStream? = null
        override fun getErrorStream(): InputStream? = null
        override fun getInputStream(): InputStream? = null
        override fun waitFor(): Int = 0
        override fun isAlive(): Boolean = alive
    }
}
