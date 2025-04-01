package org.niit_project.backend.utils;

public class PhoneNumberConverter {

    public static String convertToNormal(String phoneNumber) {
        final String phone = phoneNumber.trim();
        var is11 = phone.length() == 11 && !phone.contains("+");
        return is11 ? phone : "0" + phone.trim().substring(1, 11);
    }

    public static String convertToFull(String phoneNumber){
        var phone = phoneNumber.trim();
        var is11 = phone.length() == 11 && !phone.contains("+");
        var isNotHavePlus = phone.length() == 13 && !phone.startsWith("+");
        return is11
                ? "+234" +phone.trim().substring(1, 11)
                : isNotHavePlus
                ? "+" + phone
                : phone;

    }   
}
