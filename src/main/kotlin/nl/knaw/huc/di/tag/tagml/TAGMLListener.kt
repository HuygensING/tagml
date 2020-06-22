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
import arrow.core.Left
import arrow.core.Right
import nl.knaw.huc.di.tag.tagml.ErrorListener.CustomError
import nl.knaw.huc.di.tag.tagml.ErrorListener.TAGError
import nl.knaw.huc.di.tag.tagml.ParserUtils.getRange
import nl.knaw.huc.di.tag.tagml.TAGMLTokens.HeaderToken
import nl.knaw.huc.di.tag.tagml.TAGMLTokens.MarkupCloseToken
import nl.knaw.huc.di.tag.tagml.TAGMLTokens.MarkupOpenToken
import nl.knaw.huc.di.tag.tagml.TAGMLTokens.TextToken
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParserBaseListener
import org.antlr.v4.runtime.ParserRuleContext
import java.lang.String.format

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
            null -> addError(ctx, MISSING_ONTOLOGY_FIELD)
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
        val elements: MutableList<ElementDefinition> = mutableListOf()
        val attributes: MutableList<String> = mutableListOf()
        val rules: MutableList<String> = mutableListOf()
        for (pair in jsonValueCtx.json_obj().json_pair()) {
            when (val key = pair.JSON_STRING().text.content()) {
                "root" -> {
                    root = pair.json_value().text.content()
                }
                "elements" -> {
                    pair.json_value().json_obj().json_pair()
                            .map { parseElementDefinition(it) }
                            .forEach { either ->
                                either.fold(
                                        { errors.addAll(it) },
                                        { elements.add(it) }
                                )
                            }
                }
                "attributes" -> {
                    attributes.addAll(pair.json_value().json_obj().json_pair().map { it.text.content() })
                }
                "rules" -> {
                    rules.addAll(pair.json_value().json_arr().json_value().map { it.text.content() })
                }
                else -> errors.add(CustomError(jsonValueCtx.getRange(), format(UNEXPECTED_KEY, key)))
            }
        }
        if (root == null) {
            errors.add(CustomError(jsonValueCtx.getRange(), MISSING_ONTOLOGY_ROOT))
        }
        return if (errors.isEmpty()) {
            Either.right(TAGOntology(root!!, elements, attributes, rules))
        } else {
            Either.left(errors.toList())
        }

    }

    private fun String.content() = this.trim('"')
    private fun String.isValidName() = this.matches(Regex("[a-z][a-zA-Z_0-9]*"))

    private fun parseElementDefinition(context: TAGMLParser.Json_pairContext): Either<List<TAGError>, ElementDefinition> {
        val name = context.JSON_STRING().text.content()
        var description = ""
        val attributes = mutableListOf<AssignedAttribute>()
        val properties: MutableList<String> = mutableListOf()
        var ref = ""
        val errors: MutableList<TAGError> = mutableListOf()
        context.json_value().json_obj().json_pair().forEach { ctx ->
            when (val elementField = ctx.JSON_STRING().text.content()) {
                "description" -> description = ctx.json_value().text.content()
                "attributes" -> {
                    ctx.json_value().json_arr().json_value()
                            .map { it.text.content() }
                            .map {
                                when {
                                    it.endsWith("!") -> {
                                        val fName = it.replace("!", "")
                                        if (fName.isValidName()) {
                                            attributes.add(RequiredAttribute(fName))
                                        } else {
                                            errors.add(CustomError(ctx.getRange(), "Invalid attribute field name $it"))
                                        }
                                    }
                                    it.isValidName() -> attributes.add(OptionalAttribute(it))
                                    else -> errors.add(CustomError(ctx.getRange(), "Invalid attribute field name $it"))
                                }
                            }

                }
                "properties" -> properties.addAll(ctx.json_value().json_arr().json_value().map { it.text.content() })
                "ref" -> ref = ctx.json_value().text.content()
                else -> errors.add(CustomError(ctx.getRange(), "Unknown element field $elementField"))
            }
        }
        if (description.isEmpty()) {
            errors.add(CustomError(context.getRange(), """Element "$name" is missing a description."""))
        }
        return if (errors.isEmpty()) {
            Right(ElementDefinition(name, description, attributes, properties, ref))
        } else {
            Left(errors)
        }
    }

    override fun exitStartTag(ctx: TAGMLParser.StartTagContext) {
        val qName = ctx.markupName().text
        val expectedRoot = context.ontology?.root
        if (context.openMarkup.isEmpty() && expectedRoot != null && qName != expectedRoot) {
            addError(ctx, UNEXPECTED_ROOT, qName, expectedRoot)
        }
        val ontology = context.ontology
        if (ontology != null && !ontology.hasElement(qName)) {
            addWarning(ctx, UNDEFINED_ELEMENT, qName)
        }
        context.openMarkup += qName
        val annotationContexts = ctx.annotation()
        for (actx in annotationContexts) {
            when (actx) {
                is TAGMLParser.BasicAnnotationContext -> {
                    val attributeName = actx.annotationName().text
                    if (ontology != null && ontology.hasElement(qName) && !ontology.elementDefinition(qName)!!.hasAttribute(attributeName)) {
                        addWarning(ctx, UNDEFINED_ATTRIBUTE, attributeName, qName)
                    }
                }

                is TAGMLParser.IdentifyingAnnotationContext -> TODO()
                is TAGMLParser.RefAnnotationContext -> TODO()
            }
        }
        val token = MarkupOpenToken(ctx.getRange(), ctx.text, qName)
        _tokens += token
    }

    override fun exitEndTag(ctx: TAGMLParser.EndTagContext) {
        val rawContent = ctx.text
        val qName = ctx.markupName().text
        if (!context.openMarkup.contains(qName)) {
            addError(ctx, MISSING_OPEN_TAG, rawContent)
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

    private fun addWarning(
            ctx: ParserRuleContext, messageTemplate: String, vararg messageArgs: Any) =
            errorListener.addWarning(
                    Position.startOf(ctx), Position.endOf(ctx), messageTemplate, *messageArgs)

    private fun addError(
            ctx: ParserRuleContext, messageTemplate: String, vararg messageArgs: Any) =
            errorListener.addError(
                    Position.startOf(ctx), Position.endOf(ctx), messageTemplate, *messageArgs)

    fun addBreakingError(
            ctx: ParserRuleContext, messageTemplate: String, vararg messageArgs: Any) =
            errorListener.addBreakingError(
                    Position.startOf(ctx), Position.endOf(ctx), messageTemplate, *messageArgs)

}
