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

import arrow.core.Either
import nl.knaw.huc.di.tag.tagml.TAGMLParseResult.TAGMLParseFailure
import nl.knaw.huc.di.tag.tagml.TAGMLParseResult.TAGMLParseSuccess
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeWalker

typealias ParseResult<T> = Either<List<ErrorListener.TAGError>, T>

sealed class TAGMLParseResult(val warnings: List<ErrorListener.TAGError>) {
    class TAGMLParseSuccess(val tokens: List<TAGMLToken>, warnings: List<ErrorListener.TAGError>) : TAGMLParseResult(warnings)
    class TAGMLParseFailure(val errors: List<ErrorListener.TAGError>, warnings: List<ErrorListener.TAGError>) : TAGMLParseResult(warnings)
}

fun ParserRuleContext.getRange(): Range =
        Range(Position.startOf(this), Position.endOf(this))

fun parse(tagml: String): TAGMLParseResult {
    val antlrInputStream: CharStream = CharStreams.fromString(tagml)
    val errorListener = ErrorListener()
    val lexer = TAGMLLexer(antlrInputStream)
            .apply { addErrorListener(errorListener) }
    val tokens = CommonTokenStream(lexer)
    val parser = TAGMLParser(tokens)
            .apply {
                addErrorListener(errorListener)
                buildParseTree = true
            }
    val parseTree: ParseTree = parser.document()
    //    LOG.debug("parsetree: {}", parseTree.toStringTree(parser));
    val listener = TAGMLListener(errorListener)
    ParseTreeWalker.DEFAULT.walk(listener, parseTree)

    return if (errorListener.hasErrors) {
        TAGMLParseFailure(errorListener.orderedErrors, errorListener.orderedWarnings)
    } else {
        TAGMLParseSuccess(listener.tokens, errorListener.orderedWarnings)
    }

}

