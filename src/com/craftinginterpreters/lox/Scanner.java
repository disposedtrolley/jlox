package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    // Initialise the keywords when the Scanner class is loaded.
    static {
        keywords = new HashMap<>();
        keywords.put("and",    TokenType.AND);
        keywords.put("class",  TokenType.CLASS);
        keywords.put("else",   TokenType.ELSE);
        keywords.put("false",  TokenType.FALSE);
        keywords.put("for",    TokenType.FOR);
        keywords.put("fun",    TokenType.FUN);
        keywords.put("if",     TokenType.IF);
        keywords.put("nil",    TokenType.NIL);
        keywords.put("or",     TokenType.OR);
        keywords.put("print",  TokenType.PRINT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("super",  TokenType.SUPER);
        keywords.put("this",   TokenType.THIS);
        keywords.put("true",   TokenType.TRUE);
        keywords.put("var",    TokenType.VAR);
        keywords.put("while",  TokenType.WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    /**
     * Scans the entire source file for tokens, and appends an EOF token if the end of the file
     * has been reached.
     * @return  an array of scanned tokens.
     */
    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        // When the end of the file is reached append an EOF token.
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    /**
     * Scans a character and adds the appropriate token.
     */
    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case '-': addToken(TokenType.MINUS); break;
            case '+': addToken(TokenType.PLUS); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '*': addToken(TokenType.STAR); break;
            case '!': addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG); break;
            case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL); break;
            case '<': addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS); break;
            case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
            case '/':
                if(match('/')) {
                    // A comment goes until the end of the line. Keep advancing until the comment has ended.
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(TokenType.SLASH);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;

            case '\n':
                line++;
                break;

            case '"': string(); break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character");
                }
                break;
        }
    }

    /**
     * Processes an identifier, also checking if it conforms to a reserved keyword.
     */
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        // See if the identifier is a reserved word.
        String text = source.substring(start, current);

        TokenType type = keywords.get(text);
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type);
    }

    /**
     * Processes a number, starting at the first digit to the end, including any fractional parts after "."
     */
    private void number() {
        while (isDigit(peek())) advance();

        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();

            while (isDigit(peek())) advance();
        }

        addToken(TokenType.NUMBER,
                Double.parseDouble(source.substring(start, current)));
    }

    /**
     * Processes a string, starting at the opening " until (and including) the closing ". A new token is created
     * for a valid string stripped of the quotes.
     */
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        // Unterminated string.
        if (isAtEnd()) {
            Lox.error(line, "Unterminated string");
            return;
        }

        // Process the closing ".
        advance();

        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    /**
     * Matches a provided char with the (yet to be processed) current char. Used to ascertain whether a single or
     * multi-character lexeme is being used.
     * @param expected  the value to match against.
     * @return  whether a match has occurred.
     */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    /**
     * Lookahead function that previews the next unprocessed character but does not consume it.
     * @return  the next unprocessed character.
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * Lookahead function that returns the unprocessed character two positions in front of the last processed character.
     * @return  the unprocessed character two positions in front of the last processed character.
     */
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    /**
     * Checks if a given character is a letter or underscore.
     * @param c     the character to check.
     * @return      whether the character is a letter or underscore.
     */
    private boolean isAlpha(char c) {
        return  (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                 c == '_';
    }

    /**
     * Checks if a character is a letter, underscore, or number.
     * @param c     the character to check.
     * @return      whether the character is a letter, underscore, or number.
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    /**
     * Checks if a character is a numerical digit between (and including) 0 and 9.
     * @param c     the character to check.
     * @return      whether the character is a digit.
     */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Returns whether the end of the source file has been reached.
     * @return  whether the end of the source file has been reached.
     */
    private boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * Consumes and returns the next character in the source file.
     * @return  the next character in the source file.
     */
    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    /**
     * Creates a new token with a specified TokenType.
     * @param type  the type of the token.
     */
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    /**
     * Creates a new token with a literal value, e.g. a string or number.
     * @param type  the type of the token.
     * @param literal   the value of the token.
     */
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
