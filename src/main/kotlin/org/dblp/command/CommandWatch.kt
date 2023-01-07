package org.dblp.command

import org.dblp.db.IssueRegistry
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.resources.projects
import space.jetbrains.api.runtime.types.ChatMessage
import space.jetbrains.api.runtime.types.MessagePayload
import space.jetbrains.api.runtime.types.ProjectIdentifier
import java.time.LocalDate
import kotlin.reflect.KSuspendFunction1

suspend fun runWatchCommand(
    payload: MessagePayload,
    sendMessage: KSuspendFunction1<ChatMessage, Unit>,
    spaceClient: SpaceClient
) {

    val arguments = payloadParser(payload) ?: run {
        sendMessage(messageErrorHelp())
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

                sendMessage(messageErrorWatchCheck())
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

                    sendMessage(messageErrorWatchRegisterAResolvedIssue(theIssue.channel.contact.defaultName))
                    return

                }

                val registeredIssueOrNull = transaction {
                    IssueRegistry.select {
                        (IssueRegistry.clientId.eq(payload.clientId)) and
                                (IssueRegistry.issuerId.eq(payload.userId))
                    }.firstOrNull()
                }

                if (registeredIssueOrNull != null) {

                    sendMessage(messageErrorWatchRegisterReRegisteringIssue(theIssue.channel.contact.defaultName))
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

                sendMessage(messageAcceptWatchRegister(theIssue.channel.contact.defaultName, time))

            } catch (e: Exception) {

                sendMessage(ChatMessage.Text("Error: ${e.message}"))

            }

        }

        is WatchUpdateArguments -> {

            val issueKey = arguments.issueKey
            val time = arguments.newTime.toLong()

            val registeredIssueOrNull = transaction {
                IssueRegistry.select {
                    (IssueRegistry.clientId.eq(payload.clientId)) and
                            (IssueRegistry.issuerId.eq(payload.userId)) and
                            (IssueRegistry.issueKey.eq(issueKey))
                }.firstOrNull()
            }

            if (registeredIssueOrNull == null) {

                sendMessage(messageErrorWatchUpdateNotRegisteredIssue(issueKey))
                return

            }

            transaction {
                with(IssueRegistry) {
                    replace {
                        it[expectedDateToBeResolved] = LocalDate.now().plusDays(time)
                    }
                }
            }

            sendMessage(messageResponseWatchUpdate(issueKey))

        }

        is WatchDeleteArguments -> {

            try {

                transaction {
                    with(IssueRegistry) {
                        deleteWhere {
                            (clientId.eq(payload.clientId)) and
                                    (issuerId.eq(payload.userId)) and
                                    (issueKey.eq(arguments.issueKey))
                        }
                    }
                }

                sendMessage(messageResponseWatchDelete(arguments.issueKey))

            } catch (e: Exception) {

                sendMessage(messageErrorWatchDelete(arguments.issueKey))

            }

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

    sendMessage(messageResponseWatchCheck(responseMap))

}