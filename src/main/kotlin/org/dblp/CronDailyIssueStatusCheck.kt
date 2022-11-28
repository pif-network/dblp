package org.dblp

import org.dblp.db.AppInstallation
import org.dblp.db.IssueRegistry
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import space.jetbrains.api.runtime.SpaceAppInstance
import space.jetbrains.api.runtime.helpers.message
import space.jetbrains.api.runtime.types.ApiIcon
import space.jetbrains.api.runtime.types.ChatMessage
import space.jetbrains.api.runtime.types.MessageOutline
import java.time.LocalDate

suspend fun cronCheckRegisteredIssueStatus() {

    val unresolvedIssues = transaction {
        IssueRegistry
            .select {
                (IssueRegistry.issueStatus.eq("Open")) and
                        (IssueRegistry.expectedDaysToBeResolved.eq(LocalDate.now()))
            }
            .toList()
    }

    if (unresolvedIssues.isEmpty()) {
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
                .first() // If there is an issue, there is an instance.
        }

        val spaceClient = createSpaceClientFromAppInstance(spaceInstance)

        spaceClient.sendMessage(
            unresolvedIssue[IssueRegistry.issuerId],
            notifyUnresolvedIssueMessage(unresolvedIssue[IssueRegistry.issueTitle])
        )

    }

}

private fun notifyUnresolvedIssueMessage(issueTitle: String): ChatMessage {
    return message {
        outline(
            MessageOutline(
                icon = ApiIcon("checkbox-checked"),
                text = "Unresolved issue"
            )
        )
        section {
            text("Issue \"$issueTitle\" is still unresolved")
        }
    }
}