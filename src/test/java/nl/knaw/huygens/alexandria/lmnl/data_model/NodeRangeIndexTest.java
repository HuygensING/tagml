package nl.knaw.huygens.alexandria.lmnl.data_model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.Test;

import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;

public class NodeRangeIndexTest {

  @Test
  public void testRangesFromNodes() {
    String lmnl = "[excerpt\n"//
        + "  [source [date}1915{][title}The Housekeeper{]]\n"//
        + "  [author\n"//
        + "    [name}Robert Frost{]\n"//
        + "    [dates}1874-1963{]] }\n"//
        + "[s}[l [n}144{n]}He manages to keep the upper hand{l]\n"//
        + "[l [n}145{n]}On his own farm.{s] [s}He's boss.{s] [s}But as to hens:{l]\n"//
        + "[l [n}146{n]}We fence our flowers in and the hens range.{l]{s]\n"//
        + "{excerpt]";
    NodeRangeIndex index = index(lmnl);
    // textnode 1= "He manages to keep the upper hand"
    Set<Integer> rangeIndices = index.getRanges(1); // indices of ranges that contain textnode 1
    assertThat(rangeIndices).containsExactly(1); // excerpt,s,l
  }

  @Test
  public void testNodesFromRanges() {
    String lmnl = "[excerpt\n"//
        + "  [source [date}1915{][title}The Housekeeper{]]\n"//
        + "  [author\n"//
        + "    [name}Robert Frost{]\n"//
        + "    [dates}1874-1963{]] }\n"//
        + "[s}[l [n}144{n]}He manages to keep the upper hand{l]\n"//
        + "[l [n}145{n]}On his own farm.{s] [s}He's boss.{s] [s}But as to hens:{l]\n"//
        + "[l [n}146{n]}We fence our flowers in and the hens range.{l]{s]\n"//
        + "{excerpt]";
    NodeRangeIndex index = index(lmnl);
    Set<Integer> textNodeIndices = index.getTextNodes(0); // indices of textnodes contained in range 0: excerpt
    assertThat(textNodeIndices).containsExactly(0, 1, 2, 3, 4, 5, 6);
  }

  @Test
  public void testIndexWithAlice() {
    String lmnl = "[excerpt}[p}\n" + "Alice was beginning to get very tired of sitting by her sister on the bank,\n"
        + "and of having nothing to do: once or twice she had peeped into the book her sister\n" + "was reading, but it had no pictures or conversations in it, \n"
        + "[q=a}and what is the use of a book,{q=a]\n" + "thought Alice\n" + "[q=a}without pictures or conversation?{q=a]\n" + "{p]{excerpt]";
    NodeRangeIndex index = index(lmnl);
    Set<Integer> textNodeIndices = index.getTextNodes(2); // indices of textnodes contained in range 2: q=a
    assertThat(textNodeIndices).containsExactly(3, 5);

    Set<Integer> rangeIndices = index.getRanges(1); // indices of ranges that contain textnode 1: "On his own farm"
    assertThat(rangeIndices).containsExactly(0, 1); // excerpt,p,q=a
  }

  private NodeRangeIndex index(String lmnl) {
    LMNLImporter importer = new LMNLImporter();
    Document document = importer.importLMNL(lmnl);
    return new NodeRangeIndex(document.value());
  }

}
