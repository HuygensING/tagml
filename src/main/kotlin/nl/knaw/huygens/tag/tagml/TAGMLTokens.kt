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

sealed class TAGMLToken(private val range: Range, val rawContent: String) {
    override fun toString(): String = "$range $rawContent"

    @Suppress("UNCHECKED_CAST")
    class HeaderToken(range: Range, rawContent: String, val headerMap: Map<String, Any>) : TAGMLToken(range, rawContent) {
        private val namespaceParseResult: ParseResult<Map<String, String>> by lazy {
            headerMap.getOrElse(":namespaces") { Either.Right(mapOf<String, String>()) } as ParseResult<Map<String, String>>
        }

        val namespaces: Map<String, String> by lazy {
            namespaceParseResult.fold(
                    { mapOf() },
                    { map -> map }
            )
        }

        val ontologyParseResult: ParseResult<TAGOntology> by lazy {
            headerMap.getOrElse(":ontology")
            { Either.left(listOf(ErrorListener.CustomError(range, MISSING_ONTOLOGY_FIELD))) }
                    as ParseResult<TAGOntology>
        }
    }

    abstract class MarkupToken(
            range: Range,
            rawContent: String,
            val qName: String,
            val layers: Set<String>
    ) : TAGMLToken(range, rawContent)

    class MarkupOpenToken(
            range: Range,
            rawContent: String,
            qName: String,
            layers: Set<String>,
            val markupId: Long,
            val attributes: List<KeyValue>
    ) : MarkupToken(range, rawContent, qName, layers)

    class MarkupSuspendToken(
            range: Range,
            rawContent: String,
            qName: String,
            layers: Set<String>,
            val markupId: Long
    ) : MarkupToken(range, rawContent, qName, layers)

    class MarkupResumeToken(
            range: Range,
            rawContent: String,
            qName: String,
            layers: Set<String>,
            val markupId: Long
    ) : MarkupToken(range, rawContent, qName, layers)

    class MarkupCloseToken(
            range: Range,
            rawContent: String,
            qName: String,
            layers: Set<String>,
            val markupId: Long
    ) : MarkupToken(range, rawContent, qName, layers)

    class MarkupMilestoneToken(
            range: Range,
            rawContent: String,
            qName: String,
            layers: Set<String>,
            val attributes: List<KeyValue>
    ) : MarkupToken(range, rawContent, qName, layers)

    class TextToken(
            range: Range,
            rawContent: String
    ) : TAGMLToken(range, rawContent) {
        val isWhiteSpace: Boolean =
                rawContent.isBlank()
    }

}
