package org.dblp.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate

object AppInstallation : Table("app_installation") {
    val clientId = varchar("client_id", 36).index(isUnique = true)
    val clientSecret = varchar("client_secret", 64)
    val serverUrl = varchar("server_url", 256)

    override val primaryKey = PrimaryKey(clientId)
}

object IssueRegistry : UUIDTable("issue_registry", "uuid") {
    val issuerId = varchar("issuer_id", 36)
    val clientId = varchar("client_id", 36)

    val issueId = varchar("issue_id", 36)
    val issueKey= varchar("issue_key", 36)
    val issueTitle = varchar("issue_title", 256)

    val projectKey = varchar("project_key", 36)

    @Suppress("unused")
    val iat = date("issued_at").clientDefault { LocalDate.now() }
    val expectedDateToBeResolved = date("expected_resolve_date")
}