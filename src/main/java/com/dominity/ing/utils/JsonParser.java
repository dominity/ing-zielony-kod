package com.dominity.ing.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class JsonParser {
    public static <T extends Named> T parseFieldName(InputStream inputStream, T[] candidates, T[] values) throws IOException {
        prepareCandidates(candidates, values);
        filterCandidates(inputStream, candidates);
        return findCandidate(candidates);
    }

    public static String parseString(InputStream inputStream) throws IOException {
        var stringBuilder = new StringBuilder();
        byte ch;
        while ((ch = (byte) inputStream.read()) != '"') {
            stringBuilder.append((char) ch);
        }
        return stringBuilder.toString();
    }

    public static int parseInt(InputStream inputStream) throws IOException {
        byte ch;
        while (!isNumber(ch = (byte) inputStream.read())) {}//TODO makes sure it handles case with invalid format
        var numberArray = new int[1];
        var radix = parseInt(numberArray, inputStream);
        radix = radix == 0 ? 1 : radix * 10;
        return (ch - '0') * radix + numberArray[0];
    }

    public static double parseDouble(InputStream inputStream) throws IOException {
        byte ch;
        while (!isNumberOrDot(ch = (byte) inputStream.read())) {}//TODO makes sure it handles case with invalid format
        var stringBuilder = new StringBuilder();
        stringBuilder.append((char) ch);
        while (isNumberOrDot(ch = (byte) inputStream.read())) {
            stringBuilder.append((char) ch);
        }
        return Double.parseDouble(stringBuilder.toString());
    }

    private static <T> void prepareCandidates(T[] candidates, T[] values) {
        var index = 0;
        for (T candidate : values) {
            candidates[index ++] = candidate;
        }
    }

    private static <T extends Named> void filterCandidates(InputStream inputStream, T[] candidates) throws IOException {
        var start = 0;
        byte ch;
        while ((ch = (byte) inputStream.read()) != '"') {
            for (int i = 0; i < candidates.length; i ++) {
                var fieldName = candidates[i];
                if (fieldName == null) {
                    continue;
                }
                if (start >= fieldName.getName().length() || fieldName.getName().charAt(start) != ch) {
                    candidates[i] = null;
                }
            }
            start ++;
        }
    }

    private static <T> T findCandidate(T[] candidates) {
        for (T candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static int parseInt(int[] numberArray, InputStream inputStream) throws IOException {
        var ch = (byte) inputStream.read();
        if (!isNumber(ch)) {
            return 0;
        } else {
            var radix = parseInt(numberArray, inputStream);
            radix = radix == 0 ? 1 : radix * 10;
            numberArray[0] = (ch - '0') * radix + numberArray[0];
            return radix;
        }
    }

    private static boolean isNumber(byte charAt) {
        return charAt >= '0' && charAt <= '9';
    }

    private static boolean isNumberOrDot(byte charAt) {
        return isNumber(charAt) || charAt == '.';
    }

    public static void writeFieldName(String fieldName, OutputStream outputStream) throws IOException {
        outputStream.write('"');
        for (int i = 0; i < fieldName.length(); i ++) {
            outputStream.write(fieldName.charAt(i));
        }
        outputStream.write('"');
    }

    public static void writeInt(int value, OutputStream outputStream) throws IOException {
        var valueStr = Integer.toString(value);
        for (int i = 0; i < valueStr.length(); i ++) {
            outputStream.write(valueStr.charAt(i));
        }
    }

    public static void writeString(String value, OutputStream outputStream) throws IOException {
        outputStream.write('"');
        for (int i = 0; i < value.length(); i ++) {
            outputStream.write(value.charAt(i));
        }
        outputStream.write('"');
    }

    public interface Named {
        String getName();
    }

    public static void scrollStreamTill(char c, InputStream inputStream) throws IOException {
        while (inputStream.read() != c) {}
    }
}
