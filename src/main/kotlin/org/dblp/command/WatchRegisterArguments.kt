package org.dblp.command

import java.net.URL

class WatchRegisterArguments(
    issueLink: String,
    val time: String,
) : WatchArguments {
    private val uri = URL(issueLink)
    private val issueLinkPathSegments = uri.path.split('/')

    val parsedIssueLinkPathSegments = object : ParsedIssueLinkPathSegments {
        /** https://jetbrains.space/p/DBLP/issue/1 **/
        override val projectKey = issueLinkPathSegments[2]
        override val issueNumber = issueLinkPathSegments[4]
    }
}