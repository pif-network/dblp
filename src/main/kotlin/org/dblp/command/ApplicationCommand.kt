package org.dblp.command

import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.types.ChatMessage
import space.jetbrains.api.runtime.types.CommandDetail
import space.jetbrains.api.runtime.types.MessagePayload
import kotlin.reflect.KSuspendFunction1

/**
 * A command that the application can execute.
 */
class ApplicationCommand(
    val name: String,
    val info: String,
    val run: suspend (payload: MessagePayload, sendMessage: KSuspendFunction1<ChatMessage, Unit>, spaceClient: SpaceClient) -> Unit
) {
    /**
     * [CommandDetail] is returned to Space with an information about the command. List of commands
     * is shown to the user.
     */
    fun toSpaceCommand() = CommandDetail(name, info)
}

