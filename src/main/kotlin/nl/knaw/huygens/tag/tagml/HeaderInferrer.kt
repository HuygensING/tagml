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
package nl.knaw.huygens.tag.tagml

import arrow.core.Either
import com.google.gson.GsonBuilder

fun String.inferHeader(): Either<List<ErrorListener.TAGError>, String> {
    val tagml = """
            |[!{
            |    ":ontology": {
            |        "root" : "dummy_root"
            |    }
            |}!]
            |[dummy_root>$this<dummy_root]
            |""".trimMargin()
    return when (val parseResult = parse(tagml)) {
        is TAGMLParseResult.TAGMLParseSuccess -> {
            val elementsUsed = parseResult.tokens
                    .subList(2, parseResult.tokens.lastIndex)
                    .filterIsInstance<TAGMLToken.MarkupToken>()
                    .map { it.qName }
                    .distinct()
            val headerMap: MutableMap<String, Any> = mutableMapOf()
            val minimalElementDefinition = mapOf("description" to "...")
            headerMap[":ontology"] = mapOf<String, Any>(
                    "root" to elementsUsed[0],
                    "elements" to elementsUsed.map { it to minimalElementDefinition }.toMap()
            )
            val gson = GsonBuilder().setPrettyPrinting().create()
            val header = gson.toJson(headerMap)
            Either.right(headerJson(header))
        }
        is TAGMLParseResult.TAGMLParseFailure -> {
            Either.left(parseResult.errors)
        }
    }
}

private fun headerJson(header: String?) = """
    |[!$header!]
    |""".trimMargin().trim()
