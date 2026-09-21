package com.premisave.c2b_hakikisha.util;

/** Kenyan M-Pesa phone number helpers. */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /**
     * Normalises a Kenyan mobile number to the 254XXXXXXXXX form used by M-Pesa.
     * Accepts 2547XXXXXXXX, +2547XXXXXXXX, 07XXXXXXXX, 01XXXXXXXX, 7XXXXXXXX and 1XXXXXXXX,
     * with or without spaces or dashes.
     *
     * @return the 12-digit number starting with 254, or null if the input is not a valid number
     */
    public static String normalizeMsisdn(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 12 && digits.startsWith("254")) {
            return digits;
        }
        if (digits.length() == 10 && digits.startsWith("0")) {
            return "254" + digits.substring(1);
        }
        if (digits.length() == 9 && (digits.startsWith("7") || digits.startsWith("1"))) {
            return "254" + digits;
        }
        return null;
    }
}