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

import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.gradle.api.GradleException
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Test

class PortUtilsTest {
    @Test
    fun testFindOpenPort() {
        val port = findOpenPort()
        assertThat(port, greaterThanOrEqualTo(1024))
        assertThat(port, lessThanOrEqualTo(65535))

        try {
            Socket(InetAddress.getLoopbackAddress(), port).use { fail("Socket should not have been in use already!") }
        } catch (e: ConnectException) {
            assertThat(e.message, containsString("Connection refused"))
        }
    }

    @Test(timeout=2000)
    fun testWaitForPortOpen_timeout() {
        val stubProcess:Process = StubProcess()
        val port = findOpenPort()

        try {
            waitForPortOpen(port, 1, TimeUnit.SECONDS, stubProcess)
        } catch (e:Exception) {
            assertThat(e, instanceOf(GradleException::class.java))
            assertThat(e.message, equalTo("Timed out waiting for port $port to be opened."))
        }
    }

    @Test(timeout=2000)
    fun testWaitForPortOpen_processDied() {
        val stubProcess:Process = StubProcess(false)
        val port = findOpenPort()

        try {
            waitForPortOpen(port, 1, TimeUnit.MINUTES, stubProcess)
        } catch (e:Exception) {
            assertThat(e, instanceOf(GradleException::class.java))
            assertThat(e.message, equalTo("Process died before port $port was opened."))
        }
    }

    @Test(timeout=2000)
    fun testWaitForPortOpen_success() {
        val stubProcess: Process = StubProcess()
        val port = findOpenPort()
        val latch = CountDownLatch(1)

        Thread({
            ServerSocket(port, 1, InetAddress.getLoopbackAddress()).use {
                it.accept()
                latch.countDown()
            }
        }).start()

        waitForPortOpen(port, 1, TimeUnit.MINUTES, stubProcess)
        latch.await(1, TimeUnit.SECONDS)
        assertThat(latch.count, equalTo(0L))
    }

    class StubProcess(val alive:Boolean = true) : Process() {
        override fun destroy() {}
        override fun exitValue(): Int { return 0 }
        override fun getOutputStream(): OutputStream? { return null }
        override fun getErrorStream(): InputStream? { return null }
        override fun getInputStream(): InputStream? { return null }
        override fun waitFor(): Int { return 0 }

        override fun isAlive():Boolean {
            return alive
        }
    }
}
