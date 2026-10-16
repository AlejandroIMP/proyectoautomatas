package com.analizador.lexico;

import com.analizador.modelo.ErrorLexico;
import com.analizador.modelo.TablaSimbolos;
import com.analizador.modelo.Token;
import com.analizador.modelo.TokenType;
import java.util.ArrayList;
import java.util.List;

%%

%public
%class AnalizadorLexicoJS
%unicode
%line
%column
%type Token
%function yylex

%{
private TablaSimbolos tablaSimbolos = new TablaSimbolos();
private final List<ErrorLexico> errores = new ArrayList<>();

public AnalizadorLexicoJS(java.io.Reader reader, TablaSimbolos tablaSimbolos) {
    this(reader);
    this.tablaSimbolos = tablaSimbolos == null ? new TablaSimbolos() : tablaSimbolos;
}
public List<ErrorLexico> getErrores() { return List.copyOf(errores); }
public TablaSimbolos getTablaSimbolos() { return tablaSimbolos; }
private Token token(TokenType tipo) {
    return new Token(tipo, yytext(), yyline + 1, yycolumn + 1);
}
private Token token(TokenType tipo, Object valor) {
    return new Token(tipo, yytext(), yyline + 1, yycolumn + 1, valor);
}
%}

%eofval{
    return new Token(TokenType.EOF, "EOF", yyline + 1, yycolumn + 1);
%eofval}

LineTerminator = \r|\n|\r\n
WhiteSpace = [ \t\f] | {LineTerminator}
LineComment = "//" [^\r\n]* {LineTerminator}?
BlockComment = "/*" ([^*]|\*+[^*/])* \*+ "/"
Letter = [a-zA-Z_$]
Digit = [0-9]
Identifier = {Letter}({Letter}|{Digit})*
Integer = 0 | [1-9]{Digit}*
Float = ({Integer}"."{Digit}+|{Integer}("."{Digit}+)?[eE][+-]?{Digit}+)
DoubleString = \"([^\"\\\r\n]|\\.)*\"
SingleString = \'([^\'\\\r\n]|\\.)*\'
String = {DoubleString}|{SingleString}
TemplateString = \`([^\\\`]|\\.)*\`

%%

<YYINITIAL> {
    {WhiteSpace}       { }
    {LineComment}      { }
    {BlockComment}     { }

    "let"              { return token(TokenType.LET); }
    "const"            { return token(TokenType.CONST); }
    "var"              { return token(TokenType.VAR); }
    "function"         { return token(TokenType.FUNCTION); }
    "if"               { return token(TokenType.IF); }
    "else"             { return token(TokenType.ELSE); }
    "while"            { return token(TokenType.WHILE); }
    "for"              { return token(TokenType.FOR); }
    "return"           { return token(TokenType.RETURN); }
    "true"             { return token(TokenType.TRUE, true); }
    "false"            { return token(TokenType.FALSE, false); }
    "null"             { return token(TokenType.NULL); }

    "==="              { return token(TokenType.IGUAL_ESTRICTO); }
    "!=="              { return token(TokenType.DIFERENTE_ESTRICTO); }
    "=="               { return token(TokenType.IGUAL_IGUAL); }
    "!="               { return token(TokenType.DIFERENTE); }
    "<="               { return token(TokenType.MENOR_IGUAL); }
    ">="               { return token(TokenType.MAYOR_IGUAL); }
    "++"               { return token(TokenType.INCREMENTO); }
    "--"               { return token(TokenType.DECREMENTO); }
    "&&"               { return token(TokenType.AND); }
    "||"               { return token(TokenType.OR); }
    "+"                { return token(TokenType.MAS); }
    "-"                { return token(TokenType.MENOS); }
    "*"                { return token(TokenType.MULT); }
    "/"                { return token(TokenType.DIV); }
    "%"                { return token(TokenType.MOD); }
    "="                { return token(TokenType.ASIGNACION); }
    "<"                { return token(TokenType.MENOR); }
    ">"                { return token(TokenType.MAYOR); }
    "!"                { return token(TokenType.NOT); }

    "("                { return token(TokenType.PAREN_IZQ); }
    ")"                { return token(TokenType.PAREN_DER); }
    "{"                { return token(TokenType.LLAVE_IZQ); }
    "}"                { return token(TokenType.LLAVE_DER); }
    "["                { return token(TokenType.CORCHETE_IZQ); }
    "]"                { return token(TokenType.CORCHETE_DER); }
    ";"                { return token(TokenType.PUNTO_COMA); }
    ","                { return token(TokenType.COMA); }
    ":"                { return token(TokenType.DOS_PUNTOS); }
    "."                { return token(TokenType.PUNTO); }

    {Float}            { return token(TokenType.LIT_FLOTANTE, Double.parseDouble(yytext())); }
    {Integer}          { return token(TokenType.LIT_ENTERO, Long.parseLong(yytext())); }
    {String}           { return token(TokenType.LIT_CADENA, yytext().substring(1, yytext().length() - 1)); }
    {TemplateString}   { return token(TokenType.LIT_TEMPLATE, yytext().substring(1, yytext().length() - 1)); }
    {Identifier}       {
        tablaSimbolos.registrarIdentificador(yytext(), yyline + 1, yycolumn + 1);
        return token(TokenType.ID);
    }
    [^]                {
        errores.add(new ErrorLexico(yytext(), yyline + 1, yycolumn + 1,
                "Símbolo no perteneciente al alfabeto léxico"));
    }
}
