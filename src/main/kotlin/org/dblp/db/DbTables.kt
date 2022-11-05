package org.dblp.db

import org.jetbrains.exposed.sql.Table

object AppInstallation : Table("app_installation") {
    val clientId = varchar("client_id", 36).index(isUnique = true)
    val clientSecret = varchar("client_secret", 64)
    val serverUrl = varchar("server_url", 256)

    override val primaryKey = PrimaryKey(clientId)
}

object IssueRegistry : Table("issue_registry") {
    val issueId = varchar("issue_id",36).index(isUnique = true)
    val clientId = varchar("client_id", 36)
    val issuerId = varchar("issuer_id", 36)

    override val primaryKey = PrimaryKey(issueId)
}