package nl.knaw.huygens.alexandria.storage.dto;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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

import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo;
import org.assertj.core.api.AbstractObjectAssert;

public class AnnotationInfoAssert extends AbstractObjectAssert<AnnotationInfoAssert, AnnotationInfo> {

  public AnnotationInfoAssert(final AnnotationInfo actual) {
    super(actual, AnnotationInfoAssert.class);
  }

  public AnnotationInfoAssert hasTag(final String tag) {
    isNotNull();
    if (!actual.hasName(tag)) {
      failWithMessage("Expected annotation's tag to be <%s> but was <%s>", tag, actual.getName());
    }
    return myself;
  }

}