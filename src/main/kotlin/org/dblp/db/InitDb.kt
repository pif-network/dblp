package org.dblp.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun initDbConnection(host: String, user: String, password: String, database: String) {

    Database.connect(
        "jdbc:mysql://$host/$database?sslMode=VERIFY_IDENTITY", driver = "com.mysql.cj.jdbc.Driver",
        user = user, password = password
    )

    transaction {
        SchemaUtils.createMissingTablesAndColumns(AppInstallation, IssueRegistry)
    }

}
