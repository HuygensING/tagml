# tagml

This library contains the ANTLR4-generated `TAGMLLexer` and `TAGMLParser` for tokenizing and parsing [TAGML](https://github.com/HuygensING/TAG/tree/master/TAGML) documents.

Because in TAGML markup ranges may overlap, the markup does not have to be closed in the exact reverse order in which it was opened. This makes the TAGML grammar context-sensitive. The ANTLR4 grammar used in this library, however, is context-free, because ANTLR4 does not provide a way to encode context-sensitive grammars.
The parser generated from the grammar cannot check that every open tag (eg. `[tag>`) is eventually followed by a corresponding close tag (`<tag]`).
This check, and other validity checks are done in post-processing. (see [alexandria-markup](https://github.com/HuygensING/alexandria-markup))  

## maven usage

add this dependency to your `pom.xml` 

```xml
<dependency>
  <groupId>nl.knaw.huygens.alexandria</groupId>
  <artifactId>tagml</artifactId>
  <version>2.5-SNAPSHOT</version>
</dependency>
```

and this repository definition:
```xml
<repository>
  <id>huygens</id>
  <url>http://maven.huygens.knaw.nl/repository/</url>
  <releases>
    <enabled>true</enabled>
    <updatePolicy>always</updatePolicy>
    <checksumPolicy>warn</checksumPolicy>
  </releases>
  <snapshots>
    <enabled>true</enabled>
    <updatePolicy>always</updatePolicy>
    <checksumPolicy>fail</checksumPolicy>
  </snapshots>
</repository>
```
