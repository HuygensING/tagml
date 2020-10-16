package nl.knaw.huc.di.tag

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

import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.asciitable.CWC_LongestLine
import de.vandermeer.asciithemes.a7.A7_Grids
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Token
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream

object ANTLRUtils {

    private val LOG: Logger = LoggerFactory.getLogger("ANTLRUtils")

    @JvmStatic
    fun makeTokenTable(lexer: Lexer): String {
        val cwc = CWC_LongestLine()
        val table = AsciiTable()
                .setTextAlignment(TextAlignment.LEFT)
                .apply {
                    renderer.cwc = cwc
                    addRule()
                    addRow("Pos", "Text", "Rule", "Next mode", "Token")
                    addRule()
                }
        var token: Token
        do {
            token = lexer.nextToken()
            //      LOG.info(token.toString());

            if (token.type != Token.EOF) {
                val pos = token.line.toString() + ":" + token.charPositionInLine
                val text = "'" + token.text + "'"
                val rule = lexer.ruleNames[token.type - 1]
                val mode = lexer.modeNames[lexer._mode]
                table.addRow(pos, text, rule, mode, token)
            }
        } while (token.type != Token.EOF)
        table.addRule()
        table.context.grid = A7_Grids.minusBarPlusEquals()
        return table.render()
    }

    fun printTokens(input: String) {
        LOG.info("\nTAGML:\n{}\n", input)
        printTokens(CharStreams.fromString(input))
    }

    @Throws(IOException::class)
    fun printTokens(input: InputStream) {
        printTokens(CharStreams.fromStream(input))
    }

    private fun printTokens(inputStream: CharStream) {
        val lexer = TAGMLLexer(inputStream)
        val table: String = makeTokenTable(lexer)
        LOG.info("\nTokens:\n{}\n", table)
    }

}
