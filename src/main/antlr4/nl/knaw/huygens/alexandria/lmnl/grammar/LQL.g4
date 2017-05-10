grammar LQL;

lqlScript
  : statement* EOF
  ;

statement
  : selectStmt '\n'?
  ;

selectStmt
  : SELECT selectVariable FROM source whereClause?
  ;

selectVariable
  : base (DOT part)?
  | part
  ;

whereClause
  : WHERE expr
  ;

expr
  : literalValue 						                        # literalExpression
  | extendedIdentifier ( '=' | '==' ) literalValue  # equalityComparisonExpression
  | extendedIdentifier ( '!=' | '<>' ) literalValue # inequalityComparisonExpression
  | expr K_AND expr                                 # joiningExpression
  | IDENTIFIER IN '(' selectStmt ')'                # inExpression
  ;

extendedIdentifier
  : base (DOT part)?
  ;

base
  : IDENTIFIER
  ;

part
  : TEXT
  | ANNOTATIONVALUE '(' STRING_LITERAL ')'
  | NAME
  | ID
  ;

source
  : MARKUP markupIdentifier?                          # simpleMarkupSource
  | MARKUP '(' markupName ')' ( '[' indexValue ']' )? # parameterizedMarkupSource
  ;

markupIdentifier
  : IDENTIFIER
  ;

markupName
  : STRING_LITERAL
  ;

indexValue
  : INTEGER
  ;

literalValue
  : NUMERIC_LITERAL
  | STRING_LITERAL
  ;

annotationIdentifier
  : IDENTIFIER
  | annotationIdentifier ':' IDENTIFIER
  ;

SELECT
  : S E L E C T
  ;

INTEGER
  : DIGIT+
  ;

NUMERIC_LITERAL
  : DIGIT+ ( '.' DIGIT* )? ( E [-+]? DIGIT+ )?
  | '.' DIGIT+ ( E [-+]? DIGIT+ )?
  ;

STRING_LITERAL
  : '\'' ( ~'\'' | '\'\'' )* '\''
  ;

K_AND : A N D;

FROM
  : F R O M
  ;

TEXT
  : T E X T
  ;

ANNOTATIONVALUE
  : A N N O T A T I O N V A L U E
  ;

MARKUP
  : M A R K U P
  ;

WHERE
  : W H E R E
  ;

NAME
  : N A M E
  ;

ID
  : I D
  ;

IN
  : I N
  ;

MULTILINE_COMMENT
  : '/*' .*? ( '*/' | EOF ) -> channel(HIDDEN)
  ;

IDENTIFIER
  : [a-zA-Z_] [a-zA-Z_0-9]*
  ;

SPACES
  : [ \u000B\t\r\n] -> skip
  ;

DOT : '.';

fragment DIGIT : [0-9];

fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];

UNEXPECTED_CHAR
  : .
  ;
