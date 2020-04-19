package com.pjcampi.gradle.thrift.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction


@Suppress("UnstableApiUsage")
open class ExtractThriftTask : DefaultTask() {

    @SkipWhenEmpty
    @InputFiles
    val inputFiles = project.objects.fileCollection()

    @OutputDirectory
    val outputDirectory = project.objects.directoryProperty()

    @TaskAction
    fun extract() {
        val outputDirectory = outputDirectory.asFile.get()
        inputFiles.forEach {
            logger.debug("Extracting thrift files from $it to $outputDirectory")
            when {
                (it.isDirectory) -> {
                    project.copy {
                        from(it.path) { include("**/*.thrift") }
                        into(outputDirectory)
                        includeEmptyDirs = false
                    }
                }
                (it.path.endsWith(".thrift")) -> {
                    project.copy {
                        from(it.path)
                        into(outputDirectory)
                        includeEmptyDirs = false
                    }
                }
                (it.path.endsWith(".jar")
                        || it.path.endsWith(".zip")) -> {
                    project.copy {
                        from(project.zipTree(it.path)) { include("**/*.thrift") }
                        into(outputDirectory)
                        includeEmptyDirs = false
                    }
                }
                (it.path.endsWith(".tar")
                        || it.path.endsWith(".tar.gz")
                        || it.path.endsWith(".tar.bz2")
                        || it.path.endsWith(".tgz")) -> {
                    project.copy {
                        from(project.tarTree(it.path)) { include("**/*.thrift") }
                        into(outputDirectory)
                        includeEmptyDirs = false
                    }
                }
                else -> {
                    logger.warn("Skipping unsupported file type '${it.path}'; handles only jar, tar, tar.gz, tar.bz2 & tgz")
                }
            }
        }
    }
}