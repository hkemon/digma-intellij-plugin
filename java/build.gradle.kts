import common.platformVersion

plugins {
    id("plugin-library")
    id("common-kotlin")
}


dependencies{
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))

    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.7.22")
}

intellij {
    version.set("IC-"+ platformVersion(project))
    plugins.set(listOf("com.intellij.java","org.jetbrains.idea.maven","org.jetbrains.plugins.gradle"))
}
