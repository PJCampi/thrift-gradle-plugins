package com.pjcampi.gradle.thrift

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test

class ThriftBuildPluginTest : BaseTest() {

    @Test
    fun `java project all default`(){
        getResourceFile("MyService.kt").copyTo(testProjectDir.root.resolve("src/main/kotlin/MyService.kt"))
        getResourceFile("builds/allDefault.txt").copyTo(buildFile, true)
        copyFilesToSourceSetResources("main")
        val result = build("build", "clean", "-i")
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":extractThrift")?.outcome)
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":compileThrift")?.outcome)
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlin")?.outcome)

        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":cleanExtractThrift")?.outcome)
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":cleanCompileThrift")?.outcome)
    }

    @Test
    fun `java project all custom`(){
        getResourceFile("MyService.kt").copyTo(testProjectDir.root.resolve("src/main/kotlin/MyService.kt"))
        getResourceFile("builds/allCustom.txt").copyTo(buildFile, true)
        copyFilesToSourceSetResources("main")
        val result = build("build", "-i")
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":extractThrift")?.outcome)
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":compileThrift")?.outcome)
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlin")?.outcome)

        Assert.assertEquals(
            "number of schemas",
            2,
            testProjectDir.root.resolve("src/main/thrift/schemas/main/thrift").listFiles()!!.size
        )
        Assert.assertEquals(
            "number of compiled files",
            2,
            testProjectDir.root.resolve("src/main/thrift/compiled/test").listFiles()!!.size
        )
        Assert.assertTrue(
            "options should be applied correctly to thrift command",
            result.output.contains("Command: thrift -v --gen java:fullcamel,beans")
        )
    }

    @Test
    fun `python project no compile`(){
        getResourceFile("builds/noCompileTask.txt").copyTo(buildFile, true)
        getResourceFile("thrift").copyTo(testProjectDir.root.resolve("src/main/resources/thrift"))
        getResourceFile("thriftSchemas").copyRecursively(testProjectDir.root.resolve("src/main/thrift"))
        val result = build("compileThrift", "-i")
        Assert.assertEquals(TaskOutcome.NO_SOURCE, result.task(":extractThrift")?.outcome)
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":compileThrift")?.outcome)

        Assert.assertEquals(
            "number of compiled files: ",
            5,
            testProjectDir.root.resolve("src/main/python/test").listFiles()!!.size
        )
    }

    private fun copyFilesToSourceSetResources(sourceSet: String) {
        getResourceFile("schemas.zip").copyTo(testProjectDir.root.resolve("src/$sourceSet/resources/schemas.zip"))
        getResourceFile("thrift").copyTo(testProjectDir.root.resolve("src/$sourceSet/resources/thrift"))
    }

}