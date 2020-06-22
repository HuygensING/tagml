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

data class TAGOntology(
        val root: String,
        val elements: List<ElementDefinition>,
        val attributes: List<AttributeDefinition>,
        val rules: List<String>
)

fun TAGOntology.elementDefinition(qName: String): ElementDefinition? =
        elements.find { it.name == qName }

fun TAGOntology.hasElement(qName: String): Boolean = elements.map { it.name }.contains(qName)

