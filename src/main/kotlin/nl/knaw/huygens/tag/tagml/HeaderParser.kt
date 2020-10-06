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

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import nl.knaw.huygens.tag.tagml.AssignedAttribute.OptionalAttribute
import nl.knaw.huygens.tag.tagml.AssignedAttribute.RequiredAttribute
import nl.knaw.huygens.tag.tagml.OntologyRule.*
import nl.knaw.huygens.tag.tagml.grammar.TAGMLParser
import nl.knaw.huygens.tag.tagorl.TAGORLLexer
import nl.knaw.huygens.tag.tagorl.TAGORLParser
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
    val key = ctx.JSON_STRING().text.content()
    val value = when (key) {
        ":ontology" -> parseOntology(ctx.json_value())
        ":namespaces" -> parseNameSpaces(ctx.json_value())
        ":entities" -> parseEntities(ctx.json_value())
        else -> ctx.json_value().text.content()
    }
    return (key to value)
}

private fun parseNameSpaces(jsonValueCtx: TAGMLParser.Json_valueContext): ParseResult<Map<String, String>> {
    val errors: MutableList<ErrorListener.TAGError> = mutableListOf()
    val namespaces: MutableMap<String, String> = mutableMapOf()
    jsonValueCtx.json_obj().json_pair()
            .filterNotNull()
            .forEach { pair ->
                val key = pair.JSON_STRING().text.content()
                val value = pair.json_value().text.content()
                // TODO: value should be url
                namespaces[key] = value
            }
    return if (errors.isEmpty()) {
        Either.right(namespaces)
    } else {
        Either.left(errors.toList())
    }
}

private fun parseEntities(jsonValueCtx: TAGMLParser.Json_valueContext): ParseResult<Map<String, String>> {
    val errors: MutableList<ErrorListener.TAGError> = mutableListOf()
    val entities: MutableMap<String, String> = mutableMapOf()
    jsonValueCtx.json_obj().json_pair()
            .filterNotNull()
            .forEach { pair ->
                val key = pair.JSON_STRING().text.content()
                val value = pair.json_value().text.content()
                // TODO: value should be json map
                entities[key] = value
            }
    return if (errors.isEmpty()) {
        Either.right(entities)
    } else {
        Either.left(errors.toList())
    }
}

private fun parseOntology(jsonValueCtx: TAGMLParser.Json_valueContext): ParseResult<TAGOntology> {
    val errors: MutableList<ErrorListener.TAGError> = mutableListOf()
    var root: String? = ""
    val elementDefinitions: MutableMap<String, ElementDefinition> = mutableMapOf()
    val attributeDefinitions: MutableMap<String, AttributeDefinition> = mutableMapOf()
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
                                            { errorList -> errors += errorList },
                                            { elementDefinition -> elementDefinitions[elementDefinition.name] = elementDefinition })
                                }
                        if (elementDefinitions[root]?.isDiscontinuous == true) {
                            errors += error(pair, DISCONTINUOUS_ROOT, root!!)
                        }
                    }
                    "attributes" -> {
                        pair.json_value().json_obj().json_pair()
                                .filter { it.JSON_STRING() != null }
                                .map { parseAttributeDefinition(it) }
                                .forEach { either ->
                                    either.fold(
                                            { errors += it },
                                            { attributeDefinition -> attributeDefinitions[attributeDefinition.name] = attributeDefinition }
                                    )
                                }
                    }
                    "rules" -> {
                        val definedElements = elementDefinitions.keys
                        pair.json_value().json_arr().json_value()
                                .map { Pair(it, it.text.content()) }
                                .map { (ctx, string) -> Pair(ctx, parseRule(string, definedElements)) }
                                .forEach { (ctx, either) ->
                                    either.fold(
                                            { errorMessage -> errors += errorMessage.map { error(ctx, it, ctx.text) } },
                                            { ontologyRule -> rules += ontologyRule }
                                    )
                                }
                    }
                    else -> errors += error(jsonValueCtx, UNEXPECTED_KEY, key)
                }
            }
    checkMissingRootDefinition(root, errors, jsonValueCtx)
    checkMissingAttributeDefinitions(elementDefinitions, attributeDefinitions, errors, jsonValueCtx)
    return if (errors.isNotEmpty()) {
        Either.left(errors.toList())
    } else {
        Either.right(TAGOntology(root!!, elementDefinitions, attributeDefinitions, rules))
    }
}

