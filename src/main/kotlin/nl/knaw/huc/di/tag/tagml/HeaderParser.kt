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
import nl.knaw.huc.di.tag.tagml.AssignedAttribute.OptionalAttribute
import nl.knaw.huc.di.tag.tagml.AssignedAttribute.RequiredAttribute
import nl.knaw.huc.di.tag.tagml.OntologyRule.*
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser
import nl.knaw.huc.di.tag.tagorl.TAGORLLexer
import nl.knaw.huc.di.tag.tagorl.TAGORLParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import java.lang.String.format

fun parseHeader(ctx: TAGMLParser.HeaderContext): Map<String, Any> =
        ctx.json_pair().map { toPair(it) }
                .toMap()

@Suppress("IMPLICIT_CAST_TO_ANY")
private fun toPair(ctx: TAGMLParser.Json_pairContext): Pair<String, Any> {
    val key = ctx.JSON_STRING().text.trim('"')
    val value = when (key) {
        ":ontology" -> parseOntology(ctx.json_value())
        else -> ctx.json_value().text.trim('"')
    }
    return (key to value)
}

private fun parseOntology(jsonValueCtx: TAGMLParser.Json_valueContext): Either<List<ErrorListener.TAGError>, TAGOntology> {
    val errors: MutableList<ErrorListener.TAGError> = mutableListOf()
    var root: String? = ""
    val elements: MutableList<ElementDefinition> = mutableListOf()
    val attributes: MutableList<AttributeDefinition> = mutableListOf()
    val rules: MutableList<OntologyRule> = mutableListOf()
    jsonValueCtx.json_obj().json_pair()
            .filterNotNull()
            .forEach { pair ->
                when (val key = pair.JSON_STRING().text.content()) {
                    "root" -> {
                        root = pair.json_value().text.content()
                    }
                    "elements" -> {
                        pair.json_value().json_obj().json_pair()
                                .filter { it.JSON_STRING() != null }
                                .map { parseElementDefinition(it) }
                                .forEach { either ->
                                    either.fold(
                                            { errors += it },
                                            { elements += it }
                                    )
                                }
                    }
                    "attributes" -> {
                        pair.json_value().json_obj().json_pair()
                                .filter { it.JSON_STRING() != null }
                                .map { parseAttributeDefinition(it) }
                                .forEach { either ->
                                    either.fold(
                                            { errors += it },
                                            { attributes += it }
                                    )
                                }
                    }
                    "rules" -> {
                        pair.json_value().json_arr().json_value()
                                .map { it.text.content() }
                                .map { parseRule(it) }
                                .forEach { either ->
                                    either.fold(
                                            { errors += it },
                                            { rules += it }
                                    )
                                }

                    }
                    else -> errors += error(jsonValueCtx, UNEXPECTED_KEY, key)
                }
            }
    checkMissingRootDefinition(root, errors, jsonValueCtx)
    checkMissingAttributeDefinitions(elements, attributes, errors, jsonValueCtx)
    return if (errors.isEmpty()) {
        Either.right(TAGOntology(root!!, elements, attributes, rules))
    } else {
        Either.left(errors.toList())
    }
}

private fun checkMissingAttributeDefinitions(
        elements: MutableList<ElementDefinition>,
        attributes: MutableList<AttributeDefinition>,
        errors: MutableList<ErrorListener.TAGError>,
        jsonValueCtx: TAGMLParser.Json_valueContext
) {
    val elementAttributes = elements.map { it.attributes }.flatten().distinct().map { it.name }
    val definedAttributes = attributes.map { it.name }
    val usedButUndefinedAttributes = elementAttributes - definedAttributes
    usedButUndefinedAttributes.forEach {
        errors += error(jsonValueCtx, USED_UNDEFINED_ATTRIBUTE, it)
    }
}

private fun checkMissingRootDefinition(root: String?, errors: MutableList<ErrorListener.TAGError>, jsonValueCtx: TAGMLParser.Json_valueContext) {
    if (root == null) {
        errors += error(jsonValueCtx, MISSING_ONTOLOGY_ROOT)
    }
}

private fun String.content() = this.trim('"')
private fun String.isValidName() = this.matches(Regex("[a-z][a-zA-Z_0-9]*"))

