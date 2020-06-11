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

object TAGMLTokens {
    open class TAGMLToken(private val range: Range, private val rawContent: String) {
        override fun toString(): String = "$range $rawContent"
    }

    class HeaderToken(range: Range, rawContent: String) : TAGMLToken(range, rawContent)

    class MarkupOpenToken(range: Range, rawContent: String, val qName: String) : TAGMLToken(range, rawContent)

    class MarkupCloseToken(range: Range, rawContent: String, val qName: String) : TAGMLToken(range, rawContent)

    class TextToken(range: Range, rawContent: String) : TAGMLToken(range, rawContent) {
        val isWhiteSpace: Boolean =
                rawContent.isBlank()
    }

}
