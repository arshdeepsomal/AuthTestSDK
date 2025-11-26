package com.devconsole.auth_sdk.utils


/**
 * use this annotation for classes that we will not have coverage for
 * @param reason short description of why this is used
 * example: to be covered in another ticker or eng backlog
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExcludeFromCoverage(val reason: String)