private fun checkMissingAttributeDefinitions(
        elementDefinitions: Map<String, ElementDefinition>,
        attributeDefinitions: Map<String, AttributeDefinition>,
        errors: MutableList<ErrorListener.TAGError>,
        jsonValueCtx: TAGMLParser.Json_valueContext
) {
    val elementAttributes = elementDefinitions.values
            .asSequence()
            .map { it.attributes }
            .flatten()
            .distinct()
            .map { it.name }
    val definedAttributes = attributeDefinitions.keys
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

private fun parseElementDefinition(context: TAGMLParser.Json_pairContext): ParseResult<ElementDefinition> {
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

private fun parseAttributeDefinition(context: TAGMLParser.Json_pairContext): ParseResult<AttributeDefinition> {
    val name = context.JSON_STRING().text.content()
    var description = ""
    var dataTypeString = ""
    var ref = ""
    val errors: MutableList<ErrorListener.TAGError> = mutableListOf()
    context.json_value().json_obj().json_pair()
            .filterNotNull()
            .forEach { ctx ->
                when (val attributeField = ctx.JSON_STRING().text.content()) {
                    "description" -> description = ctx.json_value().text.content()
                    "dataType" -> dataTypeString = ctx.json_value().text.content()
                    "ref" -> ref = ctx.json_value().text.content()
                    else -> errors += error(ctx, UNKNOWN_ATTRIBUTE_FIELD, attributeField)
                }
            }
    if (description.isEmpty()) {
        errors += error(context, MISSING_ATTRIBUTE_DESCRIPTION, name)
    }
    var dataType = AttributeDataType.String
    if (dataTypeString.isEmpty()) {
        errors += error(context, MISSING_ATTRIBUTE_DATATYPE, name)
    } else {
        if (dataTypeString in attributeDataTypeNames()) {
            dataType = AttributeDataType.valueOf(dataTypeString)
        } else {
            errors += error(context, UNKNOWN_ATTRIBUTE_DATATYPE, dataTypeString, name)
        }
    }
    return if (errors.isEmpty()) {
        Right(AttributeDefinition(name, description, dataType, ref))
    } else {
        Left(errors)
    }
}

fun parseRule(tagorl: String, definedElements: Set<String>): Either<List<String>, OntologyRule> {
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
        return Left(errorListener.orderedErrors.map { it.message })
    }
    return when (val ruleContext: ParseTree = parser.ontologyRule().getChild(0)) {
        is TAGORLParser.HierarchyRuleContext -> parseHierarchyRule(ruleContext, definedElements)
        is TAGORLParser.SetRuleContext -> parseSetRule(ruleContext, definedElements)
        is TAGORLParser.TripleRuleContext -> parseTripleRule(ruleContext, definedElements)
        is ParserRuleContext -> Left(listOf("unexpected context"))
        else -> error("unexpected ruleContext")
    }
}

fun parseHierarchyRule(ruleContext: TAGORLParser.HierarchyRuleContext, definedElements: Set<String>): Either<List<String>, HierarchyRule> {
    val childMap = mutableMapOf<String, MutableSet<QualifiedElement>>()
//            .withDefault { mutableSetOf() }
    when (ruleContext) {
        is TAGORLParser.MonoLevelContext -> {
            val parent = ruleContext.Name().text
            val childElements = ruleContext.childElements()
            parseChildElementsContext(childElements, childMap, parent)
        }
        is TAGORLParser.MultiLevelContext -> {
            var parent = ruleContext.Name().text
            for (childCtx in ruleContext.childElement()) {
                val qualifiedChild = toQualifiedElement(childCtx)
                childMap.getOrPut(parent) { mutableSetOf() } += qualifiedChild
                parent = qualifiedChild.element
            }
            val ruleTail = ruleContext.childElements()
            parseChildElementsContext(ruleTail, childMap, parent)
        }
        else -> TODO("this should not happen, has TAGORL.g4 changed?")
    }

    val elements = childMap.values.flatten().map { it.element }.toSet()
    val undefinedElements = elements - definedElements
    return if (undefinedElements.isEmpty()) {
        Right(HierarchyRule(ruleContext.text, childMap))
    } else {
        Left(listOf(format(UNDEFINED_RULE_ELEMENT, "%s", undefinedElements.joinToString())))
    }
}

private fun parseChildElementsContext(ctx: TAGORLParser.ChildElementsContext, childMap: MutableMap<String, MutableSet<QualifiedElement>>, parent: String) {
    when (ctx) {
        is TAGORLParser.SingleChildContext -> childMap.getOrPut(parent) { mutableSetOf() } += toQualifiedElement(ctx.childElement())
        is TAGORLParser.ChildrenListContext -> ctx.childElement().forEach { childMap.getOrPut(parent) { mutableSetOf() } += toQualifiedElement(it) }
        is TAGORLParser.ChildrenGroupContext -> ctx.childElement().forEach { childMap.getOrPut(parent) { mutableSetOf() } += toQualifiedElement(it) }
        is TAGORLParser.OptionalChildrenGroupContext -> ctx.childElement().forEach { childMap.getOrPut(parent) { mutableSetOf() } += toQualifiedElement(it) }
        is TAGORLParser.ZeroOrMoreChildrenGroupContext -> ctx.childElement().forEach { childMap.getOrPut(parent) { mutableSetOf() } += toQualifiedElement(it) }
        is TAGORLParser.OneOrMoreChildrenGroupContext -> ctx.childElement().forEach { childMap.getOrPut(parent) { mutableSetOf() } += toQualifiedElement(it) }
        else -> error("unexpected type: ${ctx.text} ${ctx.javaClass}")
    }
}

fun toQualifiedElement(childCtx: TAGORLParser.ChildElementContext): QualifiedElement =
        when (childCtx) {
            is TAGORLParser.OneChildContext -> QualifiedElement.SingleElement(childCtx.Name().text)
            is TAGORLParser.OptionalChildContext -> QualifiedElement.OptionalElement(childCtx.Name().text)
            is TAGORLParser.ZeroOrMoreChildContext -> QualifiedElement.ZeroOrMoreElement(childCtx.Name().text)
            is TAGORLParser.OneOrMoreChildContext -> QualifiedElement.OneOrMoreElement(childCtx.Name().text)
            else -> TODO("this should not happen, has TAGORL.g4 changed?")
        }

fun parseSetRule(ruleContext: TAGORLParser.SetRuleContext, definedElements: Set<String>): Either<List<String>, SetRule> {
    val elements = ruleContext.childElement().map { it.text }
    val undefinedElements = elements - definedElements
    return if (undefinedElements.isEmpty()) {
        Right(SetRule(
                raw = ruleContext.text,
                setName = ruleContext.Name().text,
                elements = elements
        ))
    } else {
        Left(listOf(format(UNDEFINED_RULE_ELEMENT, "%s", undefinedElements.joinToString())))
    }
}

fun parseTripleRule(ruleContext: TAGORLParser.TripleRuleContext, definedElements: Set<String>): Either<List<String>, TripleRule> {
    val subject = ruleContext.subject().text
    val objects = ruleContext.`object`().children.map { it.text }
    val undefinedElements = listOf(subject) + objects - definedElements
    return if (undefinedElements.isEmpty()) {
        Right(TripleRule(
                raw = ruleContext.text,
                subject = subject,
                predicate = ruleContext.predicate().text,
                objects = objects
        ))
    } else {
        Left(listOf(format(UNDEFINED_RULE_ELEMENT, "%s", undefinedElements.joinToString())))
    }
}

private fun error(ctx: ParserRuleContext, messageTemplate: String, vararg messageArgs: Any) =
        ErrorListener.CustomError(ctx.getRange(), format(messageTemplate, *messageArgs))
