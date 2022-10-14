package io.gitlab.arturbosch.detekt.rules

import java.net.URL

private val SEQUENTIAL_WHITESPACE_PATTERN = Regex("\\s\\s+")

fun String.collapseWhitespace() =
    trim().replace(SEQUENTIAL_WHITESPACE_PATTERN, " ")

fun String.lastArgumentMatchesUrl(): Boolean {
    val lastArgument = trimEnd().split(Regex("\\s+")).last()
    return runCatching {
        URL(lastArgument).toURI()
    }.isSuccess
}
