package org.digma.intellij.plugin.ui.override

import java.awt.Dimension
import javax.swing.Icon
import javax.swing.JLabel

class MultiLineHtmlLabel : JLabel {

    constructor(text: String) : super(text)
    constructor(text: String, horizontalAlignment: Int) : super(text,horizontalAlignment)
    constructor(text: String, icon: Icon, horizontalAlignment: Int) : super(text,icon,horizontalAlignment)

    override fun getPreferredSize(): Dimension {

        calculatePreferredSize()

        return super.getPreferredSize()
    }

}