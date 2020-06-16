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
import nl.knaw.huc.di.tag.tagml.ErrorListener.*
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
        var ontology: TAGOntology? = null
        val openMarkup: MutableList<String> = mutableListOf()
    }

    val tokens: List<TAGMLTokens.TAGMLToken>
        get() = _tokens.toList()

    override fun exitHeader(ctx: TAGMLParser.HeaderContext) {
        val headerMap: Map<String, Any> = parseHeader(ctx)
        val token = HeaderToken(ctx.getRange(), ctx.text, headerMap)
        when (val ontologyParseResult = headerMap[":ontology"]) {
            null -> addError(ctx, """Field ":ontology" missing in header.""")
            is Either<*, *> -> ontologyParseResult.fold(
                    { errorListener.addErrors(it as List<TAGError>) },
                    { context.ontology = it as TAGOntology }
            )
        }
        _tokens += token
    }

    private fun parseHeader(ctx: TAGMLParser.HeaderContext): Map<String, Any> =
            ctx.json_pair().map { toPair(it) }
                    .toMap()

    private fun toPair(ctx: TAGMLParser.Json_pairContext): Pair<String, Any> {
        val key = ctx.JSON_STRING().text.trim('"')
        val value = when (key) {
            ":ontology" -> parseOntology(ctx.json_value())
            else -> ctx.json_value().text.trim('"')
        }
        return (key to value)
    }

    private fun parseOntology(jsonValueCtx: TAGMLParser.Json_valueContext): Either<List<TAGError>, TAGOntology> {
        val errors: MutableList<TAGError> = mutableListOf()
        var root: String? = ""
        val elements: MutableList<String> = mutableListOf()
        val attributes: MutableList<String> = mutableListOf()
        val rules: MutableList<String> = mutableListOf()
        for (pair in jsonValueCtx.json_obj().json_pair()) {
            val key = pair.JSON_STRING().text.trim('"')
            when (key) {
                "root" -> {
                    root = pair.json_value().text.trim('"')
                }
                "elements" -> {
                    elements.addAll(pair.json_value().json_obj().json_pair().map { it.text })
                }
                "attributes" -> {
                    attributes.addAll(pair.json_value().json_obj().json_pair().map { it.text })
                }
                "rules" -> {
                    rules.addAll(pair.json_value().json_arr().json_value().map { it.text })
                }
                else -> errors.add(CustomError(jsonValueCtx.getRange(), "Unexpected key $key"))
            }
        }
        if (root == null) {
            errors.add(CustomError(jsonValueCtx.getRange(), """Field "root" missing in ontology header."""))
        }
        return if (errors.isEmpty()) {
            Either.right(TAGOntology(root!!, elements, attributes, rules))
        } else {
            Either.left(errors.toList())
        }

    }

    override fun exitStartTag(ctx: TAGMLParser.StartTagContext) {
        val qName = ctx.markupName().text
        val expectedRoot = context.ontology?.root
        if (context.openMarkup.isEmpty() && expectedRoot != null && qName != expectedRoot) {
            addError(ctx, """Root element "$qName" does not match the one defined in the header: "$expectedRoot"""")
        }
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
