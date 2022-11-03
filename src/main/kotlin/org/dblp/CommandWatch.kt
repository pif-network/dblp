package org.dblp

import space.jetbrains.api.runtime.helpers.commandArguments
import space.jetbrains.api.runtime.types.ChatMessage
import space.jetbrains.api.runtime.types.MessagePayload
import kotlin.reflect.KSuspendFunction1

suspend fun runWatchCommand(payload: MessagePayload, sendMessage: KSuspendFunction1<ChatMessage, Unit>) {
}

private fun getArgs(payload: MessagePayload): WatchArgs? {
    val args = payload.commandArguments() ?: return null
    val issue = args.substringBefore(" ")
    val time =
        args.substringAfter(" ").trimStart().takeIf { it.isNotEmpty() }?.toLongOrNull()?.times(1000) ?: return null
    return WatchArgs(issue, time)
}

private class WatchArgs(
    val issue: String,
    val time: Long,
)
