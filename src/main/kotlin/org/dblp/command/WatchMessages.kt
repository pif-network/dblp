package org.dblp.command

import space.jetbrains.api.runtime.helpers.message
import space.jetbrains.api.runtime.types.*

fun messageAcceptWatchRegister(issueDefaultName: String, watchTime: Long): ChatMessage {
    return message {
        outline(
            MessageOutline(
                icon = ApiIcon("checkbox-checked"),
                text = "Watch registration accepted"
            )
        )
        section {
            text(
                size = MessageTextSize.REGULAR,
                content = "Successfully registered to watch $issueDefaultName for $watchTime ${if (watchTime > 1) "days" else "day"}."
            )
        }
    }
}

fun messageResponseWatchCheck(responseList: List<WatchCheckResponseProperties>): ChatMessage {
    return message {
        section {
            text(
                size = MessageTextSize.LARGE,
                content = "The following issues are registered and expected to be resolved soon.*"
            )
            fields {
                field("**Issue**", "**Day(s) left**")
                responseList.forEach {
                    field(it.issueKey, it.daysLeft)
                }
            }
            text(
                "*[*] Unresolved outdated issues will be removed from the watch list now.*",
                size = MessageTextSize.SMALL
            )
        }
    }

}

fun messageErrorWatchCheck(): ChatMessage {
    return message {
        section {
            text(
                ":exclamation: Unable to check for registers issues. Please try again later.",
                MessageStyle.ERROR
            )
        }
    }
}

fun messageErrorWatchRegisterAResolvedIssue(defaultName: String): ChatMessage {
    return ChatMessage.Text("The issue $defaultName is already closed. No need to watch it.")
}

fun messageErrorWatchRegisterReRegisteringIssue(defaultName: String): ChatMessage {
    return ChatMessage.Text(":bangbang: Error: The issue $defaultName has been registered before.")
}