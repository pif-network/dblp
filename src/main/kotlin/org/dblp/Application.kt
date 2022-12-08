package org.dblp

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.doublereceive.*
import kotlinx.coroutines.*
import org.dblp.db.initDbConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@OptIn(DelicateCoroutinesApi::class)
fun main() {

    GlobalScope.launch {

        cronDailyIssueStatusCheck()

    }

    embeddedServer(Netty, port = 8080, watchPaths = listOf("classes"), module = Application::module).start(wait = true)

}

fun Application.module() {

    install(DoubleReceive)

    var retries = 5

    while (retries >= 0) {

        val host = System.getenv("myhost").toString()
        val user = System.getenv("myuser").toString()
        val password = System.getenv("mypassword").toString()
        val database = System.getenv("mydatabase").toString()

        try {

            initDbConnection(host, user, password, database)
            break

        } catch (e: Exception) {

            log.error("Failed to connect to database. Retrying in 5 seconds.")
            runBlocking {
                delay(5000)
            }

            retries--

        }

    }

    configureRouting()

}

val config: Config by lazy { ConfigFactory.load() }

val log: Logger = LoggerFactory.getLogger("ApplicationKt")