package com.example.loginpageproject.validation.validators;

import java.util.regex.Pattern;

public class FullNameValidator {
    public static boolean isValid(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        // PART 5: letters, spaces, hyphens, apostrophes only. No numbers.
        String nameRegex = "^[a-zA-Z\\s\\-\']+$";
        return Pattern.compile(nameRegex).matcher(name).matches();
    }
}