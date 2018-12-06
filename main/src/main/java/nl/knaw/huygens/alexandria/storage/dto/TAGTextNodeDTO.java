package nl.knaw.huygens.alexandria.storage.dto;

/*
 * #%L
 * alexandria-markup-core
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

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity(version = 2)
public class TAGTextNodeDTO implements TAGDTO {
  @PrimaryKey(sequence = "tgnode_pk_sequence")
  private Long dbId;

  private String text;

  public TAGTextNodeDTO(String text) {
    this.text = text;
  }

  public TAGTextNodeDTO() {
    this("");
  }

  public Long getDbId() {
    return dbId;
  }

  public TAGTextNodeDTO setText(String text) {
    this.text = text;
    return this;
  }

  public String getText() {
    return text;
  }

}
