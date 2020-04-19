package com.pjcampi.gradle.thrift.extension

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import java.io.File

@Suppress
open class CompileExtension(
    val name: String,
    project: Project
) {

    var language = "java"

    var files: List<String> = listOf("**/*.thrift")

    var options: List<String> = listOf()

    var outputDirectory: File
        get() = outputDirectoryProperty.get().asFile
        set(value) = outputDirectoryProperty.set(value)

    @Suppress("UnstableApiUsage")
    internal val outputDirectoryProperty: DirectoryProperty = project.objects.directoryProperty()

    val generator: String
        get() = when (language) {
            "python" -> "py"
            "javascript" -> "js"
            else -> language
        }
}
