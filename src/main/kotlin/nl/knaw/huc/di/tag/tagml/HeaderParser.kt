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
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser
import org.antlr.v4.runtime.ParserRuleContext

fun parseHeader(ctx: TAGMLParser.HeaderContext): Map<String, Any> =
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

private fun parseOntology(jsonValueCtx: TAGMLParser.Json_valueContext): Either<List<ErrorListener.TAGError>, TAGOntology> {
    val errors: MutableList<ErrorListener.TAGError> = mutableListOf()
    var root: String? = ""
    val elements: MutableList<ElementDefinition> = mutableListOf()
    val attributes: MutableList<AttributeDefinition> = mutableListOf()
    val rules: MutableList<String> = mutableListOf()
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
                                            { errors.addAll(it) },
                                            { elements.add(it) }
                                    )
                                }
                    }
                    "attributes" -> {
                        pair.json_value().json_obj().json_pair()
                                .filter { it.JSON_STRING() != null }
                                .map { parseAttributeDefinition(it) }
                                .forEach { either ->
                                    either.fold(
                                            { errors.addAll(it) },
                                            { attributes.add(it) }
                                    )
                                }
                    }
                    "rules" -> {
                        rules.addAll(pair.json_value().json_arr().json_value().map { it.text.content() })
                    }
                    else -> errors.add(error(jsonValueCtx, UNEXPECTED_KEY, key))
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
        errors.add(error(jsonValueCtx, USED_UNDEFINED_ATTRIBUTE, it))
    }
}

private fun checkMissingRootDefinition(root: String?, errors: MutableList<ErrorListener.TAGError>, jsonValueCtx: TAGMLParser.Json_valueContext) {
    if (root == null) {
        errors.add(error(jsonValueCtx, MISSING_ONTOLOGY_ROOT))
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
                                                attributes.add(RequiredAttribute(fName))
                                            } else {
                                                errors.add(error(ctx, "Invalid attribute field name $it"))
                                            }
                                        }
                                        it.isValidName() -> attributes.add(OptionalAttribute(it))
                                        else -> errors.add(error(ctx, "Invalid attribute field name $it"))
                                    }
                                }

                    }
                    "properties" -> properties.addAll(ctx.json_value().json_arr().json_value().map { it.text.content() })
                    "ref" -> ref = ctx.json_value().text.content()
                    else -> errors.add(error(ctx, UNKNOWN_ELEMENT_FIELD, elementField))
                }
            }
    if (description.isEmpty()) {
        errors.add(error(context, MISSING_ELEMENT_DESCRIPTION, name))
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
                    else -> errors.add(error(ctx, UNKNOWN_ATTRIBUTE_FIELD, attributeField))
                }
            }
    if (description.isEmpty()) {
        errors.add(error(context, MISSING_ATTRIBUTE_DESCRIPTION, name))
    }
    if (dataType.isEmpty()) {
        errors.add(error(context, MISSING_ATTRIBUTE_DATATYPE, name))
    }
    return if (errors.isEmpty()) {
        Right(AttributeDefinition(name, description, dataType, ref))
    } else {
        Left(errors)
    }
}

private fun error(ctx: ParserRuleContext, messageTemplate: String, vararg messageArgs: Any) =
        ErrorListener.CustomError(ctx.getRange(), java.lang.String.format(messageTemplate, *messageArgs))
