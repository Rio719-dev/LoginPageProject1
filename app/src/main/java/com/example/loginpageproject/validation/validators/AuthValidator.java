package com.example.loginpageproject.validation.validators;

import java.util.Calendar;
import java.util.regex.Pattern;

public class AuthValidator {

    public static boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPHMobile(String mobile) {
        return Pattern.compile("^(09|\\+639)\\d{9}$").matcher(mobile).matches();
    }

    public static boolean isOfAge(String birthday) {
        try {
            String[] parts = birthday.split("/");
            int month = Integer.parseInt(parts[0]) - 1;
            int day = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            Calendar dob = Calendar.getInstance();
            Calendar today = Calendar.getInstance();
            dob.set(year, month, day);

            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age >= 18;
        } catch (Exception e) {
            return false;
        }
    }

    public static int calculatePasswordStrength(String password) {
        int strength = 0;
        if (password.length() >= 8 && password.length() <= 16) strength++;
        if (Pattern.compile("[A-Z]").matcher(password).find()) strength++;
        if (Pattern.compile("[a-z]").matcher(password).find()) strength++;
        if (Pattern.compile("[0-9]").matcher(password).find()) strength++;
        if (Pattern.compile("[!@#$%^&*(),.?\":{}|<>]").matcher(password).find()) strength++;
        return strength;
    }
}