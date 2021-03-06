# gradle-execfork-plugin

A Gradle plugin for running background processes during a build. 
Both standard executables and Java classes are supported.

Based and inspired by [`psxpaul/gradle-execfork-plugin`][original-work].

## Running an executable

```groovy
plugins {
  id 'io.spine.execfork' version '0.1.16'
}

task startDaemon(type: io.spine.gradle.task.ExecFork) {
    executable = './MainScript.sh'
    args = [ '-d', '/foo/bar/data', '-v', '-l', '3' ]
    workingDir = "$projectDir/src/main/bash"
    standardOutput = "$buildDir/daemon.log"
    errorOutput = "$buildDir/daemon-error.log"
    stopAfter = verify
    waitForPort = 8080
    waitForOutput = 'has started'
    environment = ['JAVA_HOME': "$buildDir/java", 'USER_HOME': "$buildDir/userhome"]
}
```

## Running a Java class

```groovy
plugins {
  id 'io.spine.execfork' version '0.1.16'
}

task startDaemon(type: io.spine.gradle.task.JavaExecFork) {
    classpath = sourceSets.main.runtimeClasspath
    main = 'com.sample.application.MainApp'
    args = [ '-d', '/foo/bar/data', '-v', '-l', '3' ]
    jvmArgs = [ '-Xmx500m', '-Djava.awt.headless=true' ]
    workingDir = "$buildDir/server"
    standardOutput = "$buildDir/daemon.log"
    errorOutput = "$buildDir/daemon-error.log"
    stopAfter = verify
    waitForPort = 8443
    waitForOutput = 'has started'
    environment 'JAVA_HOME', "$buildDir/java"
}
```

## Supported Properties

### `ExecFork`:

Name | Type | Description
--- | --- | ---
`workingDir` | `String` | *Optional.* The path of the working directory to run the executable from. This is treated as a relative path, so specifying an absolute path should be preferred. Default: `project.projectDir.absolutePath`.
`args` | `List<String>` | *Optional.* A list of arguments to give to the executable.
`standardOutput` | `String` | *Optional.* The path of the file to write standard output to. If none is specified, process output is written to Gradle's console output.
`errorOutput` | `String` | *Optional.* The path of the file to write error output to. If none is specified, the error output is directed to the same destination as the standard output.
`waitForPort` | `Int` | *Optional.* A port number to watch for to be open. Until opened, the task will block. If none is specified, the task will return immediately after launching the process.
`waitForOutput` | `String` | *Optional.* A string to look for in `standardOutput`. The task will block until this pattern appeared or the timeout is reached. If not specified, the task will return immediately after launching the process.
`timeout` | `Long` | *Optional.* The maximum number of seconds associated with the `waitForPort` or `waitForOutput` task. Default: `60`.
`stopAfter` | `org.gradle.api.Task` | *Optional.* A task that, when finished, will cause the process to stop. If none is specified, the process will stop at the very end of a build (whether successful or not).
`executable` | `String` | *Required.* The path to the executable.
`environment` | Two `String`s OR one `Map<String, String>` | *Optional.* Environment variables to launch the executable with. You can either assign a `Map` with the '`=`' operator, or pass 2 `String`s as key/value to the function. Note that multiple calls to this function are supported.
`forceKill` | `Boolean` | *Optional.* Kills the process foricbly. Forcible process destruction is defined as the immediate termination of a process, whereas normal termination allows the process to shut down cleanly.
`killDescendants` | `Boolean` | *Optional.* Requires Java 9+. Kill all descendents of the started process. Default: `true`.


### `JavaExecFork`:

Name | Type | Description
--- | --- | ---
`workingDir` | `String` | *Optional.* The path of the working directory to run the executable from. This is treated as a relative path, so specifying an absolute path should be preferred. Default: `project.projectDir.absolutePath`.
`args` | `List<String>` | *Optional.* A list of arguments to give to the executable.
`standardOutput` | `String` | *Optional.* The path of the file to write standard output to. If none is specified, process output is written to gradle's console output.
`errorOutput` | `String` | *Optional.* The path of the file to write error output to. If none is specified, the error output is directed to the same destination as the standard output.
`waitForPort` | `Int` | *Optional.* A port number to watch for to be open. Until opened, the task will block. If none is specified, the task will return immediately after launching the process.
`waitForOutput` | `String` | *Optional.* A string to look for in `standardOutput`. The task will block until this pattern appeared or the timeout is reached. If not specified, the task will return immediately after launching the process.
`timeout` | `Long` | *Optional.* The maximum number of seconds associated with the `waitForPort` or `waitForOutput` task. Default: `60`.
`stopAfter` | `org.gradle.api.Task` | *Optional.* A task that, when finished, will cause the process to stop. If none is specified, the process will stop at the very end of a build (whether successful or not).
`classpath` | `org.gradle.api.file.FileCollection` | *Required.* The classpath to use to launch the Java `main` class.
`main` | `String` | *Required.* The qualified name of the main Java class to execute.
`jvmArgs` | `List<String>` | *Optional.* The list of arguments to give to the JVM when launching the Java `main` class.
`environment` | Two `String`s OR one `Map<String, String>` | *Optional.* Environment variables to launch the Java `main` class with. You can either assign a `Map` with the '`=`' operator, or pass 2 Strings as key/value to the function. Note that multiple calls to this function are supported.
`forceKill` | `Boolean` | *Optional.* Kills the process foricbly. Forcible process destruction is defined as the immediate termination of a process, whereas normal termination allows the process to shut down cleanly.
`killDescendants` | `Boolean` | *Optional.* Kill all descendents of the started process. Default: `true`.

## Compatibility

Gradle Version | ExecFork version
--- | ---
7.2 | 0.1.16

[original-work]: https://github.com/psxpaul/gradle-execfork-plugin
