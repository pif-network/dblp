package org.dblp

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dblp.db.AppInstallation
import org.dblp.db.IssueRegistry
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import space.jetbrains.api.runtime.SpaceAppInstance
import space.jetbrains.api.runtime.helpers.message
import space.jetbrains.api.runtime.types.ApiIcon
import space.jetbrains.api.runtime.types.ChatMessage
import space.jetbrains.api.runtime.types.MessageOutline
import java.time.LocalDate

fun checkRegisteredIssueStatus() = runBlocking {

    val unresolvedIssues = IssueRegistry.select {
        IssueRegistry.issueStatus.eq("Open")
    }.andWhere { IssueRegistry.expectedDaysToBeResolved.eq(LocalDate.now()) }

    unresolvedIssues.forEach {

            unresolvedIssue ->

        transaction {

            val spaceInstances =
                AppInstallation.select { AppInstallation.clientId.eq(unresolvedIssue[IssueRegistry.clientId]) }
                    .map {
                        SpaceAppInstance(
                            it[AppInstallation.clientId],
                            it[AppInstallation.clientSecret],
                            it[AppInstallation.serverUrl],
                        )
                    }

            spaceInstances.forEach {

                    spaceInstance ->
                val spaceClient = createSpaceClientFromAppInstance(spaceInstance)

                launch {
                    spaceClient.sendMessage(
                        unresolvedIssue[IssueRegistry.issuerId],
                        notifyUnresolvedIssueMessage(unresolvedIssue[IssueRegistry.issueTitle])
                    )
                }

            }

        }

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