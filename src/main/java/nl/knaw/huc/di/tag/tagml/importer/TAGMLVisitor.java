package nl.knaw.huc.di.tag.tagml.importer;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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

import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParserBaseVisitor;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;

public class TAGMLVisitor extends TAGMLParserBaseVisitor {
  private TAGStore tagStore;
  private ErrorListener errorListener;

  public TAGMLVisitor(final TAGStore tagStore, final ErrorListener errorListener) {
    this.tagStore = tagStore;
    this.errorListener = errorListener;
  }

  public DocumentWrapper getDocumentWrapper() {
    // TODO
    return null;
  }
}
