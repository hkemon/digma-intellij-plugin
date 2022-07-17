import common.properties

plugins {
    id("plugin-library")
}

dependencies {
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))
}

intellij {
    version.set("GO-" + properties("platformVersion", project))
    plugins.set(listOf("org.jetbrains.plugins.go"))
}
