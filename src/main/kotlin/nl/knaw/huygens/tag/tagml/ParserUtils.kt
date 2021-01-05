package nl.knaw.huygens.tag.tagml

/*-
 * #%L
 * tagml
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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
import nl.knaw.huygens.tag.tagml.TAGMLParseResult.TAGMLParseFailure
import nl.knaw.huygens.tag.tagml.TAGMLParseResult.TAGMLParseSuccess
import nl.knaw.huygens.tag.tagml.grammar.TAGMLLexer
import nl.knaw.huygens.tag.tagml.grammar.TAGMLParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.io.File
import java.nio.file.Path

typealias ParseResult<T> = Either<List<ErrorListener.TAGError>, T>

sealed class TAGMLParseResult(val warnings: List<ErrorListener.TAGError>) {
    class TAGMLParseSuccess(val tokens: List<TAGMLToken>, warnings: List<ErrorListener.TAGError>) : TAGMLParseResult(warnings)
    class TAGMLParseFailure(val errors: List<ErrorListener.TAGError>, warnings: List<ErrorListener.TAGError>) : TAGMLParseResult(warnings)
}

fun ParserRuleContext.getRange(): Range =
        Range(Position.startOf(this), Position.endOf(this))

fun parse(tagmlPath: Path): TAGMLParseResult =
        parse(CharStreams.fromPath(tagmlPath))

fun parse(tagmlFile: File): TAGMLParseResult =
        parse(tagmlFile.toPath())

fun parse(tagml: String): TAGMLParseResult =
        parse(CharStreams.fromString(tagml))

private fun parse(antlrInputStream: CharStream): TAGMLParseResult {
    val errorListener = ErrorListener()
    val parseTree: ParseTree = antlrInputStream
            .getLexerWith(errorListener)
            .commonTokenStream()
            .getParserWith(errorListener)
            .document()
    return if (errorListener.hasErrors) {
        TAGMLParseFailure(errorListener.orderedErrors, errorListener.orderedWarnings)
    } else {
        val validator = TAGMLListener(errorListener)
        parseTree.walkWith(validator)
        if (errorListener.hasErrors) {
            TAGMLParseFailure(errorListener.orderedErrors, errorListener.orderedWarnings)
        } else {
            TAGMLParseSuccess(validator.tokens, errorListener.orderedWarnings)
        }
    }
}

private fun CharStream.getLexerWith(errorListener: ErrorListener): TAGMLLexer =
        TAGMLLexer(this)
                .apply { addErrorListener(errorListener) }

private fun TAGMLLexer.commonTokenStream(): CommonTokenStream = CommonTokenStream(this)

private fun CommonTokenStream.getParserWith(errorListener: ErrorListener): TAGMLParser =
        TAGMLParser(this)
                .apply {
                    addErrorListener(errorListener)
                    buildParseTree = true
                }

private fun ParseTree.walkWith(listener: TAGMLListener) {
    ParseTreeWalker.DEFAULT.walk(listener, this)
}


