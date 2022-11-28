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
import java.time.ZonedDateTime

@OptIn(DelicateCoroutinesApi::class)
suspend fun main() {

    embeddedServer(Netty, port = 8080, watchPaths = listOf("classes")) {

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

    }.start(wait = true)

    /**
     * This cron job implementation follows the following link.
     * @link: https://stackoverflow.com/questions/59516599/how-to-run-cron-jobs-in-kotlin-ktor
     *
     * TODO: Inspect performance difference between this implementation and using an actual library.
     * @link: https://github.com/justwrote/kjob
     * */
    GlobalScope.launch {

        val now = ZonedDateTime.now().plusSeconds(1)

        /** No matter the time of installation, the first check will always be at 9:00 next day. **/
        val tomorrowMorningAtNine = now
            .plusDays(1)
            .withHour(9)
            .minusMinutes(now.minute.toLong())

        delay(tomorrowMorningAtNine.toInstant().toEpochMilli() - now.toInstant().toEpochMilli())

        cronCheckRegisteredIssueStatus()

        val hasBeenInitialised = true

        while (hasBeenInitialised) {

            /** Daily check at 9:00. **/
            delay(24 * 60 * 60 * 1000)
            cronCheckRegisteredIssueStatus()

        }

    }

}

val config: Config by lazy { ConfigFactory.load() }

val log: Logger = LoggerFactory.getLogger("ApplicationKt")