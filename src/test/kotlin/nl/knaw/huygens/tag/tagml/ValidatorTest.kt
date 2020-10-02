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

import nl.knaw.huygens.tag.tagml.AssignedAttribute.OptionalAttribute
import nl.knaw.huygens.tag.tagml.AssignedAttribute.RequiredAttribute
import nl.knaw.huygens.tag.tagml.ErrorListener.TAGError
import nl.knaw.huygens.tag.tagml.OntologyRule.*
import nl.knaw.huygens.tag.tagml.TAGMLParseResult.TAGMLParseFailure
import nl.knaw.huygens.tag.tagml.TAGMLParseResult.TAGMLParseSuccess
import nl.knaw.huygens.tag.tagml.TAGMLToken.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ValidatorTest {

    //    @Disabled("TODO: datatype ID/String")
    @Test
    fun integration_test() {
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
            |      ":non-linear(sic,corr)",
            |      "persName said said"
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

            println(tokens.joinToString("\n"))
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
                                listOf(HierarchyRule("excerpt>chapter+,img+", mapOf(("excerpt" to setOf(QualifiedElement.OneOrMoreElement("chapter"))))),
                                        HierarchyRule("chapter>par+,said+", mapOf(("excerpt" to setOf(QualifiedElement.OneOrMoreElement("chapter"))))),
                                        HierarchyRule("par>s+", mapOf(("excerpt" to setOf(QualifiedElement.OneOrMoreElement("chapter"))))),
                                        HierarchyRule("s>(sic,corr)*", mapOf(("excerpt" to setOf(QualifiedElement.OneOrMoreElement("chapter"))))),
                                        HierarchyRule("said>persName+,emph+,said*", mapOf(("excerpt" to setOf(QualifiedElement.OneOrMoreElement("chapter"))))),
                                        SetRule(":may-not-overlap(said,s)", ":may-not-overlap", listOf("said", "s")),
                                        SetRule(":non-linear(sic,corr)", ":non-linear", listOf("sic", "corr")),
                                        TripleRule("persNamesaissaid", "persName", "said", listOf("said"))
                                )

                        )
                assertThat(ontology.elementDefinitions.values).containsOnly(
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

                val excerptOpen = tokens[1] as MarkupOpenToken
                val excerptClose = tokens[3] as MarkupCloseToken
                val openId = excerptOpen.markupId
                val closeId = excerptClose.markupId
                assertThat(openId).isEqualTo(closeId)
            }
        }
    }

    // TAGML header tests
    @Nested
    inner class HeaderTest {
        @Test
        fun header_without_root_creates_error() {
            val tagml = ("""
            |[!{
            |}!]
            |[tagml > body < tagml]
            |""".trimMargin())
            assertTAGMLHasErrors(tagml) { errors, warnings ->
                assertThat(warnings).isEmpty()
                assertThat(errors.map { it.message }).containsExactly("""Field ":ontology" missing in header.""")
            }
        }

        @Test
        fun root_element_may_not_be_discontinuous() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "root",
            |    "elements": {
            |       "root": {
            |           "description": "the root",
            |           "properties": ["discontinuous"]
            |       }
            |    }
            |   }
            |}!]
            |[root>body<root]
            |""".trimMargin())
            assertTAGMLHasErrors(tagml) { errors, warnings ->
                assertThat(warnings).isEmpty()
                assertThat(errors.map { it.message }).containsExactly("""Root element "root" is not allowed to be discontinuous.""")
            }
        }

        @Test
        fun different_root_in_header_and_body_gives_error() {
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
                assertThat(errors.map { it.message }).containsExactly("""Root element "tagml" does not match the one defined in the header: "root"""")
                assertThat(warnings.map { it.message }).containsExactly("""Element "tagml" is not defined in the ontology.""")
            }
        }

        @Test
        fun element_definition_is_required() {
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
                assertThat(errors.map { it.message }).containsExactly("""Element "tagml" is missing a description.""")
                assertThat(warnings).isEmpty()
            }
        }

        @Test
        fun using_undefined_element_gives_warning() {
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
                assertThat(warnings.map { it.message }).containsExactly("""Element "new" is not defined in the ontology.""")
            }
        }

        @Test
        fun using_undefined_attribute_gives_warning() {
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
                assertThat(warnings.map { it.message }).containsExactly("""Attribute "a" on element "tagml" is not defined in the ontology.""")
            }
        }

        @Test
        fun missing_required_attributes_gives_error() {
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
            |       "required1": { "description": "something", "dataType": "String" },
            |       "required2": { "description": "something", "dataType": "String" },
            |       "optional1": { "description": "something", "dataType": "String" }
            |    }
            |  }
            |}!]
            |[tagml>body<tagml]
            |""".trimMargin())
            assertTAGMLHasErrors(tagml) { errors, warnings ->
                assertThat(errors.map { it.message }).containsExactly(
                        """Required attribute "required1" is missing on element "tagml".""",
                        """Required attribute "required2" is missing on element "tagml"."""
                )
                assertThat(warnings).isEmpty()
            }
        }

        @Test
        fun milestone_elements_must_have_milestone_property() {
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
                assertThat(errors.map { it.message }).containsExactly("""Element "not_a_milestone" does not have the "milestone" property in its definition.""")
                assertThat(warnings.map { it.message }).containsExactly("""Element "undefined" is not defined in the ontology.""")
            }
        }

        @Test
        fun attribute_description_and_datatype_are_required() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |       "tagml": {
            |           "description": "The root element",
            |           "attributes": ["id"]}
            |    },
            |    "attributes": {
            |       "id": {  }
            |    }
            |  }
            |}!]
            |[tagml>body<tagml]
            |""".trimMargin())
            assertTAGMLHasErrors(tagml) { errors, warnings ->
                assertThat(errors.map { it.message }).containsExactly(
                        """Attribute "id" is used on an elementDefinition, but has no valid definition in the ontology.""",
                        """Attribute "id" is missing a dataType.""",
                        """Attribute "id" is missing a description."""
                )
                assertThat(warnings).isEmpty()
            }
        }

        @Test
        fun elements_used_in_rules_must_be_defined() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |       "tagml": {
            |           "description": "The root element"
            |       }    
            |    },
            |    "rules": [
            |       "tagml > book > chapter+ > paragraph+ > line+",
            |       ":may-not-overlap(chapter,verse)",
            |       "author writes book"
            |    ]
            |  }
            |}!]
            |[tagml>body<tagml]
            |""".trimMargin())
            assertTAGMLHasErrors(tagml) { errors, warnings ->
                assertThat(errors.map { it.message }).containsExactly(
                        """Rule "tagml > book > chapter+ > paragraph+ > line+" contains undefined element(s) book, chapter, paragraph, line.""",
                        """Rule ":may-not-overlap(chapter,verse)" contains undefined element(s) chapter, verse.""",
                        """Rule "author writes book" contains undefined element(s) author, book."""
                )
                assertThat(warnings).isEmpty()
            }
        }

    }

    // TAGML body tests
    @Nested
    inner class BodyTest {

        @Test
        fun illegal_closing_tag_error() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml"
            |  }
            |}!]
            |[tagml>body<somethingelse]
            |""".trimMargin())
            assertTAGMLHasErrors(tagml) { errors, warnings ->
                assertThat(errors.map { it.message }).containsExactly("""Closing tag "<somethingelse]" found without corresponding open tag.""")
                assertThat(warnings.map { it.message }).containsExactly("""Element "tagml" is not defined in the ontology.""")
            }
        }

        @Test
        fun root_tag_does_not_match_ontology_root() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml"
            |  }
            |}!]
            |[somethingelse>body<somethingelse]
            |""".trimMargin())
            assertTAGMLHasErrors(tagml) { errors, warnings ->
                assertThat(errors.map { it.message })
                        .containsExactly("""Root element "somethingelse" does not match the one defined in the header: "tagml"""")
                assertThat(warnings.map { it.message })
                        .containsExactly("""Element "somethingelse" is not defined in the ontology.""")
            }
        }

        @Test
        fun repeated_markup() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |      "tagml": {"description": "something"},
            |      "x":     {"description": "something"}
            |    }
            |  }
            |}!]
            |[tagml>[x>Lorem<x] [x>Ipsum<x] [x>Dolor<x]<tagml]
            |""".trimMargin())
            assertTAGMLParses(tagml) { tokens, warnings ->
                assertThat(warnings).isEmpty()
                val tokenIterator = tokens.iterator()
                val headerToken = tokenIterator.next() as HeaderToken

                val tagmlOpen = tokenIterator.next() as MarkupOpenToken
                val x1Open = tokenIterator.next() as MarkupOpenToken
                val textLorem = tokenIterator.next() as TextToken
                assertThat(textLorem.rawContent).isEqualTo("Lorem")

                val x1Close = tokenIterator.next() as MarkupCloseToken
                assertThat(x1Open.markupId).isEqualTo(x1Close.markupId)

                val textSpace1 = tokenIterator.next() as TextToken
                val x2Open = tokenIterator.next() as MarkupOpenToken
                val textIpsum = tokenIterator.next() as TextToken
                assertThat(textIpsum.rawContent).isEqualTo("Ipsum")

                val x2Close = tokenIterator.next() as MarkupCloseToken
                assertThat(x2Open.markupId).isEqualTo(x2Close.markupId)

                val textSpace2 = tokenIterator.next() as TextToken
                val x3Open = tokenIterator.next() as MarkupOpenToken
                val textDolor = tokenIterator.next() as TextToken
                assertThat(textDolor.rawContent).isEqualTo("Dolor")

                val x3Close = tokenIterator.next() as MarkupCloseToken
                assertThat(x3Open.markupId).isEqualTo(x3Close.markupId)

                val tagmlClose = tokenIterator.next() as MarkupCloseToken
                assertThat(tagmlOpen.markupId).isEqualTo(tagmlClose.markupId)

                assertThat(tokenIterator.hasNext()).isFalse()
            }
        }

        //        @Disabled
        @Test
        fun nested_markup() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |      "tagml": {"description": "something"},
            |      "x":     {"description": "something"}
            |    }
            |  }
            |}!]
            |[tagml>[x>Lorem [x>Ipsum<x] Dolor<x]<tagml]
            |""".trimMargin())
            assertTAGMLParses(tagml) { tokens, warnings ->
                assertThat(warnings).isEmpty()

                val tokenIterator = tokens.iterator()
                val headerToken = tokenIterator.next()
                val tagmlOpen = tokenIterator.next() as MarkupOpenToken
                val x1Open = tokenIterator.next() as MarkupOpenToken
                val textLorem = tokenIterator.next() as TextToken
                assertThat(textLorem.rawContent).isEqualTo("Lorem ")

                val x2Open = tokenIterator.next() as MarkupOpenToken
                val textIpsum = tokenIterator.next() as TextToken
                assertThat(textIpsum.rawContent).isEqualTo("Ipsum")

                val x2Close = tokenIterator.next() as MarkupCloseToken
                assertThat(x2Open.markupId).isEqualTo(x2Close.markupId)

                val textDolor = tokenIterator.next() as TextToken
                assertThat(textDolor.rawContent).isEqualTo(" Dolor")

                val x1Close = tokenIterator.next() as MarkupCloseToken
                assertThat(x1Open.markupId).isEqualTo(x1Close.markupId)

                val tagmlClose = tokenIterator.next() as MarkupCloseToken
                assertThat(tagmlOpen.markupId).isEqualTo(tagmlClose.markupId)
            }
        }

        @Test
        fun discontinuous_markup_needs_property() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |      "tagml": {"description": "something"},
            |      "q":     {"description": "quote"}
            |    }
            |  }
            |}!]
            |[tagml>[q>I think,<-q] he thought, [+q>I need to say something now.<q]<tagml]
            |""".trimMargin())
            assertTAGMLHasErrors(tagml) { errors, warnings ->
                assertThat(warnings).isEmpty()
                assertThat(errors.map { it.message }).containsExactly(
                        "Element q may not be suspended: it has not been marked as discontinuous in the ontology."
                )
            }
        }

        @Test
        fun suspended_markup() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |      "tagml": {"description": "something"},
            |      "q":     {
            |                   "description": "quote",
            |                   "properties": ["discontinuous"]
            |               }
            |    }
            |  }
            |}!]
            |[tagml>[q>I think,<-q] he thought, [+q>I need to say something now.<q]<tagml]
            |""".trimMargin())
            assertTAGMLParses(tagml) { tokens, warnings ->
                assertThat(warnings).isEmpty()

                val tokenIterator = tokens.iterator()
                val headerToken = tokenIterator.next() as HeaderToken

                val tagmlOpen = tokenIterator.next() as MarkupOpenToken
                val qOpen = tokenIterator.next() as MarkupOpenToken
                assertThat(qOpen.markupId).isNotEqualTo(tagmlOpen.markupId)

                val textThink = tokenIterator.next() as TextToken
                assertThat(textThink.rawContent).isEqualTo("I think,")

                val qSuspend = tokenIterator.next() as MarkupSuspendToken
                assertThat(qSuspend.markupId).isEqualTo(qOpen.markupId)

                val textHe = tokenIterator.next() as TextToken
                assertThat(textHe.rawContent).isEqualTo(" he thought, ")

                val qResume = tokenIterator.next() as MarkupResumeToken
                assertThat(qResume.markupId).isEqualTo(qOpen.markupId)

                val textNeed = tokenIterator.next() as TextToken
                assertThat(textNeed.rawContent).isEqualTo("I need to say something now.")

                val qClose = tokenIterator.next() as MarkupCloseToken
                assertThat(qClose.markupId).isEqualTo(qOpen.markupId)

                val tagmlClose = tokenIterator.next() as MarkupCloseToken
                assertThat(tagmlOpen.markupId).isEqualTo(tagmlClose.markupId)
            }
        }

        @Test
        fun attributes_not_allowed_on_resume_tag() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |      "tagml": {"description": "something"},
            |      "q":     {
            |                   "description": "quote",
            |                   "properties": ["discontinuous"]
            |               }
            |    }
            |  }
            |}!]
            |[tagml>[q>I think,<-q] he thought, [+q reason="whatever">I need to say something now.<q]<tagml]
            |""".trimMargin())
            assertTAGMLHasErrors(tagml) { errors, warnings ->
                assertThat(errors.map { it.message }).containsExactly(
                        """Resume tag "q" has attributes, this is not allowed"""
                )
                assertThat(warnings.map { it.message }).containsExactly(
                        """Attribute "reason" on element "q" is not defined in the ontology."""
                )
            }
        }

        //        @Disabled
        @Test
        fun overlap() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |      "tagml": {"description": "something"},
            |      "a":     {"description": "something"},
            |      "b":     {"description": "something"}
            |    }
            |  }
            |}!]
            |[tagml|+A,+B>[a|A>Cookie Monster [b|B>likes<a] cookies.<b]<tagml]
            |""".trimMargin())
            assertTAGMLParses(tagml) { tokens, warnings ->
                assertThat(warnings).isEmpty()

                val tokenIterator = tokens.iterator()
                val headerToken = tokenIterator.next()

                val tagmlOpen = tokenIterator.next() as MarkupOpenToken
                val aOpen = tokenIterator.next() as MarkupOpenToken
                assertThat(aOpen.markupId).isNotEqualTo(tagmlOpen.markupId)

                val textCookie = tokenIterator.next() as TextToken
                assertThat(textCookie.rawContent).isEqualTo("Cookie Monster ")

                val bOpen = tokenIterator.next() as MarkupOpenToken
                val textLikes = tokenIterator.next() as TextToken
                assertThat(textLikes.rawContent).isEqualTo("likes")

                val aClose = tokenIterator.next() as MarkupCloseToken
                assertThat(aClose.markupId).isEqualTo(aOpen.markupId)

                val textCookies = tokenIterator.next() as TextToken
                assertThat(textCookies.rawContent).isEqualTo(" cookies.")

                val bClose = tokenIterator.next() as MarkupCloseToken
                assertThat(bClose.markupId).isEqualTo(bOpen.markupId)

                val tagmlClose = tokenIterator.next() as MarkupCloseToken
                assertThat(tagmlOpen.markupId).isEqualTo(tagmlClose.markupId)
            }
        }

        @Test
        fun defined_attribute_datatype_must_match_use() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |       "tagml": {
            |           "description": "The root element",
            |           "attributes": ["string","int"]}
            |    },
            |    "attributes": {
            |       "string": { "description": "something", "dataType": "String" },
            |       "int": { "description": "something", "dataType": "Integer" }
            |    }
            |  }
            |}!]
            |[tagml string=42 int="foo">body<tagml]
            |""".trimMargin())
            assertTAGMLHasErrors(tagml) { errors, warnings ->
                assertThat(errors.map { it.message }).containsExactly(
                        """Attribute "string" is defined as dataType String, but is used as dataType Integer""",
                        """Attribute "int" is defined as dataType Integer, but is used as dataType String"""
                )
                assertThat(warnings).isEmpty()
            }
            // TODO: test all DataTypes
        }

        @Test
        fun predefined_namespaces_are_allowed() {
            val tagml = ("""
            |[!{
            |  ":namespaces": {
            |    "a": "http://example.org/namespace/a",
            |    "b": "http://example.org/namespace/b"
            |  },
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |      "tagml": {"description": "..."},
            |      "a:w": {"description": "..."},
            |      "b:w": {"description": "..."}
            |    }
            |  }
            |}!]
            |[tagml>[a:w>Lorem<a:w] [b:w>ipsum<b:w] dolor.<tagml]
            |""".trimMargin())
            assertTAGMLParses(tagml) { tokens, warnings ->
                assertThat(warnings).isEmpty()
                val tokenIterator = tokens.iterator()
                val headerToken = tokenIterator.next() as HeaderToken
                assertThat(headerToken.namespaces).containsOnlyKeys("a", "b")
            }
        }

        @Test
        fun undefined_namespaces_are_not_allowed() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |      "tagml": {"description": "..."}
            |    }
            |  }
            |}!]
            |[tagml>[a:w>Lorem<a:w] [b:w>ipsum<b:w] dolor.<tagml]
            |""".trimMargin())
            assertTAGMLHasErrors(tagml) { errors, warnings ->
                assertThat(errors.map { it.message }).containsExactly(
                        """Namespace "a" has not been defined in the header.""",
                        """Namespace "a" has not been defined in the header.""",
                        """Namespace "b" has not been defined in the header.""",
                        """Namespace "b" has not been defined in the header."""
                )
                assertThat(warnings.map { it.message }).containsExactly(
                        """Element "a:w" is not defined in the ontology.""",
                        """Element "b:w" is not defined in the ontology."""
                )
            }
        }

        @Test
        fun markup_hierarchy_as_defined_in_ontology_rules() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |      "tagml": {"description": "..."},
            |      "book": {"description": "..."},
            |      "title": {"description": "..."},
            |      "chapter": {"description": "..."},
            |      "p": {"description": "..."},
            |      "l": {"description": "..."}
            |    },
            |    "rules": [
            |       "tagml > book",
            |       "book > title, chapter+",
            |       "chapter > p+ > l+"
            |    ]
            |  }
            |}!]
            |[tagml>
            |  [book>
            |    [title>Example<title]
            |    [chapter>
            |      [p>
            |        [l>Lorem Ipsum<l]
            |      <p]
            |    <chapter]
            |  <book]
            |<tagml]
            |""".trimMargin())
            assertTAGMLParses(tagml) { tokens, warnings ->
                SoftAssertions().apply {
                    assertThat(warnings).isEmpty()
                    assertThat(tokens).hasSize(24)
                    assertAll()
                }
            }
        }

        @Test
        fun markup_hierarchy_does_not_follow_ontology_rules() {
            val tagml = ("""
            |[!{
            |  ":ontology": {
            |    "root": "tagml",
            |    "elements": {
            |      "tagml": {"description": "..."},
            |      "book": {"description": "..."},
            |      "title": {"description": "..."},
            |      "chapter": {"description": "..."},
            |      "p": {"description": "..."},
            |      "l": {"description": "..."}
            |    },
            |    "rules": [
            |       "tagml > book > title, chapter+",
            |       "chapter > p+ > l+"
            |    ]
            |  }
            |}!]
            |[tagml>
            |  [book>
            |    [p>
            |      [l>Lorem Ipsum 1<l]
            |    <p]
            |  <book]
            |<tagml]
            |""".trimMargin())
            assertTAGMLHasErrors(tagml) { errors, warnings ->
                println(warnings.joinToString(separator = "\n", prefix = "warnings:") { it.message })
                println(errors.joinToString(separator = "\n", prefix = "errors:") { it.message })
                SoftAssertions().apply {
                    assertThat(warnings).isEmpty()
                    assertThat(errors.map { it.message }).containsExactly("Unexpected opening tag: found [p> as child of [book>, but expected [title> or [chapter>.")
                    assertAll()
                }
            }
        }

    }

    companion object {
        private fun assertTAGMLParses(tagml: String, tokenListAssert: (List<TAGMLToken>, List<TAGError>) -> Unit) =
                when (val result = parse(tagml)) {
                    is TAGMLParseSuccess -> tokenListAssert(result.tokens, result.warnings)
                    is TAGMLParseFailure -> {
                        val errors = result.errors.joinToString("\n")
                        fail("parsing errors:\n$errors")
                    }
                }

        private fun assertTAGMLHasErrors(tagml: String, errorListAssert: (List<TAGError>, List<TAGError>) -> Unit) =
                when (val result = parse(tagml)) {
                    is TAGMLParseSuccess -> fail("expected parsing to fail")
                    is TAGMLParseFailure -> errorListAssert(result.errors, result.warnings)
                }

        private fun assertOntologyParses(headerToken: HeaderToken, ontologyAssert: (TAGOntology) -> Unit) {
            headerToken.ontologyParseResult.fold(
                    { errorList ->
                        val errors = errorList.joinToString("\n")
                        fail("parsing errors:\n$errors")
                    },
                    { tagOntology -> ontologyAssert(tagOntology) }
            )
        }
    }
}
