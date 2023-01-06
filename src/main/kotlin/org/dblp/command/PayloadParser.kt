package org.dblp.command

import space.jetbrains.api.runtime.helpers.commandArguments
import space.jetbrains.api.runtime.types.MessagePayload
import space.jetbrains.api.runtime.types.UnfurlAttachment
import space.jetbrains.api.runtime.types.UnfurlDetailsIssue

fun payloadParser(payload: MessagePayload, maximumNumberOfArguments: Int = 3): WatchArguments? {

    val argumentString = payload.commandArguments() ?: return null
    val argumentList = argumentString.trimEnd().split(" ")

    /**
     * [MessagePayload.commandArguments] has already handled the case of empty string,
     * so `argumentList` will always have at least one element.
     */
    if (argumentList.size !in 1..maximumNumberOfArguments) {
        return null
    }

    val commandAction = argumentList[0]

    if (commandAction == WatchCommandAction.CHECK.name.lowercase()) {
        if (argumentList.size != WatchCommandAction.CHECK.numberOfArguments) return null
        return WatchCheckArguments()
    }

    /** From this point, a valid issue link must exist. **/

    /**
     * The following chunk verifies the issue's link existence and validity
     * by checking the message's attachments. If the provided link does exist
     * and is valid, one and only one [UnfurlAttachment] with the detail of
     * [UnfurlDetailsIssue] will be included in the payload.
     */
    val attachments = payload.message.attachments
    if (attachments.isNullOrEmpty() || attachments.size > 1) {
        return null
    }
    when (val attachment = attachments[0]) {

        is UnfurlAttachment -> {

            if (attachment.unfurl.details !is UnfurlDetailsIssue) {
                return null
            }

        }

        else -> {

            return null

        }

    }

    if (commandAction == WatchCommandAction.REGISTER.name.lowercase()) {
        if (argumentList.size != WatchCommandAction.REGISTER.numberOfArguments) return null
        return WatchRegisterArguments(issueLink = argumentList[1], time = argumentList[2])
    }

    if (commandAction == WatchCommandAction.DELETE.name.lowercase()) {
        if (argumentList.size != WatchCommandAction.DELETE.numberOfArguments) return null
        return WatchDeleteArguments(issueKey = argumentList[1])
    }

    if (commandAction == WatchCommandAction.UPDATE.name.lowercase()) {
        return null
    }

    return null

}