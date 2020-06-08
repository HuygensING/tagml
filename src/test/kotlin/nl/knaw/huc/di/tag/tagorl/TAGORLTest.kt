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
package nl.knaw.huc.di.tag.tagorl

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Left
import arrow.core.Right
import nl.knaw.huc.di.tag.ANTLRUtils.printTAGORLTokens
import nl.knaw.huc.di.tag.tagml.TestErrorListener
import nl.knaw.huc.di.tag.tagorl.TAGORLParser.OneOrMoreChildContext
import nl.knaw.huc.di.tag.tagorl.TAGORLParser.OptionalChildContext
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TAGORLTest {

    val log: Logger = LoggerFactory.getLogger(TAGORLTest::class.java)

//    "excerpt > chapter+, img+",
//    "chapter > par+, said+",
//    "par > s+",
//    "s > (sic, corr)*",
//    "said > persName+, emph+, said*",
//    "said may-not-overlap-with s",
//    "sic is-non-linear-with corr"

    @Test
    fun test_hierarchy_rule_1() {
        val rule = "excerpt > chapter+, img?"
        val expectedParent = "excerpt"
        val expectedChildren1 = "chapter"
        val expectedChildren = "img"
        when (val result = parse(rule)) {
            is Left -> fail("$result.a")
            is Right -> {
                when (result.b) {
                    is TAGORLParser.HierarchyRuleContext -> {
                        val hrc = result.b as TAGORLParser.HierarchyRuleContext
                        assertThat(hrc.Name().text).isEqualTo(expectedParent)

                        val child0 = hrc.children().child(0) as OneOrMoreChildContext
                        assertThat(child0.Name().text).isEqualTo(expectedChildren1)

                        val child1 = hrc.children().child(1) as OptionalChildContext
                        assertThat(child1.Name().text).isEqualTo(expectedChildren)
                    }
                    else -> fail("expected HierarchyRuleContext")
                }
            }
        }
    }

    @Test
    fun test_set_rule_with_1_parameter_fails() {
        val rule = "test(parameter)"
        assertParsingFailsWithErrors(rule, listOf("syntax error: line 1:14 mismatched input ')' expecting ','"))
    }

    @Test
    fun test_set_rule_with_2_parameters() {
        val rule = "nonlinear(sic,corr)"
        assertParsesAsSetRule(rule, "nonlinear", listOf("sic", "corr"))
    }

    @Test
    fun test_set_rule_with_3_parameters() {
        val rule = "siblings(huey,dewey,louie)"
        assertParsesAsSetRule(rule, "siblings", listOf("huey", "dewey", "louie"))
    }

    @Test
    fun test_failing_set_rule() {
        val rule = "test(parameter"
        assertParsingFailsWithErrors(rule, listOf("syntax error: line 1:14 mismatched input '<EOF>' expecting ','"))
    }

    @Test
    fun test_triple_rule() {
        val rule = "author writes title"
        assertParsesAsTripleRule(rule, "author", "writes", listOf("title"))
    }

    @Test
    fun test_triple_rule_with_multiple_objects() {
        val rule = "cook prepares breakfast,lunch,dinner"
        assertParsesAsTripleRule(rule, "cook", "prepares", listOf("breakfast", "lunch", "dinner"))
    }

    private fun assertParsingFailsWithErrors(
            rule: String,
            expectedErrors: List<String>
    ) =
            when (val result = parse(rule)) {
                is Left -> {
                    assertThat(result.a).containsExactlyElementsOf(expectedErrors)
                }
                is Right -> fail("parsing succeeded, where failure was expected")
            }

    private fun assertParsesAsSetRule(
            rule: String,
            expectedFunctionName: String,
            expectedParameters: List<String>
    ) =
            when (val result = parse(rule)) {
                is Left -> fail("$result.a")
                is Right -> {
                    when (result.b) {
                        is TAGORLParser.SetRuleContext -> {
                            val sr = result.b as TAGORLParser.SetRuleContext
                            assertThat(sr.Name().text).isEqualTo(expectedFunctionName)
                            assertThat(sr.child()).hasSameSizeAs(expectedParameters)
                            for (i in sr.child().indices) {
                                val child = sr.child(i)
                                assert(child is TAGORLParser.OneChildContext)
                                val c = child as TAGORLParser.OneChildContext
                                assertThat(c.text).isEqualTo(expectedParameters[i])
                            }
                        }
                        else -> fail("expected SetRuleContext")
                    }
                }
            }

    private fun assertParsesAsTripleRule(
            rule: String,
            expectedSubject: String,
            expectedPredicate: String,
            expectedObjects: List<String>
    ) =
            when (val result = parse(rule)) {
                is Left -> fail("$result.a")
                is Right -> {
                    when (result.b) {
                        is TAGORLParser.TripleRuleContext -> {
                            val tr = result.b as TAGORLParser.TripleRuleContext
                            assertThat(tr.subject().text).isEqualTo(expectedSubject)
                            assertThat(tr.predicate().text).isEqualTo(expectedPredicate)
                            val objects = tr.`object`().Name()
                            assertThat(objects).hasSameSizeAs(expectedObjects)
                            for (i in objects.indices) {
                                assertThat(objects[i].text).isEqualTo(expectedObjects[i])
                            }
                        }
                        else -> fail("expected TripleRuleContext")
                    }
                }
            }

    private fun assertParseSucceeds(rule: String) {
        val result = parse(rule)
        assert(result is Right)
    }

    private fun assertParseFails(tagml: String) {
        val result = parse(tagml)
        assert(result is Left)
    }

    private fun parse(rule: String): Either<List<String>, ParseTree> {
        printTAGORLTokens(rule)
        val antlrInputStream: CharStream = CharStreams.fromString(rule)
        val lexer = TAGORLLexer(antlrInputStream)
        val errorListener = TestErrorListener()
        lexer.addErrorListener(errorListener)
        val tokens = CommonTokenStream(lexer)
        val parser = TAGORLParser(tokens)
        parser.addErrorListener(errorListener)
        parser.buildParseTree = true
        val parseTree: ParseTree = parser.ontologyRule()
        return if (errorListener.hasErrors) {
            Left(errorListener.errors)
        } else {
            Right(parseTree.getChild(0))
        }
    }

}
