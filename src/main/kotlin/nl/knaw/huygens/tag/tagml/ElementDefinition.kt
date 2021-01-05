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

import nl.knaw.huygens.tag.tagml.AssignedAttribute.RequiredAttribute

data class ElementDefinition(
        val name: String,
        val description: String = "",
        val attributes: List<AssignedAttribute> = listOf(),
        val properties: List<String> = listOf(),
        val ref: String = ""
) {
    private val attributeNames: List<String> by lazy { attributes.map { it.name } }

    fun hasAttribute(attributeName: String): Boolean =
            attributeName in attributeNames

    val requiredAttributes: List<String> by lazy { attributes.filterIsInstance<RequiredAttribute>().map { it.name } }

    val isDiscontinuous: Boolean = hasProperty("discontinuous")
    val isMilestone: Boolean = hasProperty("milestone")

    private fun hasProperty(property: String): Boolean =
            property in properties
}
