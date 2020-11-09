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
package nl.knaw.huygens

import java.net.URI
import kotlin.math.max

fun String.toURI(): URI? =
        try {
            URI.create(this)
        } catch (e: Exception) {
            null
        }

fun ErrorInContext.pretty(wrapAt: Int = 120): String =
        if (this.header == null) {
            this.message
        } else {
            val underlinedSourceLines = this.sourceLineRanges
                    .asSequence()
                    .map {
                        val underline = "-".repeat(it.charRange.last - it.charRange.first)
                        val padded = if (it.charRange.first == 1) underline else underline.padStart(max(0, it.charRange.last - 1))
                        val lineParts = it.sourceLine.chunked(wrapAt)
                        val underlineParts = padded.chunked(wrapAt)
                        lineParts.zip(underlineParts)
                    }
                    .flatten()
                    .filter { it.second.isNotBlank() }
                    .joinToString("\n") { "${it.first}\n${it.second}" }
            "${this.header}: ${this.message}\n$underlinedSourceLines"
        }
