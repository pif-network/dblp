package org.dblp

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import org.dblp.command.getSupportedCommands
import space.jetbrains.api.ExperimentalSpaceSdkApi
import space.jetbrains.api.runtime.Space
import space.jetbrains.api.runtime.helpers.*
import space.jetbrains.api.runtime.types.InitPayload
import space.jetbrains.api.runtime.types.ListCommandsPayload
import space.jetbrains.api.runtime.types.MessagePayload
import space.jetbrains.api.runtime.types.WebhookRequestPayload

@OptIn(ExperimentalSpaceSdkApi::class)
fun Application.configureRouting() {

    routing {

        get("/") {
            call.respondText("Hello from dblp, again.")
        }

        post("api/space") {

            val ktorRequestAdapter = object : RequestAdapter {

                override suspend fun receiveText() =
                    call.receiveText()

                override fun getHeader(headerName: String) =
                    call.request.header(headerName)

                override suspend fun respond(httpStatusCode: Int, body: String) =
                    call.respond(HttpStatusCode.fromValue(httpStatusCode), body)

            }

            Space.processPayload(ktorRequestAdapter, spaceHttpClient, AppInstanceStorage) {

                    payload ->
                when (payload) {

                    is InitPayload -> {
                        setupWebhooks()
                        requestPermissions()
                    }

                    is WebhookRequestPayload -> {
                        /** Process webhook asynchronously, respond to Space immediately **/
                        launch {
                            processWebhookEvent(payload)
                        }
                    }

                    is ListCommandsPayload -> {
                        /** Space requests the list of supported commands **/
                        call.respondText(
                            ObjectMapper().writeValueAsString(getSupportedCommands()),
                            ContentType.Application.Json
                        )

                    }

                    is MessagePayload -> {
                        /** Process webhook asynchronously, respond to Space immediately **/
                        launch {
                            processCommand(payload)
                        }
                    }

                }

                SpaceHttpResponse.RespondWithOk

            }

        }

    }

}
