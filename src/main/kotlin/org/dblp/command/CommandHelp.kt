package org.dblp.command

import space.jetbrains.api.runtime.helpers.message
import space.jetbrains.api.runtime.types.ChatMessage
import space.jetbrains.api.runtime.types.MessageStyle
import space.jetbrains.api.runtime.types.MessageTextSize
import kotlin.reflect.KSuspendFunction1

suspend fun runHelpCommand(sendMessage: KSuspendFunction1<ChatMessage, Unit>) {
    sendMessage(helpMessage())
}

fun helpMessage(): ChatMessage {
    return message {
        section {
            text("**List of available commands**", MessageStyle.PRIMARY, size = MessageTextSize.LARGE)
            fields {
                supportedCommands.forEach {
                    field("*${it.name}*", it.info)
                }
            }
            text(
                "E.g., send `watch https://<your-org>.jetbrains.space/p/<project_key>/issues/<issue_number> 7` to watch that issue for a week.",
                MessageStyle.PRIMARY,
                size = MessageTextSize.SMALL
            )
        }
    }
}

fun helpMessageError(): ChatMessage {
    return message {
        section {
            text("**:exclamation: Unable to process command**", MessageStyle.ERROR, MessageTextSize.LARGE)
            text("\n")
            text("**List of available commands**", MessageStyle.PRIMARY)
            fields {
                supportedCommands.forEach {
                    field("*${it.name}*", it.info)
                }
            }
        }
    }
}
