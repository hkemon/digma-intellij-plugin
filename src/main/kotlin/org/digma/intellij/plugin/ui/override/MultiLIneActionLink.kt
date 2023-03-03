package org.digma.intellij.plugin.ui.override

import com.intellij.ui.components.ActionLink
import java.awt.Dimension
import java.awt.event.ActionEvent

class MultiLIneActionLink(errorText: String, perform: (ActionEvent) -> Unit) : ActionLink(errorText,perform){

    override fun getPreferredSize(): Dimension {

        calculatePreferredSize()

        return super.getPreferredSize()
    }
}