package com.archinsurance.ss.adapter.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

public final class CommonUtils {
    public static String convertIntegerToCurrencyFormat(int amount) {
        return "$" + String.format("%,d", amount);
    }

    public static String convertIntStringToCurrencyFormat(String amount) {
        int intAmt = (int) Double.parseDouble(amount);
        return convertIntegerToCurrencyFormat(intAmt);
    }

    public static String convertDoubleToCurrencyFormat(double amount) {
        return "$" + String.format("%,.2f", amount);
    }

    public static String convertDoubleStringToCurrencyFormat(String amount) {
        double dblAmt = Double.parseDouble(amount);
        return convertDoubleToCurrencyFormat(dblAmt);
    }

    public static boolean compareCollectionIgnoreCase(Collection<String> coll1, Collection<String> coll2) {
        Comparator<String> comp = Comparator.comparing(String::toLowerCase);
        Set<String> s1 = new TreeSet<>(comp);
        Set<String> s2 = new TreeSet<>(comp);
        s1.addAll(coll1);
        s2.addAll(coll2);
        return s1.equals(s2);
    }

    public static String parseDate(String date, String dateFormat) {
        DateTimeFormatter fIn = DateTimeFormatter.ofPattern(dateFormat, Locale.US);
        LocalDate ld;
        if (date.contains("TODAY")) {
            ld = LocalDate.now();
            if (date.contains("+")) {
                final int plusDays = Integer.parseInt(date.replace("TODAY", "").replace("+", "").trim());
                ld = ld.plusDays(plusDays);
            } else if (date.contains("-")) {
                final int minusDays = Integer.parseInt(date.replace("TODAY", "").replace("-", "").trim());
                ld = ld.minusDays(minusDays);
            }
        } else {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("M/d/yyyy");
            ld = LocalDate.parse(date, format);
        }
        return ld.format(fIn);
    }

    public static String parseDate(String date) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("M/d/yyyy");
        LocalDate ld = LocalDate.parse(date, format);

        DateTimeFormatter df = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                .toFormatter();
        return ld.format(df);
    }

    public static String parseDate(String date, String currFormat, String desiredFormat) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern(currFormat);
        LocalDate ld = LocalDate.parse(date, format);

        DateTimeFormatter df = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ofPattern(desiredFormat))
                .toFormatter();
        return ld.format(df);
    }

    public static String readFileAsString(String file) throws IOException {
        return new String(Files.readAllBytes(Paths.get(file)));
    }

    public static JSONObject convertStringToJSON(String jsonContent) {
        return new JSONObject(jsonContent);
    }

    public static String formatPrettyJSON(String jsonContent) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Object jsonObject = mapper.readValue(jsonContent, Object.class);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
    }

    public static boolean isJsonEquals(JSONObject firstObj, JSONObject secondObj, JSONCompareMode compareMode) {
        try {
            JSONCompareResult result = JSONCompare.compareJSON(firstObj, secondObj, compareMode);
            return result.passed();
        } catch (JSONException ignored) {
        }
        return false;
    }

    public static Set<JsonElement> setOfElements(JsonArray arr) {
        Set<JsonElement> set = new HashSet<>();
        arr.forEach(set::add);
        return set;
    }

    public static String addDays(String date, int days) {
        Calendar c = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
        try {
            c.setTime(df.parse(date));
            c.add(Calendar.DAY_OF_MONTH, days);
            return df.format(c.getTime());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static String parseCurrency(String value) {
        DecimalFormat df = new DecimalFormat("###,###,###.00");
        try {
            return df.format(Float.valueOf(value));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static String getCurrentTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH-mm-ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

    public static String randomString() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String randomString = "";
        String test = "TEST_CREATE_STG_";
        int i;
        int length = 3;
        Random rand = new Random();
        char[] text = new char[length];
        for (i = 0; i < length; i++) {
            text[i] = characters.charAt(rand.nextInt(characters.length()));
        }
        for (i = 0; i < text.length; i++) {
            randomString += text[i];
        }
        randomString = test + randomString;
        return randomString;
    }
}
