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
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class `'ExecForkPlugin' should` {

    @Test
    fun `create 'stopTask'`() {
        val project: Project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.spine.execfork")

        val opts = hashMapOf("type" to JavaExecFork::class.java)
        project.task(opts, "startTestTask")

        val startTask = project.tasks.getByName("startTestTask")
        assertThat(startTask)
            .isInstanceOf(JavaExecFork::class.java)
        val forkTask = startTask as AbstractExecFork

        val stopTask = project.tasks.getByName("stopTestTask")
        assertThat(stopTask)
            .isInstanceOf(ExecJoin::class.java)

        val joinTask = stopTask as ExecJoin
        assertThat(joinTask.forkTask)
            .isSameInstanceAs(forkTask)
        assertThat(forkTask.joinTask)
            .isSameInstanceAs(joinTask)
    }
}
