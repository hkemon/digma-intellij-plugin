import common.platformVersion

plugins {
    id("plugin-library")
    id("common-kotlin")
}

intellij {
    pluginName.set("system-test-plugin")
    version.set(platformVersion())
    type.set("IC")
    plugins.set(listOf("com.intellij.java","org.jetbrains.idea.maven","org.jetbrains.plugins.gradle"))

    pluginsRepositories {
        marketplace()
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation(project(":ide-common"))
    implementation(project(":java"))
    implementation(project(":model"))
    implementation(project(":analytics-provider"))
    implementation(project(":"))
    testImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")
    testImplementation("com.squareup.retrofit2:retrofit-mock:2.9.0")
    testImplementation("org.mockito:mockito-core:5.4.0")
}

tasks.test {
    useJUnit()
}
