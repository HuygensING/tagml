package nl.knaw.huygens.tag.tagorl

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
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Left
import arrow.core.Right
import nl.knaw.huygens.tag.tagml.ANTLRUtils.printTAGORLTokens
import nl.knaw.huygens.tag.tagml.TestErrorListener
import nl.knaw.huygens.tag.tagorl.TAGORLParser.OneOrMoreChildContext
import nl.knaw.huygens.tag.tagorl.TAGORLParser.OptionalChildContext
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.fail

class TAGORLTest {

    val log: Logger = LoggerFactory.getLogger(TAGORLTest::class.java)

    @Nested
    inner class HierarchyRuleTest {

        @Test
        fun hierarchy_rule_1() {
            val rule = "excerpt > chapterTitle+, img?"
            val expectedParent = "excerpt"
            val expectedChildren1 = "chapterTitle"
            val expectedChildren = "img"
            parse(rule).fold(
                    { errors -> fail("$errors") },
                    { ctx ->
                        {
                            when (ctx) {
                                is TAGORLParser.HierarchyRuleContext -> {
                                    assertThat(ctx.Name().text).isEqualTo(expectedParent)

                                    val children = ctx.children() as TAGORLParser.ChildrenListContext
                                    val child0 = children.child(0) as OneOrMoreChildContext
                                    assertThat(child0.Name().text).isEqualTo(expectedChildren1)

                                    val child1 = children.child(1) as OptionalChildContext
                                    assertThat(child1.Name().text).isEqualTo(expectedChildren)
                                }
                                else -> fail("expected HierarchyRuleContext")
                            }
                        }
                    }
            )
        }
    }

    @Nested
    inner class SetRuleTest {
        @Test
        fun set_rule_with_1_parameter_fails() {
            val rule = "test(parameter)"
            assertParsingFailsWithErrors(rule, listOf("syntax error: line 1:14 mismatched input ')' expecting ','"))
        }

        @Test
        fun set_rule_with_2_parameters() {
            val rule = "nonlinear(sic,corr)"
            assertParsesAsSetRule(rule, "nonlinear", listOf("sic", "corr"))
        }

        @Test
        fun set_rule_with_3_parameters() {
            val rule = "siblings(huey,dewey,louie)"
            assertParsesAsSetRule(rule, "siblings", listOf("huey", "dewey", "louie"))
        }

        @Test
        fun failing_set_rule() {
            val rule = "test(parameter"
            assertParsingFailsWithErrors(rule, listOf("syntax error: line 1:14 mismatched input '<EOF>' expecting ','"))
        }
    }

    @Nested
    inner class TripleRuleTest {
        @Test
        fun triple_rule() {
            val rule = "author writes title"
            assertParsesAsTripleRule(rule, "author", "writes", listOf("title"))
        }

        @Test
        fun triple_rule_with_multiple_objects() {
            val rule = "cook prepares breakfast,lunch,dinner"
            assertParsesAsTripleRule(rule, "cook", "prepares", listOf("breakfast", "lunch", "dinner"))
        }
    }

    companion object {
        private fun parse(rule: String): Either<List<String>, ParseTree> {
            printTAGORLTokens(rule)
            val antlrInputStream: CharStream = CharStreams.fromString(rule)
            val errorListener = TestErrorListener()
            val lexer = TAGORLLexer(antlrInputStream).apply {
                addErrorListener(errorListener)
            }
            val tokens = CommonTokenStream(lexer)
            val parser = TAGORLParser(tokens).apply {
                addErrorListener(errorListener)
                buildParseTree = true
            }
            val ontologyRule = parser.ontologyRule() // this starts the parsing
            return if (errorListener.hasErrors) {
                Left(errorListener.errors)
            } else {
                Right(ontologyRule.getChild(0))
            }
        }

        private fun assertParseFails(tagml: String) {
            val result = parse(tagml)
            assert(result is Left)
        }

        private fun assertParseSucceeds(rule: String) {
            val result = parse(rule)
            assert(result is Right)
        }

        private fun assertParsesAsTripleRule(
                rule: String,
                expectedSubject: String,
                expectedPredicate: String,
                expectedObjects: List<String>
        ): Unit =
                parse(rule).fold(
                        { errors -> fail("$errors") },
                        { ctx ->
                            when (ctx) {
                                is TAGORLParser.TripleRuleContext -> {
                                    assertThat(ctx.subject().text).isEqualTo(expectedSubject)
                                    assertThat(ctx.predicate().text).isEqualTo(expectedPredicate)
                                    val objects = ctx.`object`().Name()
                                    assertThat(objects).hasSameSizeAs(expectedObjects)
                                    for (i in objects.indices) {
                                        assertThat(objects[i].text).isEqualTo(expectedObjects[i])
                                    }
                                }
                                else -> fail("expected TripleRuleContext")
                            }
                        }
                )

        private fun assertParsesAsSetRule(
                rule: String,
                expectedFunctionName: String,
                expectedParameters: List<String>
        ): Unit =
                parse(rule).fold(
                        { errors -> fail("$errors") },
                        { ctx ->
                            when (ctx) {
                                is TAGORLParser.SetRuleContext -> {
                                    assertThat(ctx.Name().text).isEqualTo(expectedFunctionName)
                                    assertThat(ctx.child()).hasSameSizeAs(expectedParameters)
                                    for (i in ctx.child().indices) {
                                        val child = ctx.child(i)
                                        assert(child is TAGORLParser.OneChildContext)
                                        val c = child as TAGORLParser.OneChildContext
                                        assertThat(c.text).isEqualTo(expectedParameters[i])
                                    }
                                }
                                else -> fail("expected SetRuleContext")
                            }
                        }
                )

        private fun assertParsingFailsWithErrors(
                rule: String,
                expectedErrors: List<String>
        ) =
                parse(rule).fold(
                        { errors -> assertThat(errors).containsExactlyElementsOf(expectedErrors) },
                        { fail("parsing succeeded, where failure was expected") }
                )
    }

}
