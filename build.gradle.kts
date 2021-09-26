import org.gradle.api.tasks.testing.logging.TestExceptionFormat

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

plugins {
    id("com.gradle.plugin-publish").version("0.14.0")
    id("org.jetbrains.kotlin.jvm").version("1.5.30")
    id("idea")
    id("maven-publish")
    id("java-gradle-plugin")
}

group = "io.spine.tools"
version = File(rootDir, "VERSION").readText().trim()

java {
    toolchain {
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.30")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.30")

    testImplementation("junit:junit:4.12")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
}

pluginBundle {
    website = "http://spine.io"
    vcsUrl = "https://github.com/SpineEventEngine/gradle-execfork-plugin"
    description = "Execute Java or shell processes in the background during a build"
    tags = listOf("java", "exec", "background", "process")

    (plugins) {
        create("execForkPlugin") {
            id = "io.spine.execfork"
            displayName = "Gradle Exec Fork Plugin"
        }
    }
}

tasks {
    val sampleProjects by creating(GradleBuild::class) {
        buildFile = File("${project.rootDir}/sample_projects/build.gradle")
        tasks = listOf("clean", "build")
    }
    sampleProjects.dependsOn("publishToMavenLocal")
    "test" { finalizedBy(sampleProjects) }
    named<Test>("test") {
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
}

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from("javadoc")
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

artifacts {
    add("archives", javadocJar)
    add("archives", sourcesJar)
}
