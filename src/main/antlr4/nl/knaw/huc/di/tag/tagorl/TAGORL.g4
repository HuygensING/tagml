grammar TAGORL;

ontologyRule
  : hierarchyRule
  | setRule
  | tripleRule
  ;

hierarchyRule
  : Name '>' children
  ;

Name
  : NameStartChar
  | NameStartChar NameChar* NameEndChar
  ;

children
  : child
  | child (',' child)+
  | '(' child (',' child)+ ')'
  ;

child
  : Name     # oneChild
  | Name '?' # optionalChild
  | Name '*' # zeroOrMoreChild
  | Name '+' # oneOrMoreChild
  ;

setRule
  : Name '(' child (',' child)+ ')'
  ;


tripleRule
  : subject predicate object
  ;

subject
  : Name
  ;

predicate
  : Name
  ;

object
  : Name ( ',' Name )*
  ;

WS
  : [ \t\r\n]+ -> skip
  ;

fragment LETTER: [a-zA-Z] | '\u00C0'..'\u00D6' | '\u00D8'..'\u00F6' | '\u00F8'..'\u00FF' ;

fragment DIGIT
  : [0-9]
  ;

fragment
NameChar
  : NameStartChar
  | '-'
  | '_'
  | DIGIT
//  | '.'
  | '\u00B7'
  | '\u0300'..'\u036F'
  | '\u203F'..'\u2040'
  ;

fragment
NameStartChar
  : [a-z]
  ;

fragment
NameEndChar
  : NameStartChar
  | DIGIT
  ;
