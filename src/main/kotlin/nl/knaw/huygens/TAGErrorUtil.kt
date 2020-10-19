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

import nl.knaw.huygens.tag.tagml.ErrorListener

data class SourceLineRange(val sourceLine: String, val charRange: IntRange)

data class ErrorInContext(
        val message: String,
        val header: String? = null,
        val sourceLineRanges: List<SourceLineRange> = emptyList()
)

fun ErrorInContext.pretty(): String =
        if (this.header == null) {
            this.message
        } else {
            val underlinedSourceLines = this.sourceLineRanges.joinToString("\n") {
                val underline = "-".repeat(it.charRange.last - it.charRange.first)
                val padded = if (it.charRange.first == 1) underline else underline.padStart(it.sourceLine.length)
                "${it.sourceLine}\n$padded"
            }
            "${this.header}: ${this.message}\n$underlinedSourceLines"
        }

class TAGErrorUtil(tagml: String) {
    private val tagmlLines: List<String> = tagml.split("\n")

    fun errorInContext(error: ErrorListener.TAGError): ErrorInContext =
            when (error) {
                is ErrorListener.RangedTAGError -> {
                    val range = error.range
                    val startPosition = range.startPosition
                    val startLine = startPosition.line
                    val endPosition = range.endPosition
                    val endLine = endPosition.line
                    val lineRangeString = "line " + if (startLine == endLine) "$startLine" else "$startLine-$endLine"
                    val sourceLineRanges = tagmlLines.subList(startLine - 1, endLine)
                            .mapIndexed { index, line ->
                                line to when (index) {
                                    0 -> IntRange(startPosition.character, if (startLine == endLine) endPosition.character else line.length + 1)
                                    endLine - startLine -> IntRange(1, endPosition.character)
                                    else -> IntRange(1, line.length + 1)
                                }
                            }
                            .map { SourceLineRange(it.first, it.second) }
                    ErrorInContext(
                            error.message,
                            lineRangeString,
                            sourceLineRanges
                    )
                }
                else -> ErrorInContext(error.message)
            }
}
