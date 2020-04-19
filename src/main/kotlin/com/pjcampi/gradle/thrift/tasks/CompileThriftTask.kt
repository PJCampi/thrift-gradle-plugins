package com.pjcampi.gradle.thrift.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileType
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File

@Suppress("UnstableApiUsage")
open class CompileThriftTask : DefaultTask() {

    @Incremental
    @InputFiles
    val inputFiles = project.files()

    private val inputSchemas: Iterable<File>
        get() = inputFiles.asFileTree.matching { include("**/*.thrift") }

    @Incremental
    @InputFiles
    val includeDirs = project.files()

    @Input
    var executable = "thrift"

    @Input
    var executableOptions = listOf<String>()

    @Input
    var generators: Map<String, List<String>> = mapOf(Pair("java", listOf()))

    @Input
    var createGenFolder: Boolean = true

    @OutputDirectory
    val outputDirectory = project.objects.directoryProperty()

    fun outputDirectoryForGenerator(generator: String): Provider<Directory> {
        return if (createGenFolder) {
            outputDirectory.dir("gen-$generator")
        } else {
            outputDirectory
        }
    }

    @TaskAction
    fun compileThrift(inputChanges: InputChanges) {

        getFilesToCompile(inputChanges).forEach {
            if (it.exists()) {
                compile(it)
            } else {
                logger.warn("$it does not exist and will be ignored for compilation.")
            }
        }
    }

    // NOTE: this method is intended to be open so it can be overriden as my logic may be too conservative
    open fun getFilesToCompile(inputChanges: InputChanges): Iterable<File> {

        if (!inputChanges.isIncremental) {
            logger.debug("Change was not incremental, recompiling all files.")
            return inputSchemas
        }

        val inputFileChanges = inputChanges.getFileChanges(inputFiles).filter {
            it.fileType == FileType.FILE
        }
        val inputFileChangePaths = inputFileChanges.toSet()

        val includeFileChanges = inputChanges.getFileChanges(includeDirs).filter {
            it.fileType == FileType.FILE && !inputFileChangePaths.contains(it)
        }

        // NOTE: there may have been changes in include dirs that affect the compilation of the input items
        // (we are executing thrift in a recursive way). It's hard to know which changes affect compilation of our inputs
        // so to be on the safe side we only do incremental compilation if files have been added. Any input file making
        // use of these new files would be changed and recompiled.
        if (!includeFileChanges.all { it.changeType == ChangeType.ADDED }) {
            return inputSchemas
        }

        // NOTE: if there's been any input removed we want to recompile it all because a deleted file dependency could
        // make a file that has not changed fail to build.
        if (inputFileChanges.any { it.changeType == ChangeType.REMOVED }) {
            return inputSchemas
        }

        return inputFileChanges.map { it.file }
    }

    private fun compile(file: File) {

        val command = makeBaseCommand()
        command += file.absolutePath

        val result = project.exec {
            commandLine = command
        }

        val exitCode = result.exitValue
        if (exitCode != 0)
            throw GradleException("Failed to compile '$file', exit=${exitCode}")
    }

    private fun makeBaseCommand(): MutableList<String> {

        // add executable and outputs
        val command = mutableListOf(
            executable,
            *executableOptions.toTypedArray()
        )

        // add all generators & generator options
        for ((generator, options) in generators) {
            var genString = generator
            if (options.isNotEmpty()) {
                genString = "$genString:${options.joinToString(",")}"
            }
            command += "--gen"
            command += genString
        }

        // add output directory
        command += if (createGenFolder) "-o" else "-out"
        command += outputDirectory.get().asFile.absolutePath

        // add include dirs
        includeDirs.forEach {
            command += "-I"
            command += it.path
        }

        return command
    }
}