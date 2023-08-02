package org.digma.intellij.plugin.test.system

import com.fasterxml.jackson.jr.ob.JSON
import com.google.gson.Gson
import com.intellij.json.psi.JsonArray
import com.intellij.openapi.diagnostic.Logger
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.digma.intellij.plugin.log.Log


class DispatcherMock: Dispatcher() {

    companion object {
        val logger: Logger = Logger.getInstance(DispatcherMock::class.java)
    }
    override fun dispatch(request: RecordedRequest): MockResponse {
        return when (val path = request.path) {
            "/CodeAnalytics/environments" -> {
                Log.log(logger::info, "DispatcherMock: /CodeAnalytics/environments")
                val envList = listOf("env1", "env2")
                val gson = Gson()
                val json = gson.toJson(envList)
                MockResponse().setResponseCode(200).setBody(json)
            }

            else -> {
                Log.log(logger::info, "DispatcherMock: $path")
                val gson = Gson()
                val json = gson.toJson("ok")
                MockResponse().setResponseCode(200).setBody(json)
            }
        }
    }
}