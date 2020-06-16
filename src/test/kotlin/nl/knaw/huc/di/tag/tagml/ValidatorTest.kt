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
import nl.knaw.huc.di.tag.tagml.ParserUtils.validate
import nl.knaw.huc.di.tag.tagml.TAGMLTokens.HeaderToken
import nl.knaw.huc.di.tag.tagml.TAGMLTokens.MarkupCloseToken
import nl.knaw.huc.di.tag.tagml.TAGMLTokens.MarkupOpenToken
import nl.knaw.huc.di.tag.tagml.TAGMLTokens.TAGMLToken
import nl.knaw.huc.di.tag.tagml.TAGMLTokens.TextToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test

class ValidatorTest {

    @Test
    fun test_header_without_root_creates_error() {
        val tagml = ("""
            |[!{
            |}!]
            |[tagml>body<tagml]
            |""".trimMargin())
        assertTAGMLHasErrors(tagml) { errors ->
            assertThat(errors).hasSize(1)
            assertThat(errors[0])
                    .hasFieldOrPropertyWithValue("message", """Field ":ontology" missing in header.""")
        }
    }

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
            |          "id"
            |        ]
            |      },
            |      "img": {
            |        "description": "Image; the (external) representation of the document containing the text",
            |        "properties": [ "milestone" ],
            |        "attributes": [ "source" ]
            |      },
            |      "chapter": {
            |        "description": "Main division of a text",
            |        "attributes": [ "n" ]
            |      },
            |      "par": {
            |        "description": "A distinct section in a text, indicated by a new line or an indentation",
            |        "attributes": [ "n" ]
            |      },
            |      "s": {
            |        "description": "contains a sentence-like division of a text",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-s.html"
            |      },
            |      "sic": {
            |        "description": "contains text reproduced although apparently incorrect or inaccurate",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-sic.html"
            |      },
            |      "corr": {
            |        "description": "contains the correct form of a passage apparently erroneous in the copy text",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-corr.html"
            |      },
            |      "said": {
            |        "description": "(speech or thought) indicates passages thought or spoken aloud",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-said.html",
            |        "attributes": [ "who" ],
            |        "properties": [ "discontinuous" ]
            |      },
            |      "persName": {
            |        "description": "personal name: contains a proper noun or proper noun phrase referring to a person",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-persName.html",
            |        "attributes": [ "id" ]
            |      },
            |      "emph": {
            |        "description": "(emphasized) marks words or phrases which are stressed or emphasized for linguistic or rhetorical effect",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-emph.html"
            |      }
            |    },
            |    "attributes": {
            |      "type": {
            |        "description": "used to classify the source of the text in the document",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-att.typed.html",
            |        "dataType": "String"
            |      },
            |      "title": {
            |        "description": "used to indicate the title of the text in the document",
            |        "dataType": "String"
            |      },
            |      "source": {
            |        "description": "refers to the source of the (external) representation of the document containing the text",
            |        "dataType": "URI"
            |      },
            |      "author": {
            |        "description": "refers to the name of the author(s) of the text in the document",
            |        "dataType": "Pointer"
            |      },
            |      "year": {
            |        "description": "refers to the year of publication of the text in the document",
            |        "dataType": "Integer"
            |      },
            |      "page": {
            |        "description": "indicates the page(s) of the text in the document",
            |        "dataType": "IntegerList"
            |      },
            |      "persons": {
            |        "description": "(fictional) persons mentioned in the document",
            |        "dataType": "StringList"
            |      },
            |      "id": {
            |        "description": "points to a unique identifier for the element bearing the attribute",
            |        "dataType": "ID"
            |      },
            |      "who": {
            |        "description": "points to the unique identifier of a person in the document",
            |        "dataType": "Pointer"
            |      },
            |      "n": {
            |        "description": "gives a number for an element which is not necessarily unique in the document",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-att.global.html",
            |        "dataType": "Integer"
            |      }
            |    },
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
            |  ":authors": [ "me", "you", "them" ],
            |  "title": "test",
            |  "version": 0.2
            |}!]
            |[excerpt>body<excerpt]
            |""".trimMargin())

        assertTAGMLParses(tagml) { tokens ->
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
            }
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
        assertTAGMLHasErrors(tagml) { errors ->
            assertThat(errors).hasSize(1)
            assertThat(errors[0])
                    .hasFieldOrPropertyWithValue("message", "Closing tag found without corresponding open tag: <somethingelse]")
        }
    }

    @Test
    fun test_different_root_in_header_and_body_gives_error() {
        val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "root"
            |  }
            |}!]
            |[tagml>body<tagml]
            |""".trimMargin())
        assertTAGMLHasErrors(tagml) { errors ->
            assertThat(errors).hasSize(1)
            assertThat(errors[0])
                    .hasFieldOrPropertyWithValue("message", """Root element "tagml" does not match the one defined in the header: "root"""")
        }
    }

    private fun assertTAGMLParses(tagml: String, tokenListAssert: (List<TAGMLToken>) -> Unit) {
        validate(tagml).fold(
                { errorList ->
                    val errors = errorList.joinToString("\n")
                    fail("parsing errors:\n$errors")
                },
                { tokenListAssert(it) }
        )
    }

    private fun assertTAGMLHasErrors(tagml: String, errorListAssert: (List<TAGError>) -> Unit) {
        validate(tagml).fold(
                { errorListAssert(it) },
                { fail("expected parsing to fail") }
        )
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
