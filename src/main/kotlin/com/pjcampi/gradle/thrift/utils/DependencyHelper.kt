package com.pjcampi.gradle.thrift.utils

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.create
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


class DependencyHelper(
    private val configurationName: String,
    private val dependencyHandler: DependencyHandler
) {

    operator fun invoke(dependencyNotation: Any): Dependency? =
        dependencyHandler.add(configurationName, dependencyNotation)

    operator fun invoke(
        dependencyNotation: String,
        dependencyConfiguration: ExternalModuleDependency.() -> Unit
    ): ExternalModuleDependency =
        dependencyHandler.add(configurationName, dependencyNotation, dependencyConfiguration)

    operator fun invoke(
        group: String,
        name: String,
        version: String? = null,
        configuration: String? = null,
        classifier: String? = null,
        ext: String? = null
    ): ExternalModuleDependency =
        dependencyHandler.run {
            create(group, name, version, configuration, classifier, ext)
                .also { add(configurationName, it) }
        }

    operator fun invoke(
        group: String,
        name: String,
        version: String? = null,
        configuration: String? = null,
        classifier: String? = null,
        ext: String? = null,
        dependencyConfiguration: ExternalModuleDependency.() -> Unit
    ): ExternalModuleDependency =
        dependencyHandler.run {
            val dep = create(group, name, version, configuration, classifier, ext)
            add(configurationName, dep, dependencyConfiguration)
        }

    operator fun <T : ModuleDependency> invoke(
        dependency: T,
        dependencyConfiguration: T.() -> Unit
    ): T =
        dependencyHandler.add(configurationName, dependency, dependencyConfiguration)

    companion object : ReadOnlyProperty<DependencyHandler, DependencyHelper> {
        override fun getValue(thisRef: DependencyHandler, property: KProperty<*>): DependencyHelper =
            DependencyHelper(property.name, thisRef)
    }
}