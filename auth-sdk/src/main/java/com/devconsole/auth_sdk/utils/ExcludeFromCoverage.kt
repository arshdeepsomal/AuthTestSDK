package com.devconsole.auth_sdk.utils


@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExcludeFromCoverage(val reason: String)