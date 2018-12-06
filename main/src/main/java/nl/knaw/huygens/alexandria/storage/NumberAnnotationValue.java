package nl.knaw.huygens.alexandria.storage;

/*-
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

@Entity(version = 1)
public class NumberAnnotationValue extends AnnotationValue {
  Double value;

  private NumberAnnotationValue(){}

  NumberAnnotationValue(final Double value) {
    setValue(value);
  }

  public void setValue(final Double value) {
    this.value = value;
  }

  public Double getValue() {
    return value;
  }
}
