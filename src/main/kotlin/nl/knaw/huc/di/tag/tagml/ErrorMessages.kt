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

const val MISSING_ONTOLOGY_FIELD = """Field ":ontology" missing in header."""
const val MISSING_ONTOLOGY_ROOT = """Field "root" missing in ontology header."""
const val UNEXPECTED_KEY = "Unexpected key %s"
const val UNEXPECTED_ROOT = """Root element "%s" does not match the one defined in the header: "%s""""
const val MISSING_OPEN_TAG = """Closing tag "%s" found without corresponding open tag."""
const val UNDEFINED_ELEMENT = """Element "%s" is not defined in the ontology."""
const val UNDEFINED_ATTRIBUTE = """Attribute "%s" on element "%s" is not defined in the ontology."""
const val USED_UNDEFINED_ATTRIBUTE = """Attribute "%s" is used on an elementDefinition, but not defined in the ontology."""
const val MISSING_ATTRIBUTE = """Required attribute "%s" is missing on element "%s"."""
const val ILLEGAL_MILESTONE = """Element "%s" does not have the "milestone" property in its definition."""
