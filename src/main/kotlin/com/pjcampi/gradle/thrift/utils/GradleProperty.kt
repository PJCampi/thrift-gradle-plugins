package com.pjcampi.gradle.thrift.utils

import org.gradle.api.Project
import kotlin.reflect.KProperty

class GradleProperty<T, V>(
    project: Project,
    type: Class<V>,
    default: V? = null
) {

    private val property = project.objects.property(type).apply {
        if (default != null) convention(default)
    }

    operator fun getValue(thisRef: T, property: KProperty<*>): V =
        this.property.get()

    operator fun setValue(thisRef: T, property: KProperty<*>, value: V) =
        this.property.set(value)
}