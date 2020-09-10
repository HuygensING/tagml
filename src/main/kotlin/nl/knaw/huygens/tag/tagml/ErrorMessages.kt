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

// TODO: check term use is consistent
const val ILLEGAL_MILESTONE = """Element "%s" does not have the "milestone" property in its definition."""
const val MISSING_ATTRIBUTE = """Required attribute "%s" is missing on element "%s"."""
const val UNDEFINED_ATTRIBUTE = """Attribute "%s" on element "%s" is not defined in the ontology."""
const val USED_UNDEFINED_ATTRIBUTE = """Attribute "%s" is used on an elementDefinition, but has no valid definition in the ontology."""
const val UNKNOWN_ATTRIBUTE_FIELD = """Unknown attribute field "%s""""
const val NO_ATTRIBUTES_ON_RESUME = """Resume tag "%s" has attributes, this is not allowed"""
const val MISSING_ATTRIBUTE_DESCRIPTION = """Attribute "%s" is missing a description."""
const val MISSING_ATTRIBUTE_DATATYPE = """Attribute "%s" is missing a dataType."""
val UNKNOWN_ATTRIBUTE_DATATYPE = """DataType "%s" for attribute "%s" is unknown. Valid dataTypes are: ${AttributeDataType.values().sorted().joinToString()}"""
const val WRONG_DATATYPE = """Attribute "%s" is defined as dataType %s, but is used as dataType %s"""
const val MISSING_ELEMENT_DESCRIPTION = """Element "%s" is missing a description."""
const val MISSING_ONTOLOGY_FIELD = """Field ":ontology" missing in header."""
const val MISSING_ONTOLOGY_ROOT = """Field "root" missing in ontology header."""
const val MISSING_OPEN_TAG = """Closing tag "%s" found without corresponding open tag."""
const val UNDEFINED_ELEMENT = """Element "%s" is not defined in the ontology."""
const val UNDEFINED_RULE_ELEMENT = """Rule %s contains undefined elements %s."""
const val UNEXPECTED_KEY = "Unexpected key %s"
const val UNEXPECTED_CLOSE_TAG = "Unexpected closing tag: found %s, but expected <%s]"
const val ILLEGAL_SUSPEND = "Element %s may not be suspended: it has not been marked as discontinuous in the ontology."
const val UNEXPECTED_ROOT = """Root element "%s" does not match the one defined in the header: "%s""""
const val UNKNOWN_ELEMENT_FIELD = """Unknown element field "%s""""
const val DISCONTINUOUS_ROOT = """Root element "%s" is not allowed to be discontinuous."""
const val UNKNOWN_ANNOTATION_TYPE = "Cannot determine the type of this annotation: %s"
const val NAMESPACE_NOT_DEFINED = """Namespace "%s" has not been defined in the header."""
