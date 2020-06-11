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
package nl.knaw.huc.di.tag.tagml

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

    private val reportAmbiguity = false
    private val reportAttemptingFullContext = false
    private val reportContextSensitivity = true

    private var hasBreakingError = false

    class TAGErrorComparator() : Comparator<TAGError> {
        override fun compare(e1: TAGError, e2: TAGError): Int = when {
            (e1 is CustomError && e2 is CustomError) -> when {
                e1.range.startPosition.line != e2.range.startPosition.line -> e1.range.startPosition.line.compareTo(e2.range.startPosition.line)
                e1.range.startPosition.character != e2.range.startPosition.character -> e1.range.startPosition.character.compareTo(e2.range.startPosition.character)
                e1.range.endPosition.line != e2.range.endPosition.line -> e1.range.endPosition.line.compareTo(e2.range.endPosition.line)
                e1.range.endPosition.character != e2.range.endPosition.character -> e1.range.endPosition.character.compareTo(e2.range.endPosition.character)
                else -> e1.message.compareTo(e2.message)
            }
            (e1 is TAGSyntaxError && e2 is TAGSyntaxError) -> when {
                e1.position.line != e2.position.line -> e1.position.line.compareTo(e2.position.line)
                e1.position.character != e2.position.character -> e1.position.character.compareTo(e2.position.character)
                else -> e1.message.compareTo(e2.message)
            }
            (e1 is CustomError && e2 is TAGSyntaxError) -> when {
                e1.range.startPosition.line != e2.position.line -> e1.range.startPosition.line.compareTo(e2.position.line)
                else -> e1.range.startPosition.character.compareTo(e2.position.character)
            }
            (e1 is TAGSyntaxError && e2 is CustomError) -> when {
                e1.position.line != e2.range.startPosition.line -> e1.position.line.compareTo(e2.range.startPosition.line)
                else -> e1.position.character.compareTo(e2.range.startPosition.character)
            }
            else -> e1.message.compareTo(e2.message)
        }
    }

    val orderedErrors: List<TAGError>
        get() = errors.sortedWith(TAGErrorComparator())

    val errorMessages: List<String>
        get() = orderedErrors.map { it.message }

    val hasErrors: Boolean
        get() = errors.isNotEmpty()

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
            errors.add(TAGAmbiguityError(message))
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
            errors.add(TAGAttemptingFullContextError(message))
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
            errors.add(TAGContextSensitivityError(message))
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
        errors.add(TAGSyntaxError(message, line, charPositionInLine))
    }

    val prefixedErrorMessagesAsString: String
        get() = orderedErrors
                .map(this::prefixedErrorMessage)
                .joinToString { "\n" }

    private fun prefixedErrorMessage(error: TAGError): String {
        if (error is CustomError) {
            return prefix(error.range.startPosition) + error.message
        }
        return if (error is TAGSyntaxError) {
            prefix(error.position) + error.message
        } else ""
    }

    private fun prefix(position: Position): String =
            format("line %d:%d : ", position.line, position.character)

    fun addError(
            startPos: Position, endPos: Position, messageTemplate: String, vararg messageArgs: Any) {
        errors.add(CustomError(startPos, endPos, String.format(messageTemplate, *messageArgs)))
    }

    fun addBreakingError(
            startPos: Position, endPos: Position, messageTemplate: String, vararg messageArgs: Any) {
        addError(startPos, endPos, messageTemplate, *messageArgs)
        abortParsing(messageTemplate, *messageArgs)
    }

    private fun abortParsing(messageTemplate: String, vararg messageArgs: Any) {
        hasBreakingError = true
        throw TAGMLBreakingError("""
                ${String.format(messageTemplate, *messageArgs)}
                parsing aborted!
                """.trimIndent())
    }

    abstract class TAGError(val message: String)

    class TAGSyntaxError(message: String, line: Int, character: Int) : TAGError(message) {
        val position: Position = Position(line, character)
    }

    class TAGAmbiguityError(message: String) : TAGError(message)

    class TAGAttemptingFullContextError(message: String) : TAGError(message)

    class TAGContextSensitivityError(message: String) : TAGError(message)

    class CustomError(startPos: Position, endPos: Position, message: String) : TAGError(message) {
        val range = Range(startPos, endPos)

        override fun toString(): String = "CustomError{range=$range, message=$message}"
    }

}
