package org.dblp.command

import org.dblp.db.IssueRegistry
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.helpers.message
import space.jetbrains.api.runtime.resources.projects
import space.jetbrains.api.runtime.types.*
import java.time.LocalDate
import kotlin.reflect.KSuspendFunction1

suspend fun runWatchCommand(
    payload: MessagePayload,
    sendMessage: KSuspendFunction1<ChatMessage, Unit>,
    spaceClient: SpaceClient
) {

    val arguments = payloadParser(payload) ?: run {
        sendMessage(helpMessageError())
        return
    }

    when (arguments) {

        is WatchCheckArguments -> {

            try {

                checkRegisteredIssueStatus(
                    sendMessage = sendMessage,
                    clientId = payload.clientId,
                    issuerId = payload.userId,
                )
                return

            } catch (e: Exception) {

                sendMessage(message {
                    section {
                        text(
                            ":exclamation: Unable to check for registers issues. Please try again later.",
                            MessageStyle.ERROR
                        )
                    }
                })
                return

            }

        }

        is WatchRegisterArguments -> {

            val projectKey = arguments.parsedIssueLinkPathSegments.projectKey
            val issueNumber = arguments.parsedIssueLinkPathSegments.issueNumber.toInt()
            val time = arguments.time.toLong()

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
                            it[expectedDateToBeResolved] = LocalDate.now().plusDays(time)
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
                content = "Successfully registered to watch $issueDefaultName for $watchTime ${if (watchTime > 1) "days" else "day"}."
            )
        }
    }
}

private suspend fun checkRegisteredIssueStatus(
    clientId: String,
    issuerId: String,
    sendMessage: KSuspendFunction1<ChatMessage, Unit>
) {

    val unresolvedIssues = transaction {
        IssueRegistry
            .select { (IssueRegistry.clientId.eq(clientId)) and (IssueRegistry.issuerId.eq(issuerId)) }
            .toList()
    }

    if (unresolvedIssues.isEmpty()) {
        sendMessage(ChatMessage.Text("No issues to check."))
        return
    }

    val responseMap = unresolvedIssues.map {

            issue ->

        WatchCheckResponseProperties(
            issueKey = issue[IssueRegistry.issueKey],
            expectedDateToBeResolved = issue[IssueRegistry.expectedDateToBeResolved]
        )

    }

    sendMessage(
        message {
            section {
                text(
                    size = MessageTextSize.LARGE,
                    content = "The following issues are registered and expected to be resolved soon.*"
                )
                fields {
                    field("**Issue**", "**Day(s) left**")
                    responseMap.forEach {
                        field(it.issueKey, it.daysLeft)
                    }
                }
                text(
                    "*[*] Unresolved outdated issues will be removed from the watch list now.*",
                    size = MessageTextSize.SMALL
                )
            }
        }
    )

}