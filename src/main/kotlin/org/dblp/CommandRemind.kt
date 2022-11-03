package org.dblp

import kotlinx.coroutines.delay
import space.jetbrains.api.runtime.helpers.commandArguments
import space.jetbrains.api.runtime.helpers.message
import space.jetbrains.api.runtime.types.*
import kotlin.reflect.KSuspendFunction1

suspend fun runRemindCommand(payload:MessagePayload?,sendMessage: KSuspendFunction1<ChatMessage, Unit>) {
    val remindMeArgs = getArgs(payload) ?: run {
        sendMessage(helpMessage())
        return
    }

    remindAfterDelay(remindMeArgs, sendMessage)
}

private suspend fun remindAfterDelay(
    remindMeArgs: RemindMeArgs,
    sendMessage: KSuspendFunction1<ChatMessage, Unit>
) {
    sendMessage(acceptRemindMessage(remindMeArgs))

    delay(remindMeArgs.delayMs)
    sendMessage(remindMessage(remindMeArgs))
}

private fun acceptRemindMessage(remindMeArgs: RemindMeArgs): ChatMessage {
    return message {
        outline(
            MessageOutline(
                icon = ApiIcon("checkbox-checked"),
                text = "Reminder accepted"
            )
        )
        section {
            text("I will remind you in ${remindMeArgs.delayMs / 1000} seconds about \"${remindMeArgs.reminderText}\"")
        }
    }
}

private fun remindMessage(remindMeArgs: RemindMeArgs): ChatMessage {
    return message {
        outline(
            MessageOutline(
                icon = ApiIcon("clock"),
                text = "Reminder"
            )
        )
        section {
            text(remindMeArgs.reminderText)
            text(
                size = MessageTextSize.SMALL,
                content = "${remindMeArgs.delayMs / 1000} seconds have passed"
            )
        }
    }
}

private fun getArgs(payload: MessagePayload?): RemindMeArgs? {
    val args = payload?.commandArguments() ?: return null
    val delayMs = args.substringBefore(" ").toLongOrNull()?.times(1000) ?: return null
    val reminderText = args.substringAfter(" ").trimStart().takeIf { it.isNotEmpty() } ?: return null
    return RemindMeArgs(delayMs, reminderText)
}

private class RemindMeArgs(
    val delayMs: Long,
    val reminderText: String,
)
