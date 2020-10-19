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

    private fun range(startLine: Int, startChar: Int, endLine: Int, endChar: Int) =
            Range(Position(startLine, startChar), Position(endLine, endChar))

}
