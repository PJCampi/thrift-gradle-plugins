package com.pjcampi.gradle.thrift.utils

import com.pjcampi.gradle.thrift.extension.CompileExtension
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer

val DependencyHandler.thrift by DependencyHelper

val DependencyHandler.testThrift by DependencyHelper

val Project.sourceSets: SourceSetContainer
    get() = this.properties["sourceSets"] as SourceSetContainer

@Suppress("UnstableApiUsage")
fun SourceSet.getOrCreateSource(
    name: String,
    objectFactory: ObjectFactory,
    sourceDirectoryName: String,
    sourceDirectoryDescription: String
): SourceDirectorySet {
    val extension = extensions.findByName(name)
    return if (extension != null) {
        (extension as SourceDirectorySet)
    } else {
        val sourceDirectorySet = objectFactory.sourceDirectorySet(sourceDirectoryName, sourceDirectoryDescription)
        extensions.add(name, sourceDirectorySet)
        sourceDirectorySet
    }
}

fun TaskContainer.ifAdded(taskName: String, configure: Task.() -> Unit): Boolean {
    var added = false

    val task = this.findByName(taskName)
    if (task != null) {
        configure(task)
        added = true
    }

    this.whenTaskAdded {
        if (this.name == taskName) {
            configure(this)
            added = true
        }
    }

    return added
}
