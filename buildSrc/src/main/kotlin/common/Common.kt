package common

import org.gradle.api.Project

fun properties(key: String,project: Project) = project.findProperty(key).toString()



fun isWindows() = org.gradle.internal.os.OperatingSystem.current().isWindows

//todo: maybe implement semantic version with this https://github.com/swiftzer/semver
//fun semanticVersion():String{
//
//}