package org.dblp

import org.dblp.command.runHelpCommand
import org.dblp.command.supportedCommands
import space.jetbrains.api.ExperimentalSpaceSdkApi
import space.jetbrains.api.runtime.helpers.ProcessingScope
import space.jetbrains.api.runtime.helpers.command
import space.jetbrains.api.runtime.types.ChatMessage
import space.jetbrains.api.runtime.types.MessagePayload

@OptIn(ExperimentalSpaceSdkApi::class)
suspend fun ProcessingScope.processCommand(payload: MessagePayload) {
    val client = clientWithClientCredentials()
    suspend fun sendMessage(message: ChatMessage) {
        client.sendMessage(payload.userId, message)
    }

    val commandName = payload.command()
    val command = supportedCommands.find { it.name == commandName }
    if (command == null) {
        runHelpCommand(::sendMessage)
    } else {
        command.run(payload, ::sendMessage, client)
    }
}