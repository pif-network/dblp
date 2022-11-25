package org.dblp.command

import org.dblp.checkRegisteredIssueStatus
import org.dblp.db.IssueRegistry
import org.dblp.log
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.transactions.transaction
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.helpers.commandArguments
import space.jetbrains.api.runtime.helpers.message
import space.jetbrains.api.runtime.resources.projects
import space.jetbrains.api.runtime.types.*
import java.net.URI
import java.time.LocalDate
import kotlin.reflect.KSuspendFunction1

suspend fun runWatchCommand(
    payload: MessagePayload,
    sendMessage: KSuspendFunction1<ChatMessage, Unit>,
    spaceClient: SpaceClient
) {

    val watchArgs = getArgs(payload) ?: run {
        sendMessage(helpMessageError())
        return
    }

    /** Checking registered issues' status. **/
    if (watchArgs.issue == "check" && watchArgs.time == null) {
        try {
            checkRegisteredIssueStatus()
            return
        } catch (e: Exception) {
            log.error(e.stackTraceToString())
            return
        }
    }

    /**
     * The following chunk verifies the issue's link existence and validity
     * by checking the message's attachments. If the provided link does exist
     * and is valid, an [UnfurlAttachment] will be included in the payload.
     */
    val attachments = payload.message.attachments
    if (attachments == null) {
        sendMessage(helpMessageError())
        return
    }

    val issueUrlMap: Map<String, String>

    try {
        issueUrlMap = extractProjectKeyAndIssueIdFromUrl(watchArgs.issue)
    } catch (e: IndexOutOfBoundsException) {
        sendMessage(helpMessageError())
        return
    }

    /** Since the issue's link is valid, the following properties will never be null. **/
    val projectKey = issueUrlMap["projectKey"]!!
    val issueNumber = issueUrlMap["issueNumber"]?.toInt()!!

    try {

        val theIssue = spaceClient.projects.planning.issues.getIssueByNumber(
            project = ProjectIdentifier.Key(projectKey),
            number = issueNumber
        ) {
            id()
            status {
                name()
            }
            title()
            /** @see [SPACE-17854](https://youtrack.jetbrains.com/issue/SPACE-17854/Space-SDK-Cannot-access-fields-that-share-permission) **/
//            channel {
//                contact {
//                    defaultName()
//                }
//            }
        }

        if (theIssue.status.name == "Done") {

            sendMessage(ChatMessage.Text("The issue is already closed. No need to watch it."))
            return

        }

        transaction {
            with(IssueRegistry) {
                replace {
                    it[issueId] = theIssue.id
                    it[issuerId] = payload.userId
                    it[this.issueNumber] = issueNumber
                    it[issueStatus] = theIssue.status.name
                    it[issueTitle] = theIssue.title
                    it[issueLink] = watchArgs.issue
                    it[expectedDaysToBeResolved] = LocalDate.now().plusDays(watchArgs.time!!)
                    it[this.projectKey] = projectKey
                    it[clientId] = payload.clientId
                }
            }
        }

        sendMessage(acceptWatchMessage(theIssue.title, projectKey))

    } catch (e: Exception) {

        sendMessage(ChatMessage.Text("Error: ${e.message}"))

    }

}

private fun acceptWatchMessage(issueTitle: String, projectKey: String): ChatMessage {
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
                content = "Successfully registered issue \"$issueTitle\" in project \"$projectKey\""
            )
        }
    }
}

private fun extractProjectKeyAndIssueIdFromUrl(url: String): Map<String, String> {
    val uri = URI(url)
    val segments = uri.path.split('/')
    // https://jetbrains.space/p/DBLP/issue/1
    return mapOf("projectKey" to segments[2], "issueNumber" to segments[4])
}


private fun getArgs(payload: MessagePayload): WatchArgs? {
    val args = payload.commandArguments() ?: return null
    val issue = args.substringBefore(" ")
    val time = args.substringAfter(" ").trimStart().takeIf { it.isNotEmpty() }?.toLongOrNull()
    return WatchArgs(issue, time)
}

private class WatchArgs(
    val issue: String,
    val time: Long?,
)
