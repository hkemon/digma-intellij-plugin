package org.digma.intellij.plugin.test.system

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class OpenProjectTest: LightJavaCodeInsightFixtureTestCase() {

    override fun getTestDataPath(): String {
        return "src/test/resoueces/"
    }
}