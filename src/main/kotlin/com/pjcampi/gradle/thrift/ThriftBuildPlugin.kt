package com.pjcampi.gradle.thrift

import com.pjcampi.gradle.thrift.extension.ThriftExtension
import com.pjcampi.gradle.thrift.tasks.CompileThriftTask
import com.pjcampi.gradle.thrift.tasks.ExtractThriftTask
import com.pjcampi.gradle.thrift.utils.getOrCreateSource
import com.pjcampi.gradle.thrift.utils.ifAdded
import com.pjcampi.gradle.thrift.utils.sourceSets
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskContainer

@Suppress("UnstableApiUsage")
open class ThriftBuildPlugin : Plugin<Project> {

    companion object {
        const val THRIFT_TASK_GROUP = "Thrift"
        const val THRIFT_PLUGIN_NAME = "thrift-build-plugin"

        val logger: Logger = Logging.getLogger(ThriftBuildPlugin::class.java)

        val multipleCompileTaskPerLanguage = mapOf(
            "java" to listOf("java", "kotlin", "groovy")
        )
    }

    override fun apply(target: Project) {
        var pluginWasApplied = false
        target.plugins.withType(JavaPlugin::class.java) {
            logger.info("Applying $THRIFT_PLUGIN_NAME")
            doApply(target)
            pluginWasApplied = true
        }

        if (!pluginWasApplied) {
            logger.warn(
                "$THRIFT_PLUGIN_NAME builds upon functionality exposed by the 'java' gradle plugin " +
                        "(even for compiling other languages like python). " +
                        "It must therefore be applied for the plugin to be applied."
            )
        }
    }

    open fun addCompiledSourceDirectoryToCompileTasks(
        taskContainer: TaskContainer,
        sourceSet: SourceSet,
        compiledDirectorySet: SourceDirectorySet,
        language: String
    ): Boolean {
        var notAdded = true
        for (languageToCompile in multipleCompileTaskPerLanguage.getOrDefault(language, listOf(language))) {
            val compileTaskName = sourceSet.getCompileTaskName(languageToCompile)

            // find compile task
            notAdded = !taskContainer.ifAdded(compileTaskName) {
                try {
                    (this as SourceTask).source(compiledDirectorySet)
                    logger.info("Added '${compiledDirectorySet.displayName}' to '$compileTaskName'")
                } catch (e: Exception) {
                    logger.warn("I could not add compiled thrift data to '$compileTaskName'. Exception: '$e'")
                }
            } && notAdded
        }
        return !notAdded
    }

    private fun doApply(target: Project) {

        // add the thrift extension
        val thriftExtension = target.extensions.create(ThriftExtension.NAME, ThriftExtension::class.java, target)

        // add a dependency to download the executable artifact
        if (thriftExtension.executableExtension.artifact == null) {
            logger.warn(
                "No thrift executable artifact configured. " +
                        "Thrift executable will be called as 'thrift' and must therefore be part of the build path."
            )
        } else {
            addDependencyForExecutable(target, thriftExtension.executableExtension.artifact!!)
        }

        // configure extensions and tasks per sourceSet
        target.sourceSets.configureEach {

            logger.info("Configuring 'thrift' for source set: $name")

            // add a CompileExtension for the SourceSet to configure compilation specific data
            val compileExtension = thriftExtension.sourceSetOptions.create(name) {
                outputDirectoryProperty.convention(thriftExtension.compileOutputDirectoryProperty.dir(this@configureEach.name))
            }

            // add configuration to register external thrift schema dependencies
            val thriftConfiguration = getTaskName(null, ThriftExtension.SCHEMA_DEPENDENCY_NAME)
            val configuration = target.configurations.register(thriftConfiguration) {
                isVisible = false
            }

            // create task to extract thrift schemas from registered dependencies
            val extractTaskName = getTaskName("extract", "thrift")
            val extractThriftTask = target.tasks.register(extractTaskName, ExtractThriftTask::class.java) {

                group = THRIFT_TASK_GROUP
                description = "Extract thrift schemas from dependencies specified in thrift configurations."

                inputFiles.from(configuration)
                outputDirectory.set(thriftExtension.extractOutputDirectoryProperty.dir(this@configureEach.name))
            }

            // create a source to manage thrift schemas and add extracted data to it
            val thriftDirectorySet =
                getOrCreateSource(ThriftExtension.THRIFT_SOURCE, target.objects, name, "$name thrift source")
                    .apply {
                        include("**/*.thrift")
                        srcDir(extractThriftTask.flatMap { it.outputDirectory })
                    }

            // create task to compile selected thrift schemas
            val compileTaskName = getTaskName("compile", "thrift")
            val compileThriftTask = target.tasks.register(compileTaskName, CompileThriftTask::class.java) {

                group = THRIFT_TASK_GROUP
                description = "Compiles thrift schemas for into builtin languages specified for."

                inputFiles.from(thriftDirectorySet.matching { include(compileExtension.files) })
                includeDirs.from(thriftDirectorySet.sourceDirectories)

                target.configurations.findByName(ThriftExtension.EXECUTABLE_DEPENDENCY_NAME)?.apply {
                    executable = this.singleFile.absolutePath
                }
                executableOptions = thriftExtension.executableExtension.options

                generators = mapOf(Pair(compileExtension.generator, compileExtension.options))

                outputDirectory.set(compileExtension.outputDirectoryProperty)
                createGenFolder = false
            }

            // create a source to manage files compiled by thrift and add compiled data to it
            val compiledDirectorySet =
                getOrCreateSource(
                    ThriftExtension.THRIFT_COMPILED_SOURCE,
                    target.objects,
                    name,
                    "$name thrift compiled source"
                )
                    .apply {
                        srcDir(compileThriftTask.flatMap { it.outputDirectory })
                    }

            // add dependencies to clean task
            target.tasks.named("clean") {
                dependsOn("clean${extractTaskName.capitalize()}")
                dependsOn("clean${compileTaskName.capitalize()}")
            }

            // NOTE: the "language" must be resolved before we add the compiled data to the relevant source for the
            // language. By that time e.g. the JavaCompile task is created so I can't just add a srcDir to the language
            // sourceDirectorySet. Instead I have to create a new sourceDirectorySet and add it to compilation task.
            target.afterEvaluate {
                val added = addCompiledSourceDirectoryToCompileTasks(
                    target.tasks, this@configureEach, compiledDirectorySet, compileExtension.language
                )

                if (!added) {
                    logger.info(
                        "no compile task was registered for '${compileExtension.language}' so " +
                                "'$compileTaskName' as not been added to the gradle lifecycle and must be added manually." +
                                "The compiled files can be found at: '${compiledDirectorySet.sourceDirectories.singleFile.absolutePath}'." +
                                "Data has been added to a new extension called compiledThrift."
                    )
                }
            }
        }
    }

    private fun addDependencyForExecutable(target: Project, artifact: Any) {
        target.configurations.register(
            ThriftExtension.EXECUTABLE_DEPENDENCY_NAME
        )
        target.dependencies.add(
            ThriftExtension.EXECUTABLE_DEPENDENCY_NAME,
            artifact
        )
    }
}