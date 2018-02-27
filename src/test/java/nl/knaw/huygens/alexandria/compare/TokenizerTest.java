package nl.knaw.huygens.alexandria.compare;

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
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenizerTest {
  @Test
  public void testTokenizeText() {
    List<TextToken> textTokens = Tokenizer.tokenizeText("a b c");
    assertThat(textTokens).hasSize(3);
  }

  @Test
  public void testTokenizeText1() {
    List<TextToken> textTokens = Tokenizer.tokenizeText(" a b c");
    assertThat(textTokens).hasSize(3);
    assertThat(textTokens.get(0).content).isEqualTo(" a ");
  }

  @Test
  public void testTokenizeText2() {
    List<TextToken> textTokens = Tokenizer.tokenizeText("\n");
    assertThat(textTokens).hasSize(1);
    assertThat(textTokens.get(0).content).isEqualTo("\n");
  }

  @Test
  public void testTokenizeText3() {
    List<TextToken> textTokens = Tokenizer.tokenizeText(" ");
    assertThat(textTokens).hasSize(1);
    assertThat(textTokens.get(0).content).isEqualTo(" ");
  }

  @Test
  public void testTokenizeText4() {
    List<TextToken> textTokens = Tokenizer.tokenizeText("! ");
    assertThat(textTokens).hasSize(1);
    assertThat(textTokens.get(0).content).isEqualTo("! ");
  }

  @Test
  public void testTokenizeText5() {
    List<TextToken> textTokens = Tokenizer.tokenizeText("Alas, poor Yorick!");
    assertThat(textTokens).hasSize(3);
    assertThat(textTokens.get(0).content).isEqualTo("Alas, ");
    assertThat(textTokens.get(1).content).isEqualTo("poor ");
    assertThat(textTokens.get(2).content).isEqualTo("Yorick!");
  }

  @Test
  public void testTokenizeText6() {
    List<TextToken> textTokens = Tokenizer.tokenizeText("Lucy, for you the snowdrop and the bay");
    assertThat(textTokens).hasSize(8);
    assertThat(textTokens.get(0).content).isEqualTo("Lucy, ");
    assertThat(textTokens.get(1).content).isEqualTo("for ");
    assertThat(textTokens.get(2).content).isEqualTo("you ");
  }
}