package org.dblp

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
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

            // read request body
            val body = call.receiveText()

            // verify if the request comes from a trusted Space instance
            val signature = call.request.header("X-Space-Public-Key-Signature")
            val timestamp = call.request.header("X-Space-Timestamp")?.toLongOrNull()

            // verifyWithPublicKey gets a key from Space, uses it to generate message hash
            // and compares the generated hash to the hash in a message
            if (signature.isNullOrBlank() || timestamp == null ||

                !spaceClient.verifyWithPublicKey(body, timestamp, signature)

            ) {

                call.respond(HttpStatusCode.Unauthorized)
                return@post

            }

//            when (val payload = readPayload(body)) {
//
//
//            }

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
                        LoggerFactory.getLogger("Space").info("Webhook initialised")
                    }

                    is WebhookRequestPayload -> {
                        // process webhook asynchronously, respond to Space immediately
                        launch {
                            processWebhookEvent(payload)
                        }
                        ktorRequestAdapter.respond(200, "")
                        LoggerFactory.getLogger("Space").info("Webhook request processed")
                    }

                    is ListCommandsPayload -> {

                        // Space requests the list of supported commands
                        call.respondText(
                            ObjectMapper().writeValueAsString(getSupportedCommands()),
                            ContentType.Application.Json
                        )

                    }

                    is MessagePayload -> {

                        // user sent a message to the application
                        val commandName = payload.command()
                        val command = supportedCommands.find { it.name == commandName }
                        if (command == null) {
                            runHelpCommand(payload)
                        } else {
                            launch { command.run(payload) }
                        }
                        call.respond(HttpStatusCode.OK, "")

                    }

                }

                SpaceHttpResponse.RespondWithOk

            }

        }

    }

}
