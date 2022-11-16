@file:OptIn(ExperimentalSpaceSdkApi::class)
@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package org.dblp

import org.dblp.db.IssueRegistry
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import space.jetbrains.api.ExperimentalSpaceSdkApi
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.helpers.ProcessingScope
import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.types.*

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalSpaceSdkApi::class)
suspend fun ProcessingScope.processWebhookEvent(payload: WebhookRequestPayload) {

    val client = clientWithClientCredentials()

    when (val event = payload.payload) {

        is IssueWebhookEvent -> {

            var theIssue: ResultRow? = null

            transaction {
                theIssue = IssueRegistry.select { IssueRegistry.issueId.eq(event.issue.id) }.firstOrNull()
            }

            if (theIssue == null) return

            if (event.status?.new?.id != event.status?.old?.id) {

                val watchersUserIds = getWatchersUserId(event.issue.id, payload.clientId)

                watchersUserIds.forEach {

                        watchersUserId ->

                    client.sendMessage(
                        watchersUserId,
                        ChatMessage.Text(
                            "Issue \"${theIssue!![IssueRegistry.issueTitle]}\" has been resolved. Removing it from watch list."
                        )
                    )

                }

                transaction {
                    IssueRegistry.deleteWhere { IssueRegistry.issueId.eq(event.issue.id) }
                }

            }

        }

//        is TeamMembershipEvent -> {
//            val membership =
//                client.teamDirectory.memberships.getMembership(TeamMembershipIdentifier.Id(event.membership.id)) {
//                    member {
//                        id()
//                        name()
//                    }
//                    team {
//                        name()
//                    }
//                }
//
//            val memberId = membership.member.id
//            when (membership.team.name) {
//                "MyTeam" -> client.sendMessage(memberId, 'hi')
//            }
//        }

        is PingWebhookEvent -> {}

        else -> {
            error("Unexpected event type")
        }
    }

}

suspend fun SpaceClient.sendMessage(userId: String, message: ChatMessage) {
    chats.messages.sendMessage(
        channel = ChannelIdentifier.Profile(ProfileIdentifier.Id(userId)),
        content = message,
    )
}
