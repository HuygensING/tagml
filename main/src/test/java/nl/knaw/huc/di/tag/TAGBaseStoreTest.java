package nl.knaw.huc.di.tag;

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

import nl.knaw.huc.di.tag.model.graph.DotFactory;
import nl.knaw.huc.di.tag.tagml.TAGMLBaseTest;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TAGBaseStoreTest extends TAGMLBaseTest {

  protected static TAGStore store;
  protected static LMNLExporter lmnlExporter;
  private static Path tmpDir;
  DotFactory dotFactory = new DotFactory();

  @BeforeClass
  public static void beforeClass() throws IOException {
    tmpDir = mkTmpDir();
    store = new TAGStore(tmpDir.toString(), false);
    store.open();
    lmnlExporter = new LMNLExporter(store).useShorthand();
  }

  @AfterClass
  public static void afterClass() throws IOException {
    if (store != null) {
      store.close();
    }
    rmTmpDir(tmpDir);
  }

  protected void logDocumentGraph(final TAGDocument document, final String input) {
    System.out.println("\n------------8<------------------------------------------------------------------------------------\n");
    System.out.println(dotFactory.toDot(document, input));
    System.out.println("\n------------8<------------------------------------------------------------------------------------\n");
  }

  private static Path mkTmpDir() throws IOException {
    String sysTmp = System.getProperty("java.io.tmpdir");
    Path tmpPath = Paths.get(sysTmp, ".alexandria");
    if (!tmpPath.toFile().exists()) {
      tmpPath = Files.createDirectory(tmpPath);
    }
    return tmpPath;
  }

  private static void rmTmpDir(final Path tmpPath) throws IOException {
    Files.walk(tmpPath)
        .map(Path::toFile)
        .forEach(File::deleteOnExit);
  }

}
