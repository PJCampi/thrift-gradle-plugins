package com.pjcampi.gradle.thrift.tasks

import com.pjcampi.gradle.thrift.BaseTest
import org.gradle.api.GradleException
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.regex.Pattern


@Suppress("SameParameterValue")
internal class CompileThriftTaskTest : BaseTest() {

    @Test
    fun `test incremental build with change of input files`() {

        // make thrift files
        val files = mutableListOf<File>()
        for (i in 0..2) {
            val file = testProjectDir.root.resolve("$i.thrift")
            file.writeText(makeThriftString("Struct$i"))
            files.add(file)
        }

        // initial compilation
        buildFile.writeText(
            makeBuildString(files.take(2).map { it.path })
        )
        var result = build("testCompilation", "-i")

        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":testCompilation")?.outcome)
        for ((file, count) in files.zip(listOf<Long>(1, 1, 0))) {
            assertCompiledNTimes(result.output, file.path, count)
        }
        assertNFilesCompiled(2)


        // rerun same compilation should yield up-to-date
        result = build("testCompilation")
        Assert.assertEquals(TaskOutcome.UP_TO_DATE, result.task(":testCompilation")?.outcome)

        // adding a file should result in only that file being be compiled
        buildFile.writeText(
            makeBuildString(files.map { it.path })
        )
        result = build("testCompilation", "-i")
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":testCompilation")?.outcome)
        for ((file, count) in files.zip(listOf<Long>(0, 0, 1))) {
            assertCompiledNTimes(result.output, file.path, count)
        }

        // editing a file should result in that file being compiled alone
        files[0].writeText(
            makeThriftString("Struct4")
        )
        result = build("testCompilation", "-i")
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":testCompilation")?.outcome)
        for ((file, count) in files.zip(listOf<Long>(1, 0, 0))) {
            assertCompiledNTimes(result.output, file.path, count)
        }

        // removing a file should result in everything being recompiled
        buildFile.writeText(
            makeBuildString(files.take(2).map { it.path })
        )
        result = build("testCompilation", "-i")
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":testCompilation")?.outcome)
        for ((file, count) in files.zip(listOf<Long>(1, 1, 0))) {
            assertCompiledNTimes(result.output, file.path, count)
        }
    }

    @Test
    fun `test incremental build with changes of include files`() {

        val file = testProjectDir.root.resolve("1.thrift")
        file.writeText(makeThriftString("Struct1", listOf("include1.thrift")))

        val includeDir = testProjectDir.root.resolve( "include")
        includeDir.mkdir()
        val includeFile = includeDir.resolve( "include1.thrift")
        includeFile.writeText(makeThriftString("Struct2"))

        // initial compilation
        buildFile.writeText(
            makeBuildString(listOf(file.absolutePath), listOf(includeDir.absolutePath))
        )
        var result = build("testCompilation", "-i")
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":testCompilation")?.outcome)
        assertCompiledNTimes(result.output, file.path, 1)
        assertCompiledNTimes(result.output, includeFile.path, 0)

        // add file to include dir
        includeDir.resolve( "include2.thrift").writeText(makeThriftString("Struct3"))
        result = build("testCompilation", "-i")
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":testCompilation")?.outcome)
        assertCompiledNTimes(result.output, file.path, 0)
        assertCompiledNTimes(result.output, includeFile.path, 0)

        // edit include file (does not compile any longer): task should fail
        includeFile.writeText(makeThriftString("Struct2").replace("struct", "stuct"))
        result = build("testCompilation", "-i", withFailure = true)
        Assert.assertEquals(TaskOutcome.FAILED, result.task(":testCompilation")?.outcome)
    }

    @Test
    fun `test build from input dir`() {

        // make directory
        val thriftDirectory = testProjectDir.root.resolve( "thrift")
        thriftDirectory.mkdir()

        // make thrift files
        val files = mutableListOf<File>()
        for (i in 0..1) {
            val file = testProjectDir.root.resolve( "thrift/$i.thrift")
            file.writeText(makeThriftString("Struct$i"))
            files.add(file)
        }

        // initial compilation
        buildFile.writeText(
            makeBuildString(listOf(thriftDirectory.absolutePath))
        )
        val result = build("testCompilation", "-i")

        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":testCompilation")?.outcome)
        for ((file, count) in files.zip(listOf<Long>(1, 1))) {
            assertCompiledNTimes(result.output, file.path, count)
        }
        assertNFilesCompiled(2)

    }

    private fun makeBuildString(
        inputFiles: List<String>,
        includeFiles: List<String> = listOf()
    ): String {

        val thrift = getResourceFile("thrift")
        val inputFilesString =
            if (inputFiles.isNotEmpty()) "inputFiles.from(\"${inputFiles.joinToString("\", \"")}\")" else ""
        val includeFilesString =
            if (includeFiles.isNotEmpty()) "includeDirs.from(\"${includeFiles.joinToString("\", \"")}\")" else ""

        return """
            import com.pjcampi.gradle.thrift.tasks.CompileThriftTask
            
            plugins {
                id("com.pjcampi.gradle.thrift.thrift-build-plugin")
                java
            }
            
            thrift {
                executable {
                    artifact = "${thrift.absolutePath}"
                }
            }
            
            tasks {
                register("testCompilation", CompileThriftTask::class.java) {
                    outputDirectory.set(project.layout.buildDirectory.dir("generated-sources/thrift"))
                    $inputFilesString
                    $includeFilesString
                }
            }
            """.trimIndent()
    }

    private fun makeThriftString(
        name: String = "Struct",
        include: List<String> = listOf()
    ): String {
        val includeString = include.joinToString("\n", transform = { "include \"$it\"" })
        return """
            namespace * test
            
            $includeString

            struct ${name.capitalize()} {
            	1:required i32 num = 0,
            }
        """.trimIndent()
    }

    private fun assertCompiledNTimes(output: String, filePath: String, expectedCount: Long) {
        val pattern = Pattern.compile("Command: thrift.*$filePath$", Pattern.MULTILINE)
        val count = pattern.matcher(output).results().count()
        Assert.assertEquals(
            "number of times '$filePath' is applied",
            expectedCount,
            count
        )
    }

    private fun assertNFilesCompiled(expectedCount: Int) {
        Assert.assertEquals(
            "number of compiled files",
            expectedCount,
            testProjectDir.root.resolve( "build/generated-sources/thrift/gen-java/test").listFiles()!!.size
        )
    }
}