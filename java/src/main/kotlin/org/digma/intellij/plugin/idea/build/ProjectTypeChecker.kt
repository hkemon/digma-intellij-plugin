package org.digma.intellij.plugin.idea.build

import com.intellij.openapi.project.Project

class ProjectTypeChecker {

    companion object {

        fun determineBuildSystem(project: Project): JavaBuildSystem {
            if (isGradleBasedProject(project)) {
                return JavaBuildSystem.GRADLE
            }
            if (isMavenBasedProject(project)) {
                return JavaBuildSystem.MAVEN
            }
            if (isAntBasedProject(project)) {
                return JavaBuildSystem.ANT
            }
            return JavaBuildSystem.UNKNOWN
        }

        private fun isMavenBasedProject(project: Project): Boolean {
            val baseDir = project.baseDir
            val pomXmlFile = baseDir.findChild("pom.xml")
            return pomXmlFile != null
        }

        private fun isGradleBasedProject(project: Project): Boolean {
            val baseDir = project.baseDir
            var gradleBuildFile = baseDir.findChild("build.gradle")
            if (gradleBuildFile == null) {
                gradleBuildFile = baseDir.findChild("build.gradle.kts")
            }
            return gradleBuildFile != null
        }

        private fun isAntBasedProject(project: Project): Boolean {
            val baseDir = project.baseDir
            val buildXmlFile = baseDir.findChild("build.xml")
            return buildXmlFile != null
        }

    }

}
