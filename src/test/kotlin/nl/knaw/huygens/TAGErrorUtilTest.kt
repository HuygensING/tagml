package nl.knaw.huygens

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

import nl.knaw.huygens.tag.tagml.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class TAGErrorUtilTest {

    @Test
    fun test_tag_error_util() {
        val tagml = """
            [!{
                "name": "test",
                "id": "test001",
                ":ontology": {
                  "elements": {
                    "p": {
                       "decription": "typo is intentional"
                    }  
                  }
                },
                "author": [ "me", "you" ]
            }!]
            [p>
            [l>[w>Just<w] [w>some<w] [w>words<w]<l]
            [l>[w>Just<w] [w>ERROR [w>words<w]<l]
            [l>[w>Just<w] [w>some<w] [w>words<w]<l]
            <p]
            """.trimIndent()
        when (val parseResult = parse(tagml)) {
            is TAGMLParseResult.TAGMLParseFailure -> {
                val u = TAGErrorUtil(tagml)
                println("errors:")
                val e0 = parseResult.errors[0]
                val rangedError0 = e0 as ErrorListener.RangedTAGError
                assertThat(rangedError0.range).isEqualTo(range(6, 9, 8, 10))

                val errorInContext0 = u.errorInContext(e0)
                assertThat(errorInContext0.header).isEqualTo("line 6-8")
                assertThat(errorInContext0.message).isEqualTo(e0.message)

                val slr00 = errorInContext0.sourceLineRanges[0]
                assertThat(slr00.charRange).isEqualTo(IntRange(9, 15))

                val slr01 = errorInContext0.sourceLineRanges[1]
                assertThat(slr01.charRange).isEqualTo(IntRange(1, 47))

                val slr02 = errorInContext0.sourceLineRanges[2]
                assertThat(slr02.charRange).isEqualTo(IntRange(1, 10))

                val pretty0 = errorInContext0.pretty()
                val expected0 = """
                    |line 6-8: Element "p" is missing a description.
                    |        "p": {
                    |        ------
                    |           "decription": "typo is intentional"
                    |----------------------------------------------
                    |        }  
                    |---------""".trimMargin()
                println(pretty0)
                assertThat(pretty0).isEqualTo(expected0)

                val e1 = parseResult.errors[1]
                val rangedError1 = e1 as ErrorListener.RangedTAGError
                assertThat(rangedError1.range).isEqualTo(range(7, 12, 7, 47))

                val pretty1 = u.errorInContext(e1).pretty()
                val expected1 = """
                    |line 7: Unknown element field "decription"
                    |           "decription": "typo is intentional"
                    |           -----------------------------------""".trimMargin()
                println(pretty1)
                assertThat(pretty1).isEqualTo(expected1)

                println("warnings:")
                for (e in (parseResult.warnings)) {
                    val errorLines = u.errorInContext(e).pretty()
                    println(errorLines)
                }
            }
            else -> {
                fail("expected error")
            }
        }
    }

    @Test
    fun test_tag_error_util2() {
        val tagml = """
            [!{
              ":ontology": {
                "root": "tagml"
              }
            }!]
            [tagml>[book>[title>Foo Bar<title][chapter>[l>Lorem ipsum dolar amacet.<l]<chapter]<book]<tagml]
            """.trimIndent()
        val u = TAGErrorUtil(tagml)
        when (val parseResult = parse(tagml)) {
            is TAGMLParseResult.TAGMLParseFailure -> {
                fail(parseResult.errors.joinToString("\n") { u.errorInContext(it).pretty() })
            }
            else -> {
                val rangedError0 = parseResult.warnings[0] as ErrorListener.RangedTAGError
                assertThat(rangedError0.range).isEqualTo(range(6, 1, 6, 8))

                val pretty0 = u.errorInContext(rangedError0).pretty()
                val expected0 = """
                    line 6: Element "tagml" is not defined in the ontology.
                    [tagml>[book>[title>Foo Bar<title][chapter>[l>Lorem ipsum dolar amacet.<l]<chapter]<book]<tagml]
                    -------
                    """.trimIndent()
                assertThat(pretty0).isEqualTo(expected0)

                val rangedError1 = parseResult.warnings[1] as ErrorListener.RangedTAGError
                assertThat(rangedError1.range).isEqualTo(range(6, 8, 6, 14))

                val pretty1 = u.errorInContext(rangedError1).pretty()
                val expected1 = """
                    line 6: Element "book" is not defined in the ontology.
                    [tagml>[book>[title>Foo Bar<title][chapter>[l>Lorem ipsum dolar amacet.<l]<chapter]<book]<tagml]
                           ------
                    """.trimIndent()
                assertThat(pretty1).isEqualTo(expected1)

                val rangedError2 = parseResult.warnings[2] as ErrorListener.RangedTAGError
                assertThat(rangedError2.range).isEqualTo(range(6, 14, 6, 21))

                val pretty2 = u.errorInContext(rangedError2).pretty()
                val expected2 = """
                    line 6: Element "title" is not defined in the ontology.
                    [tagml>[book>[title>Foo Bar<title][chapter>[l>Lorem ipsum dolar amacet.<l]<chapter]<book]<tagml]
                                 -------
                    """.trimIndent()
                assertThat(pretty2).isEqualTo(expected2)

                val rangedError3 = parseResult.warnings[3] as ErrorListener.RangedTAGError
                assertThat(rangedError3.range).isEqualTo(range(6, 35, 6, 44))

                val pretty3 = u.errorInContext(rangedError3).pretty()
                val expected3 = """
                    line 6: Element "chapter" is not defined in the ontology.
                    [tagml>[book>[title>Foo Bar<title][chapter>[l>Lorem ipsum dolar amacet.<l]<chapter]<book]<tagml]
                                                      ---------
                    """.trimIndent()
                assertThat(pretty3).isEqualTo(expected3)

                val rangedError4 = parseResult.warnings[4] as ErrorListener.RangedTAGError
                val pretty4 = u.errorInContext(rangedError4).pretty()
                val expected4 = """
                    line 6: Element "l" is not defined in the ontology.
                    [tagml>[book>[title>Foo Bar<title][chapter>[l>Lorem ipsum dolar amacet.<l]<chapter]<book]<tagml]
                                                               ---
                    """.trimIndent()
                assertThat(pretty4).isEqualTo(expected4)

                assertThat(parseResult.warnings).hasSize(5)
            }
        }
    }

    @Test
    fun test_tag_error_util_with_very_long_lines() {
        val tagml = """
            [!{
              ":ontology": {
                "root": "tagml"
              }
            }!]
            [tagml>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor [w>incididunt<w] ut labore et dolore magna aliqua. Pretium fusce id velit ut tortor pretium. Bibendum ut tristique et egestas. Varius vel pharetra vel turpis nunc eget. Neque aliquam vestibulum morbi blandit. Proin sagittis nisl rhoncus mattis. Tempor orci eu lobortis elementum nibh. Egestas maecenas pharetra convallis posuere morbi. Urna condimentum mattis pellentesque id. Mauris in aliquam sem fringilla ut. Sed felis eget velit aliquet sagittis id. Dictum fusce ut placerat orci nulla pellentesque dignissim enim sit. Lorem mollis aliquam ut porttitor leo a. Proin sagittis nisl rhoncus mattis rhoncus. Non quam lacus suspendisse faucibus interdum posuere lorem. Eu nisl nunc mi ipsum faucibus vitae aliquet nec ullamcorper. Vulputate odio ut enim blandit volutpat maecenas volutpat blanño aliquam. Fermentum et sollicitudin ac orci [x>phasellus<x].<tagml]
            """.trimIndent()
        val u = TAGErrorUtil(tagml)
        when (val parseResult = parse(tagml)) {
            is TAGMLParseResult.TAGMLParseFailure -> {
                fail(parseResult.errors.joinToString("\n") { u.errorInContext(it).pretty() })
            }
            else -> {
                SoftAssertions().apply {
                    assertThat(parseResult.warnings).hasSize(3)

                    val rangedError0 = parseResult.warnings[0] as ErrorListener.RangedTAGError
                    assertThat(rangedError0.range).isEqualTo(range(6, 1, 6, 8))

                    val pretty0 = u.errorInContext(rangedError0).pretty()
                    val expected0 = """
                        line 6: Element "tagml" is not defined in the ontology.
                        [tagml>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor [w>incididunt<w] ut labore et dolo
                        -------
                        """.trimIndent()
                    assertThat(pretty0).isEqualTo(expected0)

                    val rangedError1 = parseResult.warnings[1] as ErrorListener.RangedTAGError
                    assertThat(rangedError1.range).isEqualTo(range(6, 87, 6, 90))

                    val pretty1 = u.errorInContext(rangedError1).pretty()
                    val expected1 = """
                        line 6: Element "w" is not defined in the ontology.
                        [tagml>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor [w>incididunt<w] ut labore et dolo
                                                                                                              ---
                        """.trimIndent()
                    assertThat(pretty1).isEqualTo(expected1)

                    val rangedError2 = parseResult.warnings[2] as ErrorListener.RangedTAGError
                    assertThat(rangedError2.range).isEqualTo(range(6, 926, 6, 929))

                    val pretty2 = u.errorInContext(rangedError2).pretty(wrapAt = 80)
                    val expected2 = """
                        line 6: Element "x" is not defined in the ontology.
                        o aliquam. Fermentum et sollicitudin ac orci [x>phasellus<x].<tagml]
                                                                     ---
                        """.trimIndent()
                    assertThat(pretty2).isEqualTo(expected2)

                    assertAll()
                }
            }
        }
    }

    private fun range(startLine: Int, startChar: Int, endLine: Int, endChar: Int) =
            Range(Position(startLine, startChar), Position(endLine, endChar))

}
