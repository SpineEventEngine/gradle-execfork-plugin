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

import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class ExecJoinTest {
    @Test
    fun testCreateNameFor() {
        assertName("startJohnnie", "stopJohnnie")
        assertName("johnnie_start", "johnnie_stop")
        assertName("johnnieStart", "johnnieStop")
        assertName("johnnie_Start", "johnnie_Stop")
        assertName("johnnieSTART", "johnnieSTOP")
        assertName("STARTjohnnie", "STOPjohnnie")

        assertName("runJohnnie", "stopJohnnie")
        assertName("johnnie_run", "johnnie_stop")
        assertName("johnnieRun", "johnnieStop")
        assertName("johnnie_Run", "johnnie_Stop")
        assertName("johnnieRUN", "johnnieSTOP")
        assertName("RUNjohnnie", "STOPjohnnie")

        assertName("execJohnnie", "stopJohnnie")
        assertName("johnnie_exec", "johnnie_stop")
        assertName("johnnieExec", "johnnieStop")
        assertName("johnnie_Exec", "johnnie_Stop")
        assertName("johnnieEXEC", "johnnieSTOP")
        assertName("EXECjohnnie", "STOPjohnnie")

        assertName("joseph", "joseph_stop")
    }

    fun assertName(given:String, expected:String) {
        val project = ProjectBuilder.builder().build()
        val startTask = project.tasks.create(given, JavaExecFork::class.java)
        assertThat(createNameFor(startTask), equalTo(expected))
    }
}
