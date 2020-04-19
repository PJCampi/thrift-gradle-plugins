package com.pjcampi.gradle.thrift

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

open class BaseTest {

    @Rule
    @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()
    protected lateinit var settingsFile: File
    protected lateinit var buildFile: File

    @Before
    fun setup() {
        settingsFile = testProjectDir.newFile("settings.gradle.kts")
        buildFile = testProjectDir.newFile("build.gradle.kts")
    }

    protected fun build(vararg arguments: String, withFailure: Boolean = false): BuildResult {
        val runner = GradleRunner
            .create()
            .withProjectDir(testProjectDir.root)
            .withPluginClasspath()
            .withArguments(*arguments)
        return if (withFailure) runner.buildAndFail() else runner.build()
    }

    protected fun getResourceFile(name: String): File  = File(javaClass.classLoader.getResource(name)!!.toURI())
}