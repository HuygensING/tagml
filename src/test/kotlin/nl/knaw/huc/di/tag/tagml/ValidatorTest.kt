package nl.knaw.huc.di.tag.tagml

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

import nl.knaw.huc.di.tag.tagml.ErrorListener.TAGError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test

class ValidatorTest {

    @Test
    fun test_correct_tagml() {
        val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "excerpt",
            |    "elements": {
            |      "excerpt": {
            |        "description": "A short extract from a text",
            |        "attributes": [
            |          "type",
            |          "title",
            |          "author",
            |          "year",
            |          "page",
            |          "persons",
            |          "id!"
            |        ]
            |        },
            |      "img": {
            |        "description": "Image; the (external) representation of the document containing the text",
            |        "properties": ["milestone"],
            |        "attributes": ["source"]
            |        },
            |      "chapter": {
            |        "description": "Main division of a text",
            |        "attributes": ["n"]
            |        },
            |      "par": {
            |        "description": "A distinct section in a text, indicated by a new line or an indentation",
            |        "attributes": ["n"]
            |        },
            |      "s": {
            |        "description": "contains a sentence-like division of a text",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-s.html"
            |        },
            |      "sic": {
            |        "description": "contains text reproduced although apparently incorrect or inaccurate",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-sic.html"
            |        },
            |      "corr": {
            |        "description": "contains the correct form of a passage apparently erroneous in the copy text",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-corr.html"
            |        },
            |      "said": {
            |        "description": "(speech or thought) indicates passages thought or spoken aloud",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-said.html",
            |        "attributes": ["who"],
            |        "properties": ["discontinuous"]
            |        },
            |      "persName": {
            |        "description": "personal name: contains a proper noun or proper noun phrase referring to a person",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-persName.html",
            |        "attributes": ["id"]
            |        },
            |      "emph": {
            |        "description": "(emphasized) marks words or phrases which are stressed or emphasized for linguistic or rhetorical effect",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-emph.html"
            |        }
            |        },
            |    "attributes": {
            |      "type": {
            |        "description": "used to classify the source of the text in the document",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-att.typed.html",
            |        "dataType": "String"
            |        },
            |      "title": {
            |        "description": "used to indicate the title of the text in the document",
            |        "dataType": "String"
            |        },
            |      "source": {
            |        "description": "refers to the source of the (external) representation of the document containing the text",
            |        "dataType": "URI"
            |        },
            |      "author": {
            |        "description": "refers to the name of the author(s) of the text in the document",
            |        "dataType": "Pointer"
            |        },
            |      "year": {
            |        "description": "refers to the year of publication of the text in the document",
            |        "dataType": "Integer"
            |        },
            |      "page": {
            |        "description": "indicates the page(s) of the text in the document",
            |        "dataType": "IntegerList"
            |        },
            |      "persons": {
            |        "description": "(fictional) persons mentioned in the document",
            |        "dataType": "StringList"
            |        },
            |      "id": {
            |        "description": "points to a unique identifier for the element bearing the attribute",
            |        "dataType": "ID"
            |        },
            |      "who": {
            |        "description": "points to the unique identifier of a person in the document",
            |        "dataType": "Pointer"
            |        },
            |      "n": {
            |        "description": "gives a number for an element which is not necessarily unique in the document",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-att.global.html",
            |        "dataType": "Integer"
            |        }
            |      },
            |    "rules": [
            |      "excerpt > chapter+, img+",
            |      "chapter > par+, said+",
            |      "par > s+",
            |      "s > (sic, corr)*",
            |      "said > persName+, emph+, said*",
            |      ":may-not-overlap(said,s)",
            |      ":non-linear(sic,corr)"
            |    ]
            |  },
            |  ":authors": ["me", "you", "them"],
            |  "title": "test",
            |  "version": 0.2
            |}!]
            |[excerpt id="test001">body<excerpt]
            |""".trimMargin())

        assertTAGMLParses(tagml) { tokens, warnings ->
            assertThat(warnings).isEmpty()

            println(tokens)
            assertThat(tokens).hasSize(5)
            assertThat(tokens[0])
                    .isInstanceOf(HeaderToken::class.java)
            val headerToken = tokens[0] as HeaderToken
            assertThat(headerToken.headerMap)
                    .containsOnlyKeys("version", ":ontology", ":authors", "title")
                    .containsEntry("title", "test")
                    .containsEntry("version", "0.2")
            assertThat(tokens[1])
                    .isInstanceOf(MarkupOpenToken::class.java)
                    .hasFieldOrPropertyWithValue("qName", "excerpt")
            assertThat(tokens[2])
                    .isInstanceOf(TextToken::class.java)
                    .hasFieldOrPropertyWithValue("rawContent", "body")
                    .hasFieldOrPropertyWithValue("isWhiteSpace", false)
            assertThat(tokens[3])
                    .isInstanceOf(MarkupCloseToken::class.java)
                    .hasFieldOrPropertyWithValue("qName", "excerpt")
            assertThat(tokens[4])
                    .isInstanceOf(TextToken::class.java)
                    .hasFieldOrPropertyWithValue("isWhiteSpace", true)

            assertOntologyParses(headerToken) { ontology ->
                assertThat(ontology)
                        .hasFieldOrPropertyWithValue("root", "excerpt")
                        .hasFieldOrPropertyWithValue(
                                "rules",
                                listOf("excerpt > chapter+, img+",
                                        "chapter > par+, said+",
                                        "par > s+",
                                        "s > (sic, corr)*",
                                        "said > persName+, emph+, said*",
                                        ":may-not-overlap(said,s)",
                                        ":non-linear(sic,corr)")
                        )
                assertThat(ontology.elements).containsOnly(
                        ElementDefinition(
                                name = "excerpt",
                                description = "A short extract from a text",
                                attributes = listOf(
                                        OptionalAttribute("type"),
                                        OptionalAttribute("title"),
                                        OptionalAttribute("author"),
                                        OptionalAttribute("year"),
                                        OptionalAttribute("page"),
                                        OptionalAttribute("persons"),
                                        RequiredAttribute("id")
                                )
                        ),
                        ElementDefinition(
                                name = "img",
                                description = "Image; the (external) representation of the document containing the text",
                                attributes = listOf(OptionalAttribute("source")),
                                properties = listOf("milestone")
                        ),
                        ElementDefinition(
                                name = "chapter",
                                description = "Main division of a text",
                                attributes = listOf(OptionalAttribute("n"))
                        ),
                        ElementDefinition(
                                name = "par",
                                description = "A distinct section in a text, indicated by a new line or an indentation",
                                attributes = listOf(OptionalAttribute("n"))
                        ),
                        ElementDefinition(
                                name = "s",
                                description = "contains a sentence-like division of a text",
                                ref = "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-s.html"
                        ),
                        ElementDefinition(
                                name = "sic",
                                description = "contains text reproduced although apparently incorrect or inaccurate",
                                ref = "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-sic.html"
                        ),
                        ElementDefinition(
                                name = "corr",
                                description = "contains the correct form of a passage apparently erroneous in the copy text",
                                ref = "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-corr.html"
                        ),
                        ElementDefinition(
                                name = "said",
                                description = "(speech or thought) indicates passages thought or spoken aloud",
                                attributes = listOf(OptionalAttribute("who")),
                                properties = listOf("discontinuous"),
                                ref = "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-said.html"
                        ),
                        ElementDefinition(
                                name = "emph",
                                description = "(emphasized) marks words or phrases which are stressed or emphasized for linguistic or rhetorical effect",
                                ref = "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-emph.html"
                        ),
                        ElementDefinition(
                                name = "persName",
                                description = "personal name: contains a proper noun or proper noun phrase referring to a person",
                                attributes = listOf(OptionalAttribute("id")),
                                ref = "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-persName.html"
                        )
                )

                val saidElementDefinition = ontology.elementDefinition("said")!!
                assert(saidElementDefinition.isDiscontinuous)
                assert(!saidElementDefinition.isMilestone)

                val imgElementDefinition = ontology.elementDefinition("img")!!
                assert(imgElementDefinition.isMilestone)
                assert(!imgElementDefinition.isDiscontinuous)
            }
        }
    }

    @Test
    fun test_header_without_root_creates_error() {
        val tagml = ("""
            |[!{
            |}!]
            |[tagml > body < tagml]
            |""".trimMargin())
        assertTAGMLHasErrors(tagml) { errors, warnings ->
            assertThat(warnings).isEmpty()
            assertThat(errors).hasSize(1)
            assertThat(errors[0])
                    .hasFieldOrPropertyWithValue("message", """Field ":ontology" missing in header.""")
        }
    }

    @Test
    fun test_different_root_in_header_and_body_gives_error() {
        val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "root"
            |   }
            |}!]
            |[tagml>body<tagml]
            |""".trimMargin())
        assertTAGMLHasErrors(tagml) { errors, warnings ->
            assertThat(errors).hasSize(1)
            assertThat(errors[0])
                    .hasFieldOrPropertyWithValue("message", """Root element "tagml" does not match the one defined in the header: "root"""")
            assertThat(warnings).hasSize(1)
            assertThat(warnings[0])
                    .hasFieldOrPropertyWithValue("message", """Element "tagml" is not defined in the ontology.""")
        }
    }

    @Test
    fun test_element_definition_is_required() {
        val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |       "tagml": {}
            |    }
            |  }
            |}!]
            |[tagml>body<tagml]
            |""".trimMargin())
        assertTAGMLHasErrors(tagml) { errors, warnings ->
            assertThat(errors).hasSize(1)
            assertThat(errors[0])
                    .hasFieldOrPropertyWithValue("message", """Element "tagml" is missing a description.""")
            assertThat(warnings).isEmpty()
        }
    }

    @Test
    fun test_using_undefined_element_gives_warning() {
        val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |       "tagml": {"description":"The root element"}
            |    }
            |  }
            |}!]
            |[tagml>body [new>text<new] bla<tagml]
            |""".trimMargin())
        assertTAGMLParses(tagml) { _, warnings ->
            assertThat(warnings).hasSize(1)
            assertThat(warnings[0])
                    .hasFieldOrPropertyWithValue("message", """Element "new" is not defined in the ontology.""")
        }
    }

    @Test
    fun test_using_undefined_attribute_gives_warning() {
        val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |       "tagml": {"description":"The root element"}
            |    }
            |  }
            |}!]
            |[tagml a=true>body<tagml]
            |""".trimMargin())
        assertTAGMLParses(tagml) { _, warnings ->
            assertThat(warnings).hasSize(1)
            assertThat(warnings[0])
                    .hasFieldOrPropertyWithValue("message", """Attribute "a" on element "tagml" is not defined in the ontology.""")
        }
    }

    @Test
    fun test_missing_required_attributes_gives_error() {
        val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |       "tagml": {
            |           "description": "The root element",
            |           "attributes": ["required1!","optional1","required2!"]}
            |    },
            |    "attributes": {
            |       "required1": { "description": "something" },
            |       "required2": { "description": "something" },
            |       "optional1": { "description": "something" }
            |    }
            |  }
            |}!]
            |[tagml>body<tagml]
            |""".trimMargin())
        assertTAGMLHasErrors(tagml) { errors, warnings ->
            assertThat(errors).hasSize(2)
            assertThat(errors[0])
                    .hasFieldOrPropertyWithValue("message", """Required attribute "required1" is missing on element "tagml".""")
            assertThat(errors[1])
                    .hasFieldOrPropertyWithValue("message", """Required attribute "required2" is missing on element "tagml".""")
            assertThat(warnings).isEmpty()
        }
    }

    @Test
    fun test_milestone_elements_must_have_milestone_property() {
        val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |       "tagml": { "description": "The root element" },
            |       "milestone": { "description": "A milestone element", "properties": ["milestone"] },
            |       "not_a_milestone": { "description": "Non-milestone element" }
            |    }
            |  }
            |}!]
            |[tagml>[milestone][not_a_milestone][undefined]<tagml]
            |""".trimMargin())
        assertTAGMLHasErrors(tagml) { errors, warnings ->
            assertThat(errors).hasSize(1)
            assertThat(errors[0])
                    .hasFieldOrPropertyWithValue("message", """Element "not_a_milestone" does not have the "milestone" property in its definition.""")
            assertThat(warnings).hasSize(1)
            assertThat(warnings[0])
                    .hasFieldOrPropertyWithValue("message", """Element "undefined" is not defined in the ontology.""")
        }
    }

    @Test
    fun test_illegal_closing_tag_error() {
        val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml"
            |  }
            |}!]
            |[tagml>body<somethingelse]
            |""".trimMargin())
        assertTAGMLHasErrors(tagml) { errors, warnings ->
            assertThat(errors).hasSize(1)
            assertThat(errors[0])
                    .hasFieldOrPropertyWithValue("message", """Closing tag "<somethingelse]" found without corresponding open tag.""")
            assertThat(warnings).hasSize(1)
            assertThat(warnings[0])
                    .hasFieldOrPropertyWithValue("message", """Element "tagml" is not defined in the ontology.""")
        }
    }

    private fun assertTAGMLParses(tagml: String, tokenListAssert: (List<TAGMLToken>, List<TAGError>) -> Unit) =
            when (val result = validate(tagml)) {
                is TAGMLParseSuccess -> tokenListAssert(result.tokens, result.warnings)
                is TAGMLParseFailure -> {
                    val errors = result.errors.joinToString("\n")
                    fail("parsing errors:\n$errors")
                }
            }

    private fun assertTAGMLHasErrors(tagml: String, errorListAssert: (List<TAGError>, List<TAGError>) -> Unit) =
            when (val result = validate(tagml)) {
                is TAGMLParseSuccess -> fail("expected parsing to fail")
                is TAGMLParseFailure -> errorListAssert(result.errors, result.warnings)
            }

    private fun assertOntologyParses(headerToken: HeaderToken, ontologyAssert: (TAGOntology) -> Unit) {
        headerToken.ontologyParseResult.fold(
                { errorList ->
                    val errors = errorList.joinToString("\n")
                    fail("parsing errors:\n$errors")
                },
                { ontologyAssert(it) }
        )
    }

}
