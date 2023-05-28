package org.digma.intellij.plugin.idea.build


import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtScript

class GradleProjModifier(val project: Project) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): GradleProjModifier {
            return project.getService(GradleProjModifier::class.java)
        }
    }

    fun addDependency(configuration: String, group: String, name: String, version: String) {
        val buildFile = findBuildFile(project)
        if (buildFile != null) {
            val psiManager = PsiManager.getInstance(project)
            val psiFile = psiManager.findFile(buildFile)

            if (psiFile != null) {
                ApplicationManager.getApplication().invokeLater {
                    WriteCommandAction.runWriteCommandAction(project) {
                        val psiElementFactory = PsiElementFactory.getInstance(project)

                        val dependencyExpression = psiElementFactory.createExpressionFromText(
                            "\"$group:$name:$version\"",
                            null
                        )

//                        val dependencyStatement = psiElementFactory.createStatementFromText(
//                            "$configuration '$group:$name:$version'",
//                            null
//                        )

                        val dependenciesBlock = findDependenciesBlock(psiFile)
                        if (dependenciesBlock != null) {
//                            dependenciesBlock.addBefore(dependencyStatement, dependenciesBlock.lastChild)
                            dependenciesBlock.add(dependencyExpression)
                        }
                        PsiDocumentManager.getInstance(project).commitDocument(psiFile.viewProvider.document)
                    }
                }
            }
        }
    }

    private fun findBuildFile(project: Project): VirtualFile? {
        // Replace "build.gradle" with the appropriate name of the build file (e.g., "build.gradle.kts")
        var vf = project.baseDir?.findChild("build.gradle")
        if (vf == null) {
            vf = project.baseDir?.findChild("build.gradle.kts")
        }
        return vf
    }

    private fun findDependenciesBlock(psiFile: PsiFile): PsiElement? {
        val language = psiFile.language
        if (language.isKindOf("kotlin")) {
            // handle kotlin
            return findDependenciesBlockForKotlin(psiFile)
        } else {
            // handle groovy
            return psiFile.children.find { it.text == "dependencies" }
        }
    }

    private fun findDependenciesBlockForKotlin(psiFile: PsiFile): PsiElement? {
        val ktFile = psiFile as? KtFile
        if (ktFile != null) {
            val script = ktFile.script
            if (script != null) {
                val dependenciesBlock = script.findDependenciesBlock()
                if (dependenciesBlock != null) {
                    return dependenciesBlock as? PsiElement
                }
            }
        }
        return null
    }

    private fun KtScript.findDependenciesBlock(): KtBlockExpression? {
        for (declaration in declarations) {
            val dependenciesBlock = declaration.findDependenciesBlock()
            if (dependenciesBlock != null) {
                return dependenciesBlock
            }
        }
        return null
    }

    private fun KtDeclaration.findDependenciesBlock(): KtBlockExpression? {
        when (this) {
            is KtProperty -> {
                val initializer = initializer
                if (initializer is KtBlockExpression && name == "dependencies") {
                    return initializer
                }
            }

            is KtNamedFunction -> {
                val body = bodyExpression
                if (body is KtBlockExpression && name == "dependencies") {
                    return body
                }
            }
        }
        return null
    }
}