package org.dblp

import space.jetbrains.api.runtime.helpers.message
import space.jetbrains.api.runtime.types.*
import kotlin.reflect.KSuspendFunction1

suspend fun runHelpCommand(sendMessage: KSuspendFunction1<ChatMessage, Unit>) {
    sendMessage(helpMessage())
}

fun helpMessage(): ChatMessage {
    return message {
        MessageOutline(
            icon = ApiIcon("checkbox-checked"),
            text = "Issue watcher - help"
        )
        section {
            text("List of available commands", MessageStyle.PRIMARY)
            fields {
                supportedCommands.forEach {
                    field(it.name, it.info)
                }
            }
        }
    }
}

fun helpMessageError(): ChatMessage {
    return message {
        MessageOutline(
            icon = ApiIcon("checkbox-checked"),
            text = "Sorry, I don't understand you."
        )
        section {
            text("Error", MessageStyle.ERROR)
            fields {
                supportedCommands.forEach {
                    field(it.name, it.info)
                }
            }
        }
    }
}
