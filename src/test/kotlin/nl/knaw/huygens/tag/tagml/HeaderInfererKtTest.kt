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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class HeaderInferrerTest {

    @Test
    fun infer_basic_tagml() {
        val tagml = "[tagml>body [new>text<new] bla<tagml]"
        val expected = """
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |      "tagml": {
            |        "description": "..."
            |      },
            |      "new": {
            |        "description": "..."
            |      }
            |    },
            |    "attributes": {}
            |  }
            |}!]
            """.trimMargin()
        assertHeaderCanBeInferred(tagml) { header ->
            assertThat(header).isEqualTo(expected)
        }
    }

    @Test
    fun infer_attributes() {
        val tagml = "[tagml id=1 title=\"Title\">body [new hilite=true>text<new] bla<tagml]"
        val expected = """
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |      "tagml": {
            |        "description": "...",
            |        "attributes": [
            |          "id",
            |          "title"
            |        ]
            |      },
            |      "new": {
            |        "description": "...",
            |        "attributes": [
            |          "hilite"
            |        ]
            |      }
            |    },
            |    "attributes": {
            |      "id": {
            |        "description": "...",
            |        "dataType": "Integer"
            |      },
            |      "title": {
            |        "description": "...",
            |        "dataType": "String"
            |      },
            |      "hilite": {
            |        "description": "...",
            |        "dataType": "Boolean"
            |      }
            |    }
            |  }
            |}!]
            """.trimMargin()
        assertHeaderCanBeInferred(tagml) { header ->
            assertThat(header).isEqualTo(expected)
        }
    }

    @Test
    fun infer_discontinuity() {
        val tagml = "[tagml>[q>Time is an illusion.<-q] he said, [+q>Lunchtime doubly so.<q]<tagml]"
        val expected = """
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |      "tagml": {
            |        "description": "..."
            |      },
            |      "q": {
            |        "description": "...",
            |        "properties": [
            |          "discontinuous"
            |        ]
            |      }
            |    },
            |    "attributes": {}
            |  }
            |}!]
            """.trimMargin()
        assertHeaderCanBeInferred(tagml) { header ->
            assertThat(header).isEqualTo(expected)
        }
    }

    @Test
    fun infer_milestone() {
        val tagml = """[tagml>[q>Nothing travels faster than the speed of light, with the possible exception of bad news[note text="fake news"], which obeys its own special laws.<q]<tagml]"""
        val expected = """
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |      "tagml": {
            |        "description": "..."
            |      },
            |      "q": {
            |        "description": "..."
            |      },
            |      "note": {
            |        "description": "...",
            |        "properties": [
            |          "milestone"
            |        ],
            |        "attributes": [
            |          "text"
            |        ]
            |      }
            |    },
            |    "attributes": {
            |      "text": {
            |        "description": "...",
            |        "dataType": "String"
            |      }
            |    }
            |  }
            |}!]
            """.trimMargin()
        assertHeaderCanBeInferred(tagml) { header ->
            assertThat(header).isEqualTo(expected)
        }
    }

    private fun assertHeaderCanBeInferred(tagml: String, customAssert: (String) -> Unit) {
        tagml.inferHeader().fold(
                { errors ->
                    fail(errors.errorString())
                },
                { header ->
                    val newTagml = header + tagml
                    when (val result = parse(newTagml)) {
                        is TAGMLParseResult.TAGMLParseSuccess -> {
                            assertThat(result.warnings.isEmpty())
                            customAssert(header)
                        }
                        is TAGMLParseResult.TAGMLParseFailure -> {
                            fail((result.warnings + result.errors).errorString())
                        }
                    }
                }
        )
    }

    private fun List<ErrorListener.TAGError>.errorString() =
            joinToString(separator = "\n", prefix = "\n") { it.message }
}
