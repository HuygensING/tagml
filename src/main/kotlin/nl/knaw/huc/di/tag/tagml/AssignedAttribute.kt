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
package nl.knaw.huc.di.tag.tagml

sealed class AssignedAttribute(val name: String)

class OptionalAttribute(name: String) : AssignedAttribute(name) {
    override fun toString(): String = name

    override fun equals(other: Any?): Boolean =
            other != null && other is OptionalAttribute && other.name == name

    override fun hashCode(): Int =
            javaClass.hashCode() + name.hashCode()
}

class RequiredAttribute(name: String) : AssignedAttribute(name) {
    override fun toString(): String = "$name!"

    override fun equals(other: Any?): Boolean =
            other != null && other is RequiredAttribute && other.name == name

    override fun hashCode(): Int =
            javaClass.hashCode() + name.hashCode()

}