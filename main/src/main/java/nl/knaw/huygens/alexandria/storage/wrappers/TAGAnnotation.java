package nl.knaw.huygens.alexandria.storage.wrappers;

/*
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

import nl.knaw.huygens.alexandria.storage.TAGAnnotationDTO;
import nl.knaw.huygens.alexandria.storage.TAGDocumentDTO;
import nl.knaw.huygens.alexandria.storage.TAGStore;

import java.util.stream.Stream;

public class TAGAnnotation {
  private final TAGStore store;
  private final TAGAnnotationDTO annotation;

  public TAGAnnotation(TAGStore store, TAGAnnotationDTO annotation) {
    this.store = store;
    this.annotation = annotation;
    update();
  }

  public TAGDocument getDocument() {
    TAGDocumentDTO document = store.getDocument(annotation.getDocumentId());
    return new TAGDocument(store, document);
  }

  public Long getDbId() {
    return annotation.getDbId();
  }

  public String getTag() {
    return annotation.getTag();
  }

  public TAGAnnotation addAnnotation(TAGAnnotation TAGAnnotation) {
    annotation.getAnnotationIds().add(TAGAnnotation.getDbId());
    update();
    return this;
  }

  public Stream<TAGAnnotation> getAnnotationStream() {
    return annotation.getAnnotationIds().stream()//
        .map(store::getAnnotationWrapper);
  }

  public TAGAnnotationDTO getAnnotation() {
    return annotation;
  }

  private void update() {
    store.persist(annotation);
  }

  @Override
  public String toString() {
    // TODO process different annotation types
    return annotation.getTag() + '=';
  }
}
