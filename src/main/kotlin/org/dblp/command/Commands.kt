package org.dblp.command

import space.jetbrains.api.runtime.types.Commands
import space.jetbrains.api.runtime.types.ListCommandsPayload

val supportedCommands = listOf(

    ApplicationCommand(
        "help",
        "Show this help",
    ) { _, sendMessage, _ -> runHelpCommand(sendMessage = sendMessage) },

    ApplicationCommand(
        "remind",
        "Remind me about something in N seconds, e.g., to remind about \"the thing\" in 10 seconds, send 'remind 10 the thing' ",
    ) { payload, sendMessage, _ -> runRemindCommand(payload, sendMessage) },

    ApplicationCommand(
        "watch",
        "Params:\n" +
                "* `issue_link`: use the link from the `Copy to clipboard` button on the issue page.\n" +
                "* `time`: watching time, in day. Watch list will be checked daily for unresolved out-of-time issue(s)."
    ) { payload, sendMessage, spaceClient ->
        runWatchCommand(
            payload = payload,
            sendMessage = sendMessage,
            spaceClient = spaceClient
        )
    },

    )

/**
 * Response to the [ListCommandsPayload]. Space will display the returned commands as commands supported
 * by your application.
 */
fun getSupportedCommands() = Commands(
    supportedCommands.map {
        it.toSpaceCommand()
    }
)
