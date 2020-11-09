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

import org.antlr.v4.runtime.ANTLRErrorListener
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import java.lang.String.format
import java.util.*

class ErrorListener : ANTLRErrorListener {
    private val errors: MutableList<TAGError> = mutableListOf()
    private val warnings: MutableList<TAGError> = mutableListOf()

    private val reportAmbiguity = false
    private val reportAttemptingFullContext = false
    private val reportContextSensitivity = true

    private var hasBreakingError = false

    class TAGErrorComparator : Comparator<TAGError> {
        override fun compare(e1: TAGError, e2: TAGError): Int = when {
            (e1 is RangedTAGError && e2 is RangedTAGError) -> when {
                e1.range.startPosition.line != e2.range.startPosition.line -> e1.range.startPosition.line.compareTo(e2.range.startPosition.line)
                e1.range.startPosition.character != e2.range.startPosition.character -> e1.range.startPosition.character.compareTo(e2.range.startPosition.character)
                e1.range.endPosition.line != e2.range.endPosition.line -> e1.range.endPosition.line.compareTo(e2.range.endPosition.line)
                e1.range.endPosition.character != e2.range.endPosition.character -> e1.range.endPosition.character.compareTo(e2.range.endPosition.character)
                else -> e1.message.compareTo(e2.message)
            }
            else -> e1.message.compareTo(e2.message)
        }
    }

    val orderedErrors: List<TAGError>
        get() = errors.sortedWith(TAGErrorComparator())/*.withConsecutiveSyntaxErrorsJoined()*/

    val orderedWarnings: List<TAGError>
        get() = warnings.sortedWith(TAGErrorComparator())

    val errorMessages: List<String>
        get() = orderedErrors.map { it.message }

    val hasErrors: Boolean
        get() = errors.isNotEmpty()

    val hasWarnings: Boolean
        get() = warnings.isNotEmpty()

    override fun reportAmbiguity(
            recognizer: Parser,
            dfa: DFA,
            startIndex: Int,
            stopIndex: Int,
            exact: Boolean,
            ambigAlts: BitSet,
            configs: ATNConfigSet) {
        if (reportAmbiguity) {
            val message = """
                ambiguity:
                 recognizer=$recognizer,
                 dfa=$dfa,
                 startIndex=$startIndex,
                 stopIndex=$stopIndex,
                 exact=$exact,
                 ambigAlts=$ambigAlts,
                 configs=$configs""".trimIndent()
            errors += TAGAmbiguityError(message)
        }
    }

    override fun reportAttemptingFullContext(
            recognizer: Parser,
            dfa: DFA,
            startIndex: Int,
            stopIndex: Int,
            conflictingAlts: BitSet,
            configs: ATNConfigSet) {
        if (reportAttemptingFullContext) {
            val message = """
                attempting full context error:
                 recognizer=$recognizer,
                 dfa=$dfa,
                 startIndex=$startIndex,
                 stopIndex=$stopIndex,
                 conflictingAlts=$conflictingAlts,
                 configs=$configs""".trimIndent()
            errors += TAGAttemptingFullContextError(message)
        }
    }

    override fun reportContextSensitivity(
            recognizer: Parser,
            dfa: DFA,
            startIndex: Int,
            stopIndex: Int,
            prediction: Int,
            configs: ATNConfigSet) {
        if (reportContextSensitivity) {
            val message = """
                context sensitivity error:
                 recognizer=$recognizer,
                 dfa=$dfa,
                 startIndex=$startIndex,
                 stopIndex=$stopIndex,
                 prediction=$prediction,
                 configs=$configs""".trimIndent()
            errors += TAGContextSensitivityError(message)
        }
    }

