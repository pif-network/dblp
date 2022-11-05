package org.dblp

import org.dblp.db.AppInstallation
import org.dblp.db.IssueRegistry
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import space.jetbrains.api.runtime.SpaceAppInstance
import space.jetbrains.api.runtime.SpaceAuth
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.ktorClientForSpace

//val spaceAppInstance = SpaceAppInstance(
//    /**
//     * Copy client id and client secret from the `Authentication` tab
//     * on the application page for your app in Space.
//     *
//     * Client Secret is a sensitive value. Normally you would pass it to the application
//     * as an environment variable.
//     */
//    clientId = config.getString("space.clientId"),
//    clientSecret = config.getString("space.clientSecret"),
//    /**
//     * URL of your Space instance
//     */
//    spaceServerUrl = config.getString("space.serverUrl"),
//)


/**
 * Space Client used to call API methods in Space.
 * Note the usage of [SpaceAuth.ClientCredentials] for authorization: the application will
 * authorize in Space based on clientId+clientSecret and will act on behalf of itself (not
 * on behalf of a Space user).
 */
//val spaceClient =
//    SpaceClient(ktorClient = spaceHttpClient, appInstance = spaceAppInstance, auth = SpaceAuth.ClientCredentials())
val spaceHttpClient = ktorClientForSpace()

/**
 * Call API method in Space to send a message to the user.
 */
//suspend fun sendMessage(userId: String, message: ChatMessage) {
//    spaceClient.chats.messages.sendMessage(
//        channel = ChannelIdentifier.Profile(ProfileIdentifier.Id(userId)),
//        content = message
//    )
//}

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

fun getWatcherUserId(issueId: String): String? {
    return transaction {
        IssueRegistry.select { IssueRegistry.issueId.eq(issueId) }
            .map { it[IssueRegistry.issuerId] }
            .firstOrNull()
    }
}
