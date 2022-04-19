package com.azharkova.di.inject

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqName

val container = FqName("com.azharkova.di.inject.Container")
val single = FqName("com.azharkova.di.inject.Single")
val testContainer = FqName("com.azharkova.kmmdi.shared.config.ConfigurationApp")

fun ClassDescriptor.isContainer(): Boolean =
    annotations.hasAnnotation(container)

fun ClassDescriptor.isSingle(): Boolean =
    annotations.hasAnnotation(single)