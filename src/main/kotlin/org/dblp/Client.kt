package org.dblp

import org.dblp.db.AppInstallation
import org.dblp.db.IssueRegistry
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import space.jetbrains.api.runtime.SpaceAppInstance
import space.jetbrains.api.runtime.SpaceAuth
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.ktorClientForSpace

val spaceHttpClient = ktorClientForSpace()

fun createSpaceClientFromAppInstance(appInstance: SpaceAppInstance): SpaceClient {
    val spaceHttpClient = ktorClientForSpace()
    return SpaceClient(ktorClient = spaceHttpClient, appInstance = appInstance, auth = SpaceAuth.ClientCredentials())
}

fun getAppInstanceFromClientId(clientId: String): SpaceAppInstance? {
    return transaction {
        AppInstallation.select { AppInstallation.clientId.eq(clientId) }
            .map {
                SpaceAppInstance(
                    it[AppInstallation.clientId],
                    it[AppInstallation.clientSecret],
                    it[AppInstallation.serverUrl],
                )
            }
            .firstOrNull()
    }
}

fun getWatchersUserId(issueId: String, clientId: String): List<String> {
    return transaction {
        IssueRegistry
            .select { IssueRegistry.issueId.eq(issueId) }
            .andWhere { IssueRegistry.clientId.eq(clientId) }
            .map { it[IssueRegistry.issuerId] }
    }
}
