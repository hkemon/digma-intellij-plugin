package org.digma.intellij.plugin.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.common.Laf
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.SwingConstants

fun createNotSupportedPanel(project: Project): JPanel {

    val mainPanel = JPanel(GridBagLayout())
    mainPanel.isOpaque = false
    mainPanel.border = JBUI.Borders.empty()

    val constraints = GridBagConstraints()
    constraints.gridx = 1
    constraints.gridy = 1
    constraints.ipady = 20
    val icon = JLabel(org.digma.intellij.plugin.icons.AppIcons.LOGO)
    icon.horizontalAlignment = SwingConstants.CENTER
    mainPanel.add(icon,constraints)


    val textPane = JTextPane()
    textPane.apply {
        contentType = "text/html"
        isEditable = false
        isOpaque = false
        background = null
        border = null
        putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true)
//        font = BaseCopyableLabel.DEFAULT_FONT
        text = getNoConnectionMessageHtml()
        toolTipText = ""
        isFocusCycleRoot = false
        isFocusTraversalPolicyProvider = false
    }
    constraints.gridy = 2
    mainPanel.add(textPane,constraints)


    val slackPanel = createSlackLinkPanel(project)
    constraints.gridy = 3
    mainPanel.add(slackPanel,constraints)

    return mainPanel
}

fun getNoConnectionMessageHtml(): String {

    val title = "Non supported IDE version"
    val paragraph = "This plugin requires a newer version of IntelliJ.<br>" +
            "Please update to the latest version of IntelliJ."

    return "<html>" +
            "<head>" +
            "<style>" +
            "h3 {text-align: center;}" +
            "p {text-align: center;}" +
            "div {text-align: center;}" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<h3>$title</h3>" +
            "<p>$paragraph</p>" +
            "</body>" +
            "</html>"
}


fun createSlackLinkPanel(project: Project): JPanel {

    val slackLinkPanel = JPanel(BorderLayout(10,5))
    slackLinkPanel.isOpaque = false
    slackLinkPanel.border = JBUI.Borders.empty()

    slackLinkPanel.add(JLabel(Laf.Icons.Environment.SLACK), BorderLayout.WEST)
    val slackLink = ActionLink("Join Our Slack Channel for Support"){
        BrowserUtil.browse(DIGMA_SLACK_SUPPORT_CHANNEL, project)
    }
    slackLink.toolTipText = "Join Our Slack Channel for Support"
    slackLinkPanel.add(slackLink, BorderLayout.CENTER)
    return slackLinkPanel
}


const val DIGMA_SLACK_SUPPORT_CHANNEL = "https://join.slack.com/t/continuous-feedback/shared_invite/zt-1hk5rbjow-yXOIxyyYOLSXpCZ4RXstgA"
const val DIGMA_DOCKER_APP_URL = "https://open.docker.com/extensions/marketplace?extensionId=digmaai/digma-docker-extension"