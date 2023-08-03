package org.digma.intellij.plugin.model.rest.event

import java.time.ZonedDateTime

data class LatestCodeObjectEventsRequest(val environments: List<String>,val fromDate: ZonedDateTime)
