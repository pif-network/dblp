@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package org.dblp

import space.jetbrains.api.ExperimentalSpaceSdkApi
import space.jetbrains.api.runtime.helpers.ProcessingScope
import space.jetbrains.api.runtime.resources.applications
import space.jetbrains.api.runtime.types.ApplicationIdentifier
import space.jetbrains.api.runtime.types.CustomGenericSubscriptionIn
import space.jetbrains.api.runtime.types.GlobalPermissionContextIdentifier

@ExperimentalSpaceSdkApi
suspend fun ProcessingScope.setupWebhooks() {
    val spaceClient = clientWithClientCredentials()
    val webhook = spaceClient.applications.webhooks.createWebhook(
        application = ApplicationIdentifier.Me,
        name = "Issue",
        description = "Track all issues events",
        acceptedHttpResponseCodes = listOf(200),
    )

    spaceClient.applications.webhooks.subscriptions.createSubscription(
        application = ApplicationIdentifier.Me,
        webhookId = webhook.id,
        name = "Issue created",
        subscription = CustomGenericSubscriptionIn(
            subjectCode = "Created",
            filters = emptyList(),
            eventTypeCodes = listOf("Issue.Created"),
        )
    )

    spaceClient.applications.webhooks.subscriptions.createSubscription(
        application = ApplicationIdentifier.Me,
        webhookId = webhook.id,
        name = "Issue updated",
        subscription = CustomGenericSubscriptionIn(
            subjectCode = "Updated",
            filters = emptyList(),
            eventTypeCodes = listOf("Issue.Updated"),
        )
    )
}

@OptIn(ExperimentalSpaceSdkApi::class)
suspend fun ProcessingScope.requestPermissions() {
    val spaceClient = clientWithClientCredentials()
    spaceClient.applications.authorizations.authorizedRights.requestRights(
        application = ApplicationIdentifier.Me,
        contextIdentifier = GlobalPermissionContextIdentifier,
        listOf("Project.Issues.View", "Team.View")
    )
}
