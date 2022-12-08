package org.dblp.command

import org.dblp.createSpaceClientFromAppInstance
import org.dblp.db.AppInstallation
import org.dblp.db.IssueRegistry
import org.dblp.log
import org.dblp.sendMessage
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import space.jetbrains.api.runtime.SpaceAppInstance
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

            sendMessage(ChatMessage.Text("Checking registered issues' status..."))
            checkRegisteredIssueStatus(
                sendMessage = sendMessage,
                clientId = payload.clientId
            )
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
            channel {
                contact {
                    defaultName()
                }
            }
        }

        if (theIssue.status.name == "Done") {

                    sendMessage(ChatMessage.Text("The issue ${theIssue.channel.contact.defaultName} is already closed. No need to watch it."))
                    return

        }

        transaction {
            with(IssueRegistry) {
                replace {
                    it[issuerId] = payload.userId
                    it[clientId] = payload.clientId
                    it[issueId] = theIssue.id
                    it[issueKey] = theIssue.channel.contact.defaultName
                    it[issueTitle] = theIssue.title
                    it[this.projectKey] = projectKey
                    it[expectedDateToBeResolved] = LocalDate.now().plusDays(watchArgs.time!!)
                }
            }
        }

                sendMessage(acceptWatchMessage(theIssue.channel.contact.defaultName, time))

    } catch (e: Exception) {

        sendMessage(ChatMessage.Text("Error: ${e.message}"))

    }

}

    }

}

private fun acceptWatchMessage(issueDefaultName: String, watchTime: Long): ChatMessage {
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
                content = "Successfully registered to watch $issueDefaultName for $watchTime days."
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
    val args = payload.commandArguments()?.trimEnd() ?: return null
    val issue = args.substringBefore(" ")
    val time = args.substringAfter(" ").trimStart().takeIf { it.isNotEmpty() }?.toLongOrNull()
    return WatchArgs(issue, time)
}

private class WatchArgs(
    val issue: String,
    val time: Long?,
)

private suspend fun checkRegisteredIssueStatus(
    clientId: String,
    sendMessage: KSuspendFunction1<ChatMessage, Unit>
) {

    val unresolvedIssues = transaction {
        IssueRegistry
            .select { IssueRegistry.clientId.eq(clientId) }
            .toList()
    }

    if (unresolvedIssues.isEmpty()) {
        sendMessage(ChatMessage.Text("No issues to check."))
        return
    }

    unresolvedIssues.forEach {

            unresolvedIssue ->

        val spaceInstance = transaction {
            AppInstallation
                .select { AppInstallation.clientId.eq(unresolvedIssue[IssueRegistry.clientId]) }
                .map {
                    SpaceAppInstance(
                        it[AppInstallation.clientId],
                        it[AppInstallation.clientSecret],
                        it[AppInstallation.serverUrl],
                    )
                }
                .first()
        }

        val spaceClient = createSpaceClientFromAppInstance(spaceInstance)

        spaceClient.sendMessage(
            unresolvedIssue[IssueRegistry.issuerId],
            ChatMessage.Text(
                "The issue \"${unresolvedIssue[IssueRegistry.issueTitle]}\" " +
                        "in project \"${unresolvedIssue[IssueRegistry.projectKey]}\" " +
                        "has not been resolved yet."
            )
        )

    }

}
