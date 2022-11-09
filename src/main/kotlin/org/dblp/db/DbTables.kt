package org.dblp.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate

object AppInstallation : Table("app_installation") {
    val clientId = varchar("client_id", 36).index(isUnique = true)
    val clientSecret = varchar("client_secret", 64)
    val serverUrl = varchar("server_url", 256)

    override val primaryKey = PrimaryKey(clientId)
}

object IssueRegistry : Table("issue_registry") {
    val issueId = varchar("issue_id", 36).index(isUnique = true)

    val issuerId = varchar("issuer_id", 36)

    val issueNumber = integer("issue_number")
    val issueTitle = varchar("issue_title", 256)
    val issueDefaultName = varchar("issue_default_name", 256)
    val issueStatus = varchar("issue_status", 36)

    val projectKey = varchar("project_key", 36)

    val expectedDaysToBeResolved = date("expected_days")
    val iat = date("issued_at").default(LocalDate.now())

    val clientId = varchar("client_id", 36)

    override val primaryKey = PrimaryKey(issueId)
}