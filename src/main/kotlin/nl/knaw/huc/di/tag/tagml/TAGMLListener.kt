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

import nl.knaw.huc.di.tag.tagml.ParserUtils.getRange
import nl.knaw.huc.di.tag.tagml.TAGMLTokens.HeaderToken
import nl.knaw.huc.di.tag.tagml.TAGMLTokens.MarkupCloseToken
import nl.knaw.huc.di.tag.tagml.TAGMLTokens.MarkupOpenToken
import nl.knaw.huc.di.tag.tagml.TAGMLTokens.TextToken
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParserBaseListener
import org.antlr.v4.runtime.ParserRuleContext

class TAGMLListener(private val errorListener: ErrorListener) : TAGMLParserBaseListener() {

    private val _tokens: MutableList<TAGMLTokens.TAGMLToken> = mutableListOf()
    private val context = ListenerContext()

    class ListenerContext {
        val openMarkup: MutableList<String> = mutableListOf()
    }

    val tokens: List<TAGMLTokens.TAGMLToken>
        get() = _tokens.toList()

    override fun exitHeader(ctx: TAGMLParser.HeaderContext) {
        val token = HeaderToken(ctx.getRange(), ctx.text)
        _tokens += token
    }

    override fun exitStartTag(ctx: TAGMLParser.StartTagContext) {
        val qName = ctx.markupName().text
        context.openMarkup += qName
        val token = MarkupOpenToken(ctx.getRange(), ctx.text, qName)
        _tokens += token
    }

    override fun exitEndTag(ctx: TAGMLParser.EndTagContext) {
        val rawContent = ctx.text
        val qName = ctx.markupName().text
        if (!context.openMarkup.contains(qName)) {
            addError(ctx, "Closing tag found without corresponding open tag: $rawContent")
        } else {
            context.openMarkup.remove(qName)
        }
        val token = MarkupCloseToken(ctx.getRange(), rawContent, qName)
        _tokens += token
    }

    override fun exitText(ctx: TAGMLParser.TextContext) {
        val token = TextToken(ctx.getRange(), ctx.text)
        _tokens += token
    }

    private fun addError(
            ctx: ParserRuleContext, messageTemplate: String, vararg messageArgs: Any) {
        errorListener.addError(
                Position.startOf(ctx), Position.endOf(ctx), messageTemplate, messageArgs)
    }

    fun addBreakingError(
            ctx: ParserRuleContext, messageTemplate: String, vararg messageArgs: Any) {
        errorListener.addBreakingError(
                Position.startOf(ctx), Position.endOf(ctx), messageTemplate, messageArgs)
    }

}
