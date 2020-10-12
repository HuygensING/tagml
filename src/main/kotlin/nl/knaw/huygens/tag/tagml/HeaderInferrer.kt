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
import com.google.gson.GsonBuilder

fun String.inferHeader(): Either<List<ErrorListener.TAGError>, String> {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val entitiesMap = Regex("""->([a-zA-Z0-9]+)""")
            .findAll(this)
            .map { it.groupValues[1] }
            .map { it to mapOf<String, Any>() }
            .toMap()
    val entitiesJson = gson.toJson(entitiesMap)
    val nameSpacesJson = gson.toJson(
            Regex("""\[([a-zA-Z0-9]+):""")
                    .findAll(this)
                    .map { it.groupValues[1] }
                    .map { it to "http://example.org/ns/test" }
                    .toMap()
    )
    val tagml = """
            |[!{
            |    ":namespaces": $nameSpacesJson, 
            |    ":entities": $entitiesJson, 
            |    ":ontology": {
            |        "root" : "dummy_root"
            |    }
            |}!]
            |[dummy_root>$this<dummy_root]
            |""".trimMargin()
    return when (val parseResult = parse(tagml)) {
        is TAGMLParseResult.TAGMLParseSuccess -> {
            val elementDefinitions: MutableMap<String, MutableMap<String, Any>> = mutableMapOf()
            val markupTokens = parseResult.tokens
                    .subList(2, parseResult.tokens.lastIndex)
                    .filterIsInstance<TAGMLToken.MarkupToken>()
            val root = markupTokens[0].qName
            val attributeDataTypeMap: MutableMap<String, AttributeDataType> = mutableMapOf()
            val namespaces: MutableSet<String> = mutableSetOf()
            markupTokens.forEach { mt ->
                if (mt.qName.contains(':')) {
                    namespaces += mt.qName.subSequence(0, mt.qName.indexOf(':')).toString()
                }
                val elementDefinition = elementDefinitions.getOrPut(mt.qName) { mutableMapOf() }
                val elementAttributes: MutableList<String> = ((elementDefinition["attributes"]
                        ?: listOf<String>()) as List<String>).toMutableList()
                val elementProperties: MutableList<String> = ((elementDefinition["properties"]
                        ?: listOf<String>()) as List<String>).toMutableList()
                if (mt is TAGMLToken.MarkupSuspendToken) {
                    elementProperties += "discontinuous"
                } else if (mt is MarkupTokenWithAttributes) {
                    if (mt is TAGMLToken.MarkupMilestoneToken) {
                        elementProperties += "milestone"
                    }
                    mt.attributes.forEach { kv ->
                        elementAttributes += kv.key
                        attributeDataTypeMap[kv.key] = (kv.value as TypedValue).type
                    }
                }
                if (elementDefinition["description"] == null) {
                    elementDefinition["description"] = "..."
                }
                if (elementProperties.isNotEmpty()) {
                    elementDefinition["properties"] = elementProperties
                }
                if (elementAttributes.isNotEmpty()) {
                    elementDefinition["attributes"] = elementAttributes
                }
            }
            val headerMap: Map<String, Any> = listOf(
                    ":entities" to entitiesMap,
                    ":namespaces" to namespaces.map { it to "https://example.org/ns/$it" }.toMap(),
                    ":ontology" to mapOf(
                            "root" to root,
                            "elements" to elementDefinitions,
                            "attributes" to attributeDefinitionMap(attributeDataTypeMap)
                    ))
                    .filter { (_: String, v: Map<String, Any>) -> v.isNotEmpty() }
                    .toMap()
            val header = gson.toJson(headerMap)
            Either.right(headerJson(header))
        }
        is TAGMLParseResult.TAGMLParseFailure -> {
            Either.left(parseResult.errors)
        }
    }
}

private fun attributeDefinitionMap(typeMap: MutableMap<String, AttributeDataType>) =
        typeMap.map { (name, type) ->
            name to mapOf(
                    "description" to "...",
                    "dataType" to type.name
            )
        }.toMap()

private fun headerJson(header: String?) = """
    |[!$header!]
    |""".trimMargin().trim()
