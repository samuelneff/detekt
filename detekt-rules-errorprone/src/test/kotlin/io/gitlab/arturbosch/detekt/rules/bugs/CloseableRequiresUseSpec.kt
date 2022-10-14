package io.gitlab.arturbosch.detekt.rules.bugs

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.rules.collapseWhitespace
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private const val FAIL_MARKER = "/**/"

@KotlinCoreEnvironmentTest
class CloseableRequiresUseSpec(private val env: KotlinCoreEnvironment) {
    private val subject = CloseableRequiresUse(Config.empty)

    private fun assertCompliant(@Language("kotlin") content: String) {
        assertThat(content).withFailMessage(
            "Unexpectedly found source marker ($FAIL_MARKER) in assertCompliant code.\n\n$content"
        )
        val findings = subject.compileAndLintWithContext(env, content.trimIndent())
        assertThat(findings).isEmpty()
    }

    private fun assertNonCompliant(@Language("kotlin") content: String) {
        val trimmed = content.trimIndent()
        val contentLines = trimmed.lines()
        val failLineIndex = contentLines.indexOfFirst { it.contains(FAIL_MARKER) }
        assertThat(failLineIndex).withFailMessage(
            """
                Non-compliant failure source location not provided and 
                no marker ($FAIL_MARKER) found.
            """.collapseWhitespace()
        ).isNotEqualTo(-1)
        val failSourceLine = contentLines[failLineIndex]
        val failColumnIndex = failSourceLine.indexOf(FAIL_MARKER)
        val updatedContent = contentLines.mapIndexed { index, line ->
            if (index == failLineIndex) {
                line.removeRange(failColumnIndex..failColumnIndex)
            } else {
                line
            }
        }.joinToString("\n")
        assertNonCompliant(updatedContent, failLineIndex + 1, failColumnIndex + 1)
    }

    private fun assertNonCompliant(@Language("kotlin") content: String, line: Int, column: Int) {
        val findings = subject.compileAndLintWithContext(env, content.trimIndent())
        assertThat(findings).hasSize(1)
        assertThat(findings).hasStartSourceLocation(line, column)
        assertThat(findings[0]).isExactlyInstanceOf(CloseableRequiresUse::class.java)
    }

    @Nested
    @Suppress("ClassName") // The inner classes fail naming check but match our Spec convention
    inner class `Constructing closeables` {

        @Test
        fun `Compliant - use directly included`() {
            assertCompliant(
                """
                    fun test() {
                        StringReader("This is a string")./**/use { reader ->
                            reader.forEachLine { line -> 
                                println(line) 
                            }
                        }
                    }
                """
            )
        }

        @Test
        fun `Non-compliant - another extension method called`() {
            assertNonCompliant(
                """
                    fun test() {
                        StringReader("This is a string")./**/forEachLine { line -> 
                            println(line) 
                        }
                    }
                """
            )
        }

        @Test
        fun `Compliant - only Closeable result stored in local variable`() {
            assertCompliant(
                """
                    fun test() {
                        val lines = StringReader("This is a string").use { reader ->
                            reader.readLines()
                        }
                    }
                """
            )
        }

        @Test
        fun `Non-compliant - Closeable stored in local variable`() {
            assertNonCompliant(
                """
                    fun test() {
                        val reader = StringReader("This is a string")
                        val lines = reader./**/readLines()
                    }
                """
            )
        }
    }

    @Nested
    @Suppress("ClassName") // The inner classes fail naming check but match our Spec convention
    inner class `Functions returning Closeable` {

        @Test
        fun `Compliant - directly uses use with Unit function`() {
            assertCompliant(
                """
                    fun iReturnCloseable() = StringReader("close me")
                    fun test() {
                        iReturnCloseable().use { reader ->
                            reader.forEachLine { line -> println() }
                        }
                    }            
                """
            )
        }

        @Test
        fun `Compliant - directly uses use with result stored`() {
            assertCompliant(
                """
                    fun iReturnCloseable() = StringReader("close me")
                    fun test() {
                        val lines = iReturnCloseable().use { reader ->
                            reader.readLines()
                        }
                    }            
                """
            )
        }

        @Test
        fun `Non-compliant - directly calls other extension function`() {
            assertNonCompliant(
                """
                    fun iReturnCloseable() = StringReader("close me")
                    fun test() {
                        iReturnCloseable()./**/forEachLine { line -> println() }
                    }            
                """
            )
        }

        @Test
        fun `Non-compliant - stores closeable`() {
            assertNonCompliant(
                """
                    fun iReturnCloseable() = StringReader("close me")
                    fun test() {
                        val reader = iReturnCloseable()
                        val lines = reader./**/readLines()
                    }            
                """
            )
        }

    }

    @Nested
    @Suppress("ClassName") // The inner classes fail naming check but match our Spec convention
    inner class `Functions that accept Closeable parameters` {

        @Test
        fun `Compliant - Unit function with single Closeable parameter`() {
            assertCompliant(
                """
                    fun debugCloseable(closeable: Closable) {
                        println(closeable.toString())
                    }
                """
            )
        }

        @Test
        fun `Compliant - Closeable returning function with single Closeable parameter`() {
            assertCompliant(
                """
                    fun tapCloseable(closeable: Closable): Closeable {
                        println(closeable.toString())
                        return closeable
                    }
                """
            )
        }
    }


    @Nested
    @Suppress("ClassName") // The inner classes fail naming check but match our Spec convention
    inner class `Closeable extension functions` {

        @Test
        fun `Compliant - Unit extension function`() {
            assertCompliant(
                """
                    fun Closeable.println() {
                        println(this.toString())
                    }
                """
            )
        }

        @Test
        fun `Compliant - Passthrough extension function`() {
            assertCompliant(
                """
                    fun Closeable.tap(): Closeable {
                        println(this.toString())
                        return this
                    }
                """
            )
        }
    }
}
