@file:OptIn(ExperimentalSpaceSdkApi::class)

package org.dblp

import org.dblp.db.IssueRegistry
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import space.jetbrains.api.ExperimentalSpaceSdkApi
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.helpers.ProcessingScope
import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.types.*

@OptIn(ExperimentalSpaceSdkApi::class)
suspend fun ProcessingScope.processWebhookEvent(payload: WebhookRequestPayload) {

    val client = clientWithClientCredentials()

    when (val event = payload.payload) {

        is IssueWebhookEvent -> {

            val theIssue = transaction {
                IssueRegistry.select { IssueRegistry.issueId.eq(event.issue.id) }.firstOrNull()
            } ?: return

            /**
             * The status name does not exist in the payload,
             * so if the status changes, the issue has been resolved.
             *
             * Trust me.
             */
            if (event.status?.new?.id != event.status?.old?.id) {

                val watchersUserIds = getWatchersUserId(event.issue.id, payload.clientId)

                watchersUserIds.forEach {

                        watchersUserId ->

                    client.sendMessage(
                        watchersUserId,
                        ChatMessage.Text(
                            "Issue \"${theIssue[IssueRegistry.issueTitle]}\" has been resolved. Removing it from watch list."
                        )
                    )

                }

                transaction {
                    IssueRegistry.deleteWhere { IssueRegistry.issueId.eq(event.issue.id) }
                }

            }

        }

        is PingWebhookEvent -> {}

        else -> {
            error("Unexpected event type.")
        }

    }

}

suspend fun SpaceClient.sendMessage(userId: String, message: ChatMessage) {
    chats.messages.sendMessage(
        channel = ChannelIdentifier.Profile(ProfileIdentifier.Id(userId)),
        content = message,
    )
}
