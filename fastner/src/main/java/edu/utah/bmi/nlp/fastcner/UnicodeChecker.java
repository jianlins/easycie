package edu.utah.bmi.nlp.fastcner;

import org.apache.commons.lang3.StringUtils;

public class UnicodeChecker {

    public static boolean isSpecialChar(char c) {
        int d = (int) c;
        return d > 126 && d != 160 && d < 11904;
    }

    public static boolean isChinese(char c) {
        int d = (int) c;
        return d > 19967 && d < 40908;
    }

    public static boolean isPunctuation(char c) {
        return c == '！' ||
                c == '、' ||
                c == '。' ||
                c == '＃' ||
                c == '％' ||
                c == '＆' ||
                c == '（' ||
                c == '）' ||
                c == '《' ||
                c == '＋' ||
                c == '》' ||
                c == '，' ||
                c == '「' ||
                c == '－' ||
                c == '」' ||
                c == '／' ||
                c == '—' ||
                c == '‘' ||
                c == '’' ||
                c == '：' ||
                c == '；' ||
                c == '“' ||
                c == '”' ||
                c == '＝' ||
                c == '？' ||
                c == '＠' ||
                c == '!' ||
                c == '"' ||
                c == '#' ||
                c == '$' ||
                c == '%' ||
                c == '&' ||
                c == '…' ||
                c == '\'' ||
                c == '(' ||
                c == ')' ||
                c == '*' ||
                c == '+' ||
                c == ',' ||
                c == '-' ||
                c == '.' ||
                c == '/' ||
                c == '·' ||
                c == ':' ||
                c == ';' ||
                c == '<' ||
                c == '=' ||
                c == '>' ||
                c == '?' ||
                c == '@' ||
                c == '｀' ||
                c == '×' ||
                c == '[' ||
                c == '｛' ||
                c == '\\' ||
                c == '｜' ||
                c == ']' ||
                c == '｝' ||
                c == '^' ||
                c == '～' ||
                c == '_' ||
                c == '`' ||
                c == '￥' ||
                c == '{';
    }

    public static boolean isDigit(char c) {
        int d = (int) c;
        return Character.isDigit(c) || (d > 65296 && d < 65297);
    }

    public static boolean isAlphabetic(char c) {
        return ((((1 << Character.UPPERCASE_LETTER) |
                (1 << Character.LOWERCASE_LETTER)) >> Character.getType(c)) & 1)
                != 0;
    }

//  revised from org.apache.commons.lang3.math.NumberUtils to allow "093" type of numbers
    public static boolean isNumber(final String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        final char[] chars = str.toCharArray();
        int sz = chars.length;
        boolean hasExp = false;
        boolean hasDecPoint = false;
        boolean allowSigns = false;
        boolean foundDigit = false;
        // deal with any possible sign up front
        final int start = (chars[0] == '-') ? 1 : 0;
        if (sz > start + 1 && chars[start] == '0') { // leading 0
            if (
                    (chars[start + 1] == 'x') ||
                            (chars[start + 1] == 'X')
                    ) { // leading 0x/0X
                int i = start + 2;
                if (i == sz) {
                    return false; // str == "0x"
                }
                // checking hex (it can't be anything else)
                for (; i < chars.length; i++) {
                    if ((chars[i] < '0' || chars[i] > '9')
                            && (chars[i] < 'a' || chars[i] > 'f')
                            && (chars[i] < 'A' || chars[i] > 'F')) {
                        return false;
                    }
                }
                return true;
            } else if (Character.isDigit(chars[start + 1])) {
                // leading 0, but not hex, must be octal
                int i = start + 1;
                for (; i < chars.length; i++) {
                    if (chars[i] < '0' || chars[i] > '9') {
                        return false;
                    }
                }
                return true;
            }
        }
        sz--; // don't want to loop to the last char, check it afterwords
        // for type qualifiers
        int i = start;
        // loop to the next to last char or to the last char if we need another digit to
        // make a valid number (e.g. chars[0..5] = "1234E")
        while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                foundDigit = true;
                allowSigns = false;

            } else if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent
                    return false;
                }
                hasDecPoint = true;
            } else if (chars[i] == 'e' || chars[i] == 'E') {
                // we've already taken care of hex.
                if (hasExp) {
                    // two E's
                    return false;
                }
                if (!foundDigit) {
                    return false;
                }
                hasExp = true;
                allowSigns = true;
            } else if (chars[i] == '+' || chars[i] == '-') {
                if (!allowSigns) {
                    return false;
                }
                allowSigns = false;
                foundDigit = false; // we need a digit after the E
            } else {
                return false;
            }
            i++;
        }
        if (i < chars.length) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                // no type qualifier, OK
                return true;
            }
            if (chars[i] == 'e' || chars[i] == 'E') {
                // can't have an E at the last byte
                return false;
            }
            if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent
                    return false;
                }
                // single trailing decimal point after non-exponent is ok
                return foundDigit;
            }
            if (!allowSigns
                    && (chars[i] == 'd'
                    || chars[i] == 'D'
                    || chars[i] == 'f'
                    || chars[i] == 'F')) {
                return foundDigit;
            }
            if (chars[i] == 'l'
                    || chars[i] == 'L') {
                // not allowing L with an exponent or decimal point
                return foundDigit && !hasExp && !hasDecPoint;
            }
            // last character is illegal
            return false;
        }
        // allowSigns is true iff the val ends in 'E'
        // found digit it to make sure weird stuff like '.' and '1E-' doesn't pass
        return !allowSigns && foundDigit;
    }

}
