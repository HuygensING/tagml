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
package nl.knaw.huc.di.tag.tagml

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import nl.knaw.huc.di.tag.ANTLRUtils.printTokens
import nl.knaw.huc.di.tag.tagml.TAGML.escapeDoubleQuotedText
import nl.knaw.huc.di.tag.tagml.TAGML.escapeRegularText
import nl.knaw.huc.di.tag.tagml.TAGML.escapeSingleQuotedText
import nl.knaw.huc.di.tag.tagml.TAGML.escapeVariantText
import nl.knaw.huc.di.tag.tagml.TAGML.unEscape
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.antlr.v4.runtime.tree.ParseTree
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class TAGMLTest {

    val LOG: Logger = LoggerFactory.getLogger(TAGMLTest::class.java)

    //    @Nested
//    inner class TestEscape {
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
//    }

    @Test
    fun testCorrectTAGML() {
        val tagml = "[!schema http://alexandria.net/schemas/xyz.yaml]\n[tagml>Hello World!<tagml]\n"
        assertParseSucceeds(tagml)
    }

    @Test
    fun testURLInSchemaLocationParses1() {
        val tagml = "[!schema file://localhost/tmp/schema.yaml]\n[tagml>Hello World!<tagml]\n"
        assertParseSucceeds(tagml)
    }

    @Test
    fun testURLInSchemaLocationParses2() {
        val tagml = "[!schema file:///tmp/schema.yaml]\n[tagml>Hello World!<tagml]\n"
        assertParseSucceeds(tagml)
    }

    @Test
    fun testMissingSchemaFails() {
        val tagml = "[tagml>Hello World!<tagml]\n"
        assertParseFails(tagml)
    }

    @Test
    fun testSchemaLocationAndNamespace() {
        val tagml = "[!schema file:///tmp/schema.yaml]\n[!ns a http://example.org/bla]\n[tagml>Hello World!<tagml]\n"
        assertParseSucceeds(tagml)
    }

//    @Test
//    fun testURLInSchemaLocationParses3() {
//        val tagml = "[!schema file://localhost/c\$/WINDOWS/Temp/schema.yaml]\n[tagml>Hello World!<tagml]\n"
//        assertParseSucceeds(tagml)
//    }

    @Test
    fun testURLInSchemaLocationParses4() {
        val tagml = "[!schema file:///c:/WINDOWS/Temp/hello%20world%20schema.yaml]\n[tagml>Hello World!<tagml]\n"
        assertParseSucceeds(tagml)
    }

    @Test
    fun testIncorrectTAGML() {
        val tagml = "<xml>This is not TAGML!</xml>"
        assertParseFails(tagml)
    }

    private fun assertParseSucceeds(tagml: String) {
        val result = parse(tagml)
        assertThat(result.isRight()).isTrue()
    }

    private fun assertParseFails(tagml: String) {
        val result = parse(tagml)
        assertThat(result.isLeft()).isTrue()
    }

    private fun parse(tagml: String): Either<List<String>, ParseTree> {
        printTokens(tagml)
        val antlrInputStream: CharStream = CharStreams.fromString(tagml)
        val lexer = TAGMLLexer(antlrInputStream)
        val errorListener = TestErrorListener()
        lexer.addErrorListener(errorListener)
        val tokens = CommonTokenStream(lexer)
        val parser = TAGMLParser(tokens)
        parser.addErrorListener(errorListener)
        parser.buildParseTree = true
        val parseTree: ParseTree = parser.document()
        return if (errorListener.hasErrors) {
            Left(errorListener.errors)
        } else {
            Right(parseTree)
        }
    }
}

class TestErrorListener : ANTLRErrorListener {
    private val LOG: Logger = LoggerFactory.getLogger(TestErrorListener::class.java)

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
            errors.add("ambiguity:\n recognizer=" + recognizer //
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
            errors.add("attempting full context error:\n recognizer=" + recognizer //
                    + ",\n dfa=" + dfa //
                    + ",\n startIndex=" + startIndex //
                    + ",\n stopIndex=" + stopIndex //
                    + ",\n conflictingAlts=" + conflictingAlts //
                    + ",\n configs=" + configs)
        }
    }

    override fun reportContextSensitivity(recognizer: Parser, dfa: DFA, startIndex: Int, stopIndex: Int, prediction: Int, configs: ATNConfigSet) {
        if (reportContextSensitivity) {
            errors.add("context sensitivity error:\n recognizer=" + recognizer //
                    + ",\n dfa=" + dfa //
                    + ",\n startIndex=" + startIndex //
                    + ",\n stopIndex=" + stopIndex //
                    + ",\n prediction=" + prediction //
                    + ",\n configs=" + configs)
        }
    }

    fun addError(messageTemplate: String, vararg messageArgs: Any) {
        errors.add(String.format(messageTemplate, *messageArgs))
    }

}
