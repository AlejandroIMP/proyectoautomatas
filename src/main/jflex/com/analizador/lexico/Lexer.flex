package com.analizador.lexico;

import com.analizador.modelo.ErrorLexico;
import com.analizador.modelo.TablaSimbolos;
import com.analizador.modelo.Token;
import com.analizador.modelo.TokenType;
import com.analizador.sintactico.sym;
import java_cup.runtime.Symbol;
import java.util.ArrayList;
import java.util.List;

%%

%public
%class AnalizadorLexicoJS
%unicode
%line
%column
%cup
%state SIN_CIERRE

%{
private TablaSimbolos tablaSimbolos = new TablaSimbolos();
private final List<ErrorLexico> errores = new ArrayList<>();
private final List<Token> tokens = new ArrayList<>();

public AnalizadorLexicoJS(java.io.Reader reader, TablaSimbolos tablaSimbolos) {
    this(reader);
    this.tablaSimbolos = tablaSimbolos == null ? new TablaSimbolos() : tablaSimbolos;
}

public List<ErrorLexico> getErrores() { return List.copyOf(errores); }
public List<Token> getTokens() { return List.copyOf(tokens); }
public TablaSimbolos getTablaSimbolos() { return tablaSimbolos; }

public Symbol yylex() throws java.io.IOException {
    return next_token();
}

private Symbol symbol(int cupSym, TokenType tipo) {
    Token t = new Token(tipo, yytext(), yyline + 1, yycolumn + 1, yytext());
    tokens.add(t);
    return new Symbol(cupSym, yyline + 1, yycolumn + 1, yytext());
}

private Symbol symbol(int cupSym, TokenType tipo, Object valor) {
    Token t = new Token(tipo, yytext(), yyline + 1, yycolumn + 1, valor);
    tokens.add(t);
    return new Symbol(cupSym, yyline + 1, yycolumn + 1, valor);
}
%}

%eofval{
    Token eofToken = new Token(TokenType.EOF, "EOF", yyline + 1, yycolumn + 1);
    tokens.add(eofToken);
    return new Symbol(sym.EOF, yyline + 1, yycolumn + 1, "EOF");
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
    "/*"               {
        errores.add(new ErrorLexico(yytext(), yyline + 1, yycolumn + 1,
                "Comentario de bloque sin cerrar"));
        yybegin(SIN_CIERRE);
    }

    "let"              { return symbol(sym.LET, TokenType.LET); }
    "const"            { return symbol(sym.CONST, TokenType.CONST); }
    "var"              { return symbol(sym.VAR, TokenType.VAR); }
    "function"         { return symbol(sym.FUNCTION, TokenType.FUNCTION); }
    "if"               { return symbol(sym.IF, TokenType.IF); }
    "else"             { return symbol(sym.ELSE, TokenType.ELSE); }
    "while"            { return symbol(sym.WHILE, TokenType.WHILE); }
    "for"              { return symbol(sym.FOR, TokenType.FOR); }
    "return"           { return symbol(sym.RETURN, TokenType.RETURN); }
    "true"             { return symbol(sym.TRUE, TokenType.TRUE, true); }
    "false"            { return symbol(sym.FALSE, TokenType.FALSE, false); }
    "null"             { return symbol(sym.NULL, TokenType.NULL, null); }

    "print"            { return symbol(sym.PRINT, TokenType.PRINT); }
    "console"          { return symbol(sym.CONSOLE, TokenType.CONSOLE); }
    "log"              { return symbol(sym.LOG, TokenType.LOG); }

    "==="              { return symbol(sym.IGUAL_ESTRICTO, TokenType.IGUAL_ESTRICTO); }
    "!=="              { return symbol(sym.DIFERENTE_ESTRICTO, TokenType.DIFERENTE_ESTRICTO); }
    "=="               { return symbol(sym.IGUAL_IGUAL, TokenType.IGUAL_IGUAL); }
    "!="               { return symbol(sym.DIFERENTE, TokenType.DIFERENTE); }
    "<="               { return symbol(sym.MENOR_IGUAL, TokenType.MENOR_IGUAL); }
    ">="               { return symbol(sym.MAYOR_IGUAL, TokenType.MAYOR_IGUAL); }
    "++"               { return symbol(sym.INCREMENTO, TokenType.INCREMENTO); }
    "--"               { return symbol(sym.DECREMENTO, TokenType.DECREMENTO); }
    "&&"               { return symbol(sym.AND, TokenType.AND); }
    "||"               { return symbol(sym.OR, TokenType.OR); }
    "+"                { return symbol(sym.MAS, TokenType.MAS); }
    "-"                { return symbol(sym.MENOS, TokenType.MENOS); }
    "*"                { return symbol(sym.MULT, TokenType.MULT); }
    "/"                { return symbol(sym.DIV, TokenType.DIV); }
    "%"                { return symbol(sym.MOD, TokenType.MOD); }
    "="                { return symbol(sym.ASIGNACION, TokenType.ASIGNACION); }
    "<"                { return symbol(sym.MENOR, TokenType.MENOR); }
    ">"                { return symbol(sym.MAYOR, TokenType.MAYOR); }
    "!"                { return symbol(sym.NOT, TokenType.NOT); }

    "("                { return symbol(sym.PAREN_IZQ, TokenType.PAREN_IZQ); }
    ")"                { return symbol(sym.PAREN_DER, TokenType.PAREN_DER); }
    "{"                { return symbol(sym.LLAVE_IZQ, TokenType.LLAVE_IZQ); }
    "}"                { return symbol(sym.LLAVE_DER, TokenType.LLAVE_DER); }
    "["                { return symbol(sym.CORCHETE_IZQ, TokenType.CORCHETE_IZQ); }
    "]"                { return symbol(sym.CORCHETE_DER, TokenType.CORCHETE_DER); }
    ";"                { return symbol(sym.PUNTO_COMA, TokenType.PUNTO_COMA); }
    ","                { return symbol(sym.COMA, TokenType.COMA); }
    ":"                { return symbol(sym.DOS_PUNTOS, TokenType.DOS_PUNTOS); }
    "."                { return symbol(sym.PUNTO, TokenType.PUNTO); }

    {Float}            { return symbol(sym.LIT_FLOTANTE, TokenType.LIT_FLOTANTE, Double.parseDouble(yytext())); }
    {Digit}+ {Letter} ({Letter}|{Digit})* {
        errores.add(new ErrorLexico(yytext(), yyline + 1, yycolumn + 1,
            "Identificador inválido: no puede iniciar con un dígito"));
    }
    {Integer}          { return symbol(sym.LIT_ENTERO, TokenType.LIT_ENTERO, Long.parseLong(yytext())); }
    {String}           { return symbol(sym.LIT_CADENA, TokenType.LIT_CADENA, yytext().substring(1, yytext().length() - 1)); }
    {TemplateString}   { return symbol(sym.LIT_TEMPLATE, TokenType.LIT_TEMPLATE, yytext().substring(1, yytext().length() - 1)); }
    {Identifier}       {
        tablaSimbolos.registrarIdentificador(yytext(), yyline + 1, yycolumn + 1);
        return symbol(sym.ID, TokenType.ID, yytext());
    }
    [^]                {
        errores.add(new ErrorLexico(yytext(), yyline + 1, yycolumn + 1,
                "Símbolo no perteneciente al alfabeto léxico"));
    }
}

<SIN_CIERRE> {
    [^]                { }
}
