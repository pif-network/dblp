package org.dblp

import kotlinx.coroutines.delay
import org.dblp.db.AppInstallation
import org.dblp.db.IssueRegistry
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import space.jetbrains.api.runtime.SpaceAppInstance
import space.jetbrains.api.runtime.helpers.message
import space.jetbrains.api.runtime.types.ApiIcon
import space.jetbrains.api.runtime.types.ChatMessage
import space.jetbrains.api.runtime.types.MessageOutline
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Proceeds a daily check for registered issues at 9am.
 *
 * TODO: Inspect performance difference between this implementation and using an actual library.
 * @link: https://stackoverflow.com/questions/59516599/how-to-run-cron-jobs-in-kotlin-ktor
 * @link: https://github.com/justwrote/kjob
 */
suspend fun cronDailyIssueStatusCheck() {

    val now = ZonedDateTime.now().plusSeconds(1)

    /** No matter the time of installation, the first check will always be at 9:00 next day. **/
    val tomorrowMorningAtNine = now
        .plusDays(1)
        .withHour(9)
        .minusMinutes(now.minute.toLong())

    delay(tomorrowMorningAtNine.toInstant().toEpochMilli() - now.toInstant().toEpochMilli())

    checkRegisteredIssueStatus()

    val hasBeenInitialised = true

    while (hasBeenInitialised) {

        /** Daily check at 9:00. **/
        delay(24 * 60 * 60 * 1000)
        checkRegisteredIssueStatus()

    }

}

/**
 * Checks the status of all registered issues and notifies issuers if their issues are overdue.
 */
suspend fun checkRegisteredIssueStatus() {

    val unresolvedIssues = transaction {
        IssueRegistry
            .select { IssueRegistry.expectedDateToBeResolved.eq(LocalDate.now()) }
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
            notifyUnresolvedIssueMessage(unresolvedIssue[IssueRegistry.issueKey])
        )

    }

}

private fun notifyUnresolvedIssueMessage(issueKey: String): ChatMessage {
    return message {
        outline(
            MessageOutline(
                icon = ApiIcon("checkbox-checked"),
                text = "Unresolved issue"
            )
        )
        section {
            text("Issue $issueKey is still unresolved")
        }
    }
}