private fun parseElementDefinition(context: TAGMLParser.Json_pairContext): Either<List<ErrorListener.TAGError>, ElementDefinition> {
    val name = context.JSON_STRING().text.content()
    var description = ""
    val attributes = mutableListOf<AssignedAttribute>()
    val properties: MutableList<String> = mutableListOf()
    var ref = ""
    val errors: MutableList<ErrorListener.TAGError> = mutableListOf()
    context.json_value().json_obj().json_pair()
            .filterNotNull()
            .forEach { ctx ->
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
                                                attributes += RequiredAttribute(fName)
                                            } else {
                                                errors += error(ctx, "Invalid attribute field name $it")
                                            }
                                        }
                                        it.isValidName() -> attributes += OptionalAttribute(it)
                                        else -> errors += error(ctx, "Invalid attribute field name $it")
                                    }
                                }

                    }
                    "properties" -> properties += ctx.json_value().json_arr().json_value().map { it.text.content() }
                    "ref" -> ref = ctx.json_value().text.content()
                    else -> errors += error(ctx, UNKNOWN_ELEMENT_FIELD, elementField)
                }
            }
    if (description.isEmpty()) {
        errors += error(context, MISSING_ELEMENT_DESCRIPTION, name)
    }
    return if (errors.isEmpty()) {
        Right(ElementDefinition(name, description, attributes, properties, ref))
    } else {
        Left(errors)
    }
}

private fun parseAttributeDefinition(context: TAGMLParser.Json_pairContext): Either<List<ErrorListener.TAGError>, AttributeDefinition> {
    val name = context.JSON_STRING().text.content()
    var description = ""
    var dataType = ""
    var ref = ""
    val errors: MutableList<ErrorListener.TAGError> = mutableListOf()
    context.json_value().json_obj().json_pair()
            .filterNotNull()
            .forEach { ctx ->
                when (val attributeField = ctx.JSON_STRING().text.content()) {
                    "description" -> description = ctx.json_value().text.content()
                    "dataType" -> dataType = ctx.json_value().text.content()
                    "ref" -> ref = ctx.json_value().text.content()
                    else -> errors += error(ctx, UNKNOWN_ATTRIBUTE_FIELD, attributeField)
                }
            }
    if (description.isEmpty()) {
        errors += error(context, MISSING_ATTRIBUTE_DESCRIPTION, name)
    }
    if (dataType.isEmpty()) {
        errors += error(context, MISSING_ATTRIBUTE_DATATYPE, name)
    }
    return if (errors.isEmpty()) {
        Right(AttributeDefinition(name, description, dataType, ref))
    } else {
        Left(errors)
    }
}

private fun parseRule(tagorl: String): Either<List<ErrorListener.TAGError>, OntologyRule> {
    val antlrInputStream: CharStream = CharStreams.fromString(tagorl)
    val errorListener = ErrorListener()
    val lexer = TAGORLLexer(antlrInputStream).apply {
        addErrorListener(errorListener)
    }
    val tokens = CommonTokenStream(lexer)
    val parser = TAGORLParser(tokens).apply {
        addErrorListener(errorListener)
        buildParseTree = true
    }
    if (errorListener.hasErrors) {
        return Left(errorListener.orderedErrors)
    }
    return when (val ruleContext: ParseTree = parser.ontologyRule().getChild(0)) {
        is TAGORLParser.HierarchyRuleContext -> parseHierarchyRule(ruleContext)
        is TAGORLParser.SetRuleContext -> parseSetRule(ruleContext)
        is TAGORLParser.TripleRuleContext -> parseTripleRule(ruleContext)
        is ParserRuleContext -> Left(listOf(ErrorListener.CustomError(Position.startOf(ruleContext), Position.endOf(ruleContext), "unexpected context")))
        else -> error("unexpected ruleContext")
    }
}

fun parseHierarchyRule(ruleContext: TAGORLParser.HierarchyRuleContext): Either<List<ErrorListener.TAGError>, HierarchyRule> {
    return Right(HierarchyRule(ruleContext.text))
}

fun parseSetRule(ruleContext: TAGORLParser.SetRuleContext): Either<List<ErrorListener.TAGError>, SetRule> =
        Right(SetRule(
                raw = ruleContext.text,
                setName = ruleContext.Name().text,
                elements = ruleContext.child().map { it.text }
        ))

fun parseTripleRule(ruleContext: TAGORLParser.TripleRuleContext): Either<List<ErrorListener.TAGError>, TripleRule> {
    return Right(TripleRule(
            raw = ruleContext.text,
            subject = ruleContext.subject().text,
            predicate = ruleContext.predicate().text,
            objects = ruleContext.`object`().children.map { it.text }
    ))
}

private fun error(ctx: ParserRuleContext, messageTemplate: String, vararg messageArgs: Any) =
        ErrorListener.CustomError(ctx.getRange(), format(messageTemplate, *messageArgs))
