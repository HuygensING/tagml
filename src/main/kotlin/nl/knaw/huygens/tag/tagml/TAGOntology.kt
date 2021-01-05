package nl.knaw.huygens.tag.tagml

/*-
 * #%L
 * tagml
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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
        val elementDefinitions: Map<String, ElementDefinition>,
        val attributes: Map<String, AttributeDefinition>,
        val rules: List<OntologyRule>
)

fun TAGOntology.expectedChildrenFor(parent: String): Set<String> =
        rules.asSequence()
                .filterIsInstance<OntologyRule.HierarchyRule>()
                .mapNotNull { it.childMap[parent] }
                .flatten()
                .map { it.element }
                .toSet()

fun TAGOntology.elementDefinition(qName: String): ElementDefinition? =
        elementDefinitions[qName]

fun TAGOntology.hasElement(qName: String): Boolean =
        elementDefinitions.containsKey(qName)

