package com.pjcampi.gradle.thrift.extension

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import java.io.File

open class ThriftExtension(project: Project) {

    companion object {

        const val NAME = "thrift"

        const val EXECUTABLE_EXTENSION = "executable"
        const val EXECUTABLE_DEPENDENCY_NAME = "thriftExecutable"

        const val SCHEMA_DEPENDENCY_NAME = "thrift"

        const val THRIFT_SOURCE = "thrift"
        const val THRIFT_COMPILED_SOURCE = "thriftCompiled"
    }

    val executableExtension: ExecutableExtension
        get() {
            val extensions = (this as ExtensionAware).extensions
            if (extensions.findByName(EXECUTABLE_EXTENSION) == null) {
                extensions.create(EXECUTABLE_EXTENSION, ExecutableExtension::class.java)
            }
            return extensions.getByName(EXECUTABLE_EXTENSION) as ExecutableExtension
        }

    fun executable(configure: ExecutableExtension.() -> Unit) {
        configure(executableExtension)
    }

    var extractOutputDirectory: File
        get() = extractOutputDirectoryProperty.get().asFile
        set(value) = extractOutputDirectoryProperty.set(value)

    @Suppress("UnstableApiUsage")
    internal val extractOutputDirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("download/thrift")
    )

    var compileOutputDirectory: File
        get() = compileOutputDirectoryProperty.get().asFile
        set(value) = compileOutputDirectoryProperty.set(value)

    @Suppress("UnstableApiUsage")
    internal var compileOutputDirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.get().dir("generated-sources/thrift")
    )

    fun sourceSetOptions(config: NamedDomainObjectContainer<CompileExtension>.() -> Unit) {
        sourceSetOptions.configure(object : Closure<Unit>(this, this) {
            fun doCall() {
                (delegate as? NamedDomainObjectContainer<CompileExtension>)?.let {
                    config(it)
                }
            }
        })
    }

    fun sourceSetOptions(config: Closure<Unit>) {
        sourceSetOptions.configure(config)
    }

    val sourceSetOptions = project.container(CompileExtension::class.java) { name ->
        CompileExtension(name, project)
    }

}
