package nl.knaw.huygens.tag.tagml

/*-
 * #%L
 * tagml
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import nl.knaw.huygens.tag.tagml.ANTLRUtils.printTAGMLTokens
import nl.knaw.huygens.tag.tagml.TAGML.BRANCH
import nl.knaw.huygens.tag.tagml.TAGML.BRANCHES
import nl.knaw.huygens.tag.tagml.TAGML.CLOSE_TAG_ENDCHAR
import nl.knaw.huygens.tag.tagml.TAGML.CLOSE_TAG_STARTCHAR
import nl.knaw.huygens.tag.tagml.TAGML.OPEN_TAG_ENDCHAR
import nl.knaw.huygens.tag.tagml.TAGML.OPEN_TAG_STARTCHAR
import nl.knaw.huygens.tag.tagml.TAGML.escapeDoubleQuotedText
import nl.knaw.huygens.tag.tagml.TAGML.escapeRegularText
import nl.knaw.huygens.tag.tagml.TAGML.escapeSingleQuotedText
import nl.knaw.huygens.tag.tagml.TAGML.escapeVariantText
import nl.knaw.huygens.tag.tagml.TAGML.unEscape
import nl.knaw.huygens.tag.tagml.grammar.TAGMLLexer
import nl.knaw.huygens.tag.tagml.grammar.TAGMLParser
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.antlr.v4.runtime.tree.ParseTree
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class TAGMLTest {

    val log: Logger = LoggerFactory.getLogger(TAGMLTest::class.java)

    @Nested
    inner class TAGMLParserTests {

        @Test
        fun test() {
            val tagml = "[!{}!][test|+A,+B>[a|A>Jonn [b|B>loves<a] Oreos<b]<test]"
            assertParseSucceeds(tagml)
        }

        @Test
        fun header() {
            val tagml = """
            [!{
                "name": "test",
                "id": "test001",
                ":ontology": {
                },
                "author": [ "me", "you" ]
            }!]
            [l>[w>Just<w] [w>some<w] [w>words<w]<l]
            """.trimIndent()
            assertParseSucceeds(tagml)
        }

        @Test
        fun whitespace_in_text() {
            val tagml = "[!{}!][l>[w>Just<w] [w>some<w] [w>words<w]<l] \n \n"
            assertParseSucceeds(tagml)
        }

        @Test
        fun schema_header_is_required() {
            val tagmlBad = "[tagml>Hello World!<tagml]\n"
            assertParseFails(tagmlBad)

            val tagmlGood = """
            [!{}!]
            [tagml>Hello World!<tagml]
            """.trimIndent()
            assertParseSucceeds(tagmlGood)
        }

        @Test
        fun testIncorrectTAGML() {
            val tagml = "<xml>This is not TAGML!</xml>"
            assertParseFails(tagml)
        }
    }

    @Nested
    inner class EscapeTests {
        @Test
        fun testEscapeRegularText() {
            val text = """Escape these characters: \ < [, but not these: | " ' """
            val expectation = """Escape these characters: \\ \< \[, but not these: | " ' """
            val escaped = escapeRegularText(text)
            assertThat(escaped).isEqualTo(expectation)
        }

        @Test
        fun testEscapeVariantText() {
            val text = """Escape these characters : \ < [|, but not these: " ' """
            val expectation = """Escape these characters : \\ \< \[\|, but not these: " ' """
            val escaped = escapeVariantText(text)
            assertThat(escaped).isEqualTo(expectation)
        }

        @Test
        fun testEscapeSingleQuotedText() {
            val text = """Escape these characters: \ ', but not these: < [ | " """
            val expectation = """Escape these characters: \\ \', but not these: < [ | " """
            val escaped = escapeSingleQuotedText(text)
            assertThat(escaped).isEqualTo(expectation)
        }

        @Test
        fun testEscapeDoubleQuotedText() {
            val text = """Escape these characters: \ ", but not these: < [ | ' """
            val expectation = """Escape these characters: \\ \", but not these: < [ | ' """
            val escaped = escapeDoubleQuotedText(text)
            assertThat(escaped).isEqualTo(expectation)
        }

        @Test
        fun testUnEscape() {
            val text = """Unescape this: \< \[ \| \! \" \' \\ """
            val expectation = """Unescape this: < [ | ! " ' \ """
            val unEscaped = unEscape(text)
            assertThat(unEscaped).isEqualTo(expectation)
        }
    }

    @Test
    fun call_constants() {
        assertThat(OPEN_TAG_STARTCHAR).isEqualTo("[")
        assertThat(OPEN_TAG_ENDCHAR).isEqualTo(">")
        assertThat(CLOSE_TAG_STARTCHAR).isEqualTo("<")
        assertThat(CLOSE_TAG_ENDCHAR).isEqualTo("]")
        assertThat(BRANCHES).isEqualTo(":branches")
        assertThat(BRANCH).isEqualTo(":branch")
    }

    companion object {
        private fun assertParseSucceeds(tagml: String) {
            val result = parse(tagml)
            assert(result is Either.Right)
        }

        private fun assertParseFails(tagml: String) {
            val result = parse(tagml)
            assert(result is Either.Left)
        }

        private fun parse(tagml: String): Either<List<String>, ParseTree> {
            printTAGMLTokens(tagml)
            val antlrInputStream: CharStream = CharStreams.fromString(tagml)
            val errorListener = TestErrorListener()
            val lexer = TAGMLLexer(antlrInputStream).apply {
                addErrorListener(errorListener)
            }
            val tokens = CommonTokenStream(lexer)
            val parser = TAGMLParser(tokens).apply {
                addErrorListener(errorListener)
                buildParseTree = true
            }
            val parseTree: ParseTree = parser.document()
            return if (errorListener.hasErrors) {
                Left(errorListener.errors)
            } else {
                Right(parseTree)
            }
        }
    }
}

class TestErrorListener : ANTLRErrorListener {
    private val log: Logger = LoggerFactory.getLogger(TestErrorListener::class.java)

    internal val errors: MutableList<String> = ArrayList()
    val hasErrors: Boolean
        get() = errors.isNotEmpty()

    private val reportAmbiguity = false
    private val reportAttemptingFullContext = false
    private val reportContextSensitivity = true

    override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
        errors.add(String.format("syntax error: line %d:%d %s", line, charPositionInLine, msg.replace("token recognition error at", "unexpected token")))
    }

    override fun reportAmbiguity(recognizer: Parser, dfa: DFA, startIndex: Int, stopIndex: Int, exact: Boolean, ambigAlts: BitSet, configs: ATNConfigSet) {
        if (reportAmbiguity) {
            errors += ("ambiguity:\n recognizer=" + recognizer //
                    + ",\n dfa=" + dfa //
                    + ",\n startIndex=" + startIndex //
                    + ",\n stopIndex=" + stopIndex //
                    + ",\n exact=" + exact //
                    + ",\n ambigAlts=" + ambigAlts //
                    + ",\n configs=" + configs)
        }
    }

    override fun reportAttemptingFullContext(recognizer: Parser, dfa: DFA, startIndex: Int, stopIndex: Int, conflictingAlts: BitSet, configs: ATNConfigSet) {
        if (reportAttemptingFullContext) {
            errors += ("attempting full context error:\n recognizer=" + recognizer //
                    + ",\n dfa=" + dfa //
                    + ",\n startIndex=" + startIndex //
                    + ",\n stopIndex=" + stopIndex //
                    + ",\n conflictingAlts=" + conflictingAlts //
                    + ",\n configs=" + configs)
        }
    }

    override fun reportContextSensitivity(recognizer: Parser, dfa: DFA, startIndex: Int, stopIndex: Int, prediction: Int, configs: ATNConfigSet) {
        if (reportContextSensitivity) {
            errors += ("context sensitivity error:\n recognizer=" + recognizer //
                    + ",\n dfa=" + dfa //
                    + ",\n startIndex=" + startIndex //
                    + ",\n stopIndex=" + stopIndex //
                    + ",\n prediction=" + prediction //
                    + ",\n configs=" + configs)
        }
    }

    fun addError(messageTemplate: String, vararg messageArgs: Any) {
        errors += String.format(messageTemplate, *messageArgs)
    }
}