    override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?) {
        val message = format("syntax error: %s", msg.replace("token recognition error at", "unexpected token"))
        val range = syntaxErrorRange(line, charPositionInLine, message)
        errors += TAGSyntaxError(range, message)
    }

    val prefixedErrorMessagesAsString: String
        get() = orderedErrors
                .map(this::prefixedErrorMessage)
                .joinToString { "\n" }

    private fun prefixedErrorMessage(error: TAGError): String =
            when (error) {
                is CustomError -> prefix(error.range.startPosition) + error.message
                is CustomWarning -> "warning: " + prefix(error.range.startPosition) + error.message
                is TAGSyntaxError -> prefix(error.range.startPosition) + error.message
                else -> ""
            }

    private fun prefix(position: Position): String =
            format("line %d:%d : ", position.line, position.character)

    fun addError(
            startPos: Position, endPos: Position, messageTemplate: String, vararg messageArgs: Any) {
        errors += CustomError(startPos, endPos, format(messageTemplate, *messageArgs))
    }

    fun addWarning(
            startPos: Position, endPos: Position, messageTemplate: String, vararg messageArgs: Any) {
        warnings += CustomError(startPos, endPos, format(messageTemplate, *messageArgs))
    }

    fun addBreakingError(
            startPos: Position, endPos: Position, messageTemplate: String, vararg messageArgs: Any) {
        addError(startPos, endPos, messageTemplate, *messageArgs)
        abortParsing(messageTemplate, *messageArgs)
    }

    private fun abortParsing(messageTemplate: String, vararg messageArgs: Any) {
        hasBreakingError = true
        throw TAGMLBreakingError("""
                ${format(messageTemplate, *messageArgs)}
                parsing aborted!
                """.trimIndent())
    }

    fun addErrors(list: List<TAGError>) {
        errors += list
    }

//    private fun List<TAGError>.withConsecutiveSyntaxErrorsJoined(): List<TAGError> {
//        val list = mutableListOf<TAGError>()
//        var previousTAGSyntaxError: TAGSyntaxError? = null
//        for (error in this) {
//            when (error) {
//                is TAGSyntaxError -> {
//                    if (previousTAGSyntaxError == null) {
//                        previousTAGSyntaxError = error
//                    } else {
//                        if (previousTAGSyntaxError.range.endPosition == error.range.startPosition) {
//                            previousTAGSyntaxError.joinWith(error)
//                        }
//                    }
//                }
//                else -> {
//                    if (previousTAGSyntaxError != null) {
//                        list += previousTAGSyntaxError
//                        previousTAGSyntaxError = null
//                    }
//                    list += error
//                }
//            }
//        }
//        return list
//    }

    abstract class TAGError(var message: String)

    class TAGAmbiguityError(message: String) : TAGError(message)

    class TAGAttemptingFullContextError(message: String) : TAGError(message)

    class TAGContextSensitivityError(message: String) : TAGError(message)

    abstract class RangedTAGError(var range: Range, message: String) : TAGError(message)

    class TAGSyntaxError(range: Range, message: String) : RangedTAGError(range, message)

    private fun syntaxErrorRange(line: Int, character: Int, message: String): Range {
        val startQuoteIndex = message.indexOf('\'')
        val endQuoteIndex = message.indexOf('\'', startQuoteIndex + 1)
        val tokenLength = endQuoteIndex - startQuoteIndex
        return Range(Position(line, character + 1), Position(line, character + tokenLength))
    }

    class CustomError(
            startPos: Position,
            endPos: Position,
            message: String
    ) : RangedTAGError(Range(startPos, endPos), message) {
        constructor(range: Range, message: String) :
                this(range.startPosition, range.endPosition, message)

        override fun toString(): String = "CustomError{range=$range, message=$message}"
    }

    class CustomWarning(
            startPos: Position,
            endPos: Position,
            message: String
    ) : RangedTAGError(Range(startPos, endPos), message) {
        constructor(range: Range, message: String) :
                this(range.startPosition, range.endPosition, message)

        override fun toString(): String = "CustomWarning{range=$range, message=$message}"
    }

}

