package com.accounting.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StateCodeUtil {

    private static final Map<String, String> STATE_TO_GSTIN_CODE = new LinkedHashMap<>();

    static {
        STATE_TO_GSTIN_CODE.put("ANDAMAN AND NICOBAR ISLANDS", "35");
        STATE_TO_GSTIN_CODE.put("ANDHRA PRADESH", "28");
        STATE_TO_GSTIN_CODE.put("ANDHRA PRADESH (NEW)", "37");
        STATE_TO_GSTIN_CODE.put("ARUNACHAL PRADESH", "12");
        STATE_TO_GSTIN_CODE.put("ASSAM", "18");
        STATE_TO_GSTIN_CODE.put("BIHAR", "10");
        STATE_TO_GSTIN_CODE.put("CHANDIGARH", "04");
        STATE_TO_GSTIN_CODE.put("CHATTISGARH", "22");
        STATE_TO_GSTIN_CODE.put("CHHATTISGARH", "22");
        STATE_TO_GSTIN_CODE.put("DADRA AND NAGAR HAVELI", "26");
        STATE_TO_GSTIN_CODE.put("DADRA AND NAGAR HAVELI AND DAMAN AND DIU", "26");
        STATE_TO_GSTIN_CODE.put("DAMAN AND DIU", "25");
        STATE_TO_GSTIN_CODE.put("DELHI", "07");
        STATE_TO_GSTIN_CODE.put("GOA", "30");
        STATE_TO_GSTIN_CODE.put("GUJARAT", "24");
        STATE_TO_GSTIN_CODE.put("HARYANA", "06");
        STATE_TO_GSTIN_CODE.put("HIMACHAL PRADESH", "02");
        STATE_TO_GSTIN_CODE.put("JAMMU AND KASHMIR", "01");
        STATE_TO_GSTIN_CODE.put("JHARKHAND", "20");
        STATE_TO_GSTIN_CODE.put("KARNATAKA", "29");
        STATE_TO_GSTIN_CODE.put("KERALA", "32");
        STATE_TO_GSTIN_CODE.put("LADAKH", "38");
        STATE_TO_GSTIN_CODE.put("LAKSHADWEEP", "31");
        STATE_TO_GSTIN_CODE.put("LAKSHADWEEP ISLANDS", "31");
        STATE_TO_GSTIN_CODE.put("MADHYA PRADESH", "23");
        STATE_TO_GSTIN_CODE.put("MAHARASHTRA", "27");
        STATE_TO_GSTIN_CODE.put("MANIPUR", "14");
        STATE_TO_GSTIN_CODE.put("MEGHALAYA", "17");
        STATE_TO_GSTIN_CODE.put("MIZORAM", "15");
        STATE_TO_GSTIN_CODE.put("NAGALAND", "13");
        STATE_TO_GSTIN_CODE.put("ODISHA", "21");
        STATE_TO_GSTIN_CODE.put("PONDICHERRY", "34");
        STATE_TO_GSTIN_CODE.put("PUDUCHERRY", "34");
        STATE_TO_GSTIN_CODE.put("PUNJAB", "03");
        STATE_TO_GSTIN_CODE.put("RAJASTHAN", "08");
        STATE_TO_GSTIN_CODE.put("SIKKIM", "11");
        STATE_TO_GSTIN_CODE.put("TAMIL NADU", "33");
        STATE_TO_GSTIN_CODE.put("TELANGANA", "36");
        STATE_TO_GSTIN_CODE.put("TRIPURA", "16");
        STATE_TO_GSTIN_CODE.put("UTTAR PRADESH", "09");
        STATE_TO_GSTIN_CODE.put("UTTARAKHAND", "05");
        STATE_TO_GSTIN_CODE.put("WEST BENGAL", "19");
    }

    public static String getStateCode(String stateName) {
        if (stateName == null || stateName.isBlank()) return "";
        String code = STATE_TO_GSTIN_CODE.get(stateName.toUpperCase().trim());
        return code != null ? code : "";
    }

    public static List<String> getAllStateNames() {
        return List.of(
            "Andaman and Nicobar Islands",
            "Andhra Pradesh",
            "Andhra Pradesh (New)",
            "Arunachal Pradesh",
            "Assam",
            "Bihar",
            "Chandigarh",
            "Chhattisgarh",
            "Dadra and Nagar Haveli and Daman and Diu",
            "Delhi",
            "Goa",
            "Gujarat",
            "Haryana",
            "Himachal Pradesh",
            "Jammu and Kashmir",
            "Jharkhand",
            "Karnataka",
            "Kerala",
            "Ladakh",
            "Lakshadweep",
            "Madhya Pradesh",
            "Maharashtra",
            "Manipur",
            "Meghalaya",
            "Mizoram",
            "Nagaland",
            "Odisha",
            "Puducherry",
            "Punjab",
            "Rajasthan",
            "Sikkim",
            "Tamil Nadu",
            "Telangana",
            "Tripura",
            "Uttar Pradesh",
            "Uttarakhand",
            "West Bengal"
        );
    }

    public static Map<String, String> getAllStateCodes() {
        return Collections.unmodifiableMap(STATE_TO_GSTIN_CODE);
    }
}
