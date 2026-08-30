# Login and Account Creation Implementation Plan (with Supabase)

Implement a secure, feature-complete Android authentication system adhering to all specified requirements, using Supabase for the backend.

## User Review Required

> [!IMPORTANT]
> **Supabase Setup**: You will need to provide your Supabase URL and Anon Key in `gradle.properties` or a configuration file after I set up the boilerplate.
> **OTP Logic**: The requirements specify Gmail/email OTP. I will implement this using Supabase's `signUp` with email verification or a custom database-driven OTP flow if manual control is preferred.

## Proposed Changes

### Build and Core Configuration

#### [MODIFY] [libs.versions.toml](file:///Users/mark/Killua/LoginPageProject1/gradle/libs.versions.toml)
* Downgrade AGP to `8.7.0` and Gradle to `8.10` for stability.
* Add Supabase dependencies (`io.github.jan-tennert.supabase:postgrest-kt`, `gotrue-kt`).
* Add Kotlin Coroutines and Lifecycle dependencies.

#### [MODIFY] [build.gradle.kts (App)](file:///Users/mark/Killua/LoginPageProject1/app/build.gradle.kts)
* Enable Kotlin support.
* Add necessary dependencies for Supabase, Material Design 3, and Lifecycle.

---

### Data and Backend (Supabase)

#### [NEW] [SupabaseClient.kt](file:///Users/mark/Killua/LoginPageProject1/app/src/main/java/com/example/loginpageproject/data/SupabaseClient.kt)
* Initialize the Supabase client with URL and Key.

#### [NEW] [UserRepository.kt](file:///Users/mark/Killua/LoginPageProject1/app/src/main/java/com/example/loginpageproject/data/UserRepository.kt)
* Handle registration, login (with attempt counting), and password resets using Supabase Auth and Database.

---

### UI and Logic Enhancements

#### [MODIFY] [AuthValidator.java](file:///Users/mark/Killua/LoginPageProject1/app/src/main/java/com/example/loginpageproject/validation/validators/AuthValidator.java)
* Add **Full Name Validation** (regex for letters/hyphens/apostrophes).
* Update **Mobile Validation** to strictly enforce `09XXXXXXXXX` and `+639XXXXXXXXX`.
* Add **Dynamic Age Validation** (18+ calculation).
* Refine **Password Strength** to return detailed status for the checklist.

#### [MODIFY] [SignUpActivity.java](file:///Users/mark/Killua/LoginPageProject1/app/src/main/java/com/example/loginpageproject/presentation/activities/SignUpActivity.java)
* Implement the **Password Requirement Checklist** (real-time ❌/✅ updates).
* Replace text input for Birthday with a `DatePickerDialog` restricted to 18+ years ago.
* Implement the OTP redirection flow.

#### [NEW] [OTPActivity.java](file:///Users/mark/Killua/LoginPageProject1/app/src/main/java/com/example/loginpageproject/presentation/activities/OTPActivity.java)
* UI for OTP entry, timer for expiry, and resend functionality.

#### [MODIFY] [LoginActivity.java](file:///Users/mark/Killua/LoginPageProject1/app/src/main/java/com/example/loginpageproject/presentation/activities/LoginActivity.java)
* Add **Login Attempt Logic** (3 strikes, 5-minute lockout).
* Implement **Remember Me** using `EncryptedSharedPreferences`.
* Remove Access Type selection (automatic role detection from Supabase).

#### [MODIFY] [BaseActivity.java](file:///Users/mark/Killua/LoginPageProject1/app/src/main/java/com/example/loginpageproject/BaseActivity.java)
* Fix the launcher redirect bug by excluding `MainActivity` and `SignUpActivity` correctly.
* Add "Inactivity Warning" dialog 10 seconds before auto-logout.

---

### Role-Based Access

#### [MODIFY] [LandingActivity.java](file:///Users/mark/Killua/LoginPageProject1/app/src/main/java/com/example/loginpageproject/presentation/activities/LandingActivity.java)
* Dynamically show/hide features based on the `accessType` retrieved from the session.
* Add "User Management" section visible only to `Super Admin`.

---

### Theming

#### [NEW] [ThemeSettings.kt](file:///Users/mark/Killua/LoginPageProject1/app/src/main/java/com/example/loginpageproject/ui/theme/ThemeSettings.kt)
* Logic to toggle between Light and Dark mode and persist the choice.

## Verification Plan

### Automated Tests
* Unit tests for `AuthValidator` (Mobile formats, Age calculation, Password strength).
* Mock repository tests for Login lockout and Password reuse.

### Manual Verification
1. Verify `MainActivity` launches correctly without bouncing to Login.
2. Test Sign-Up with invalid name/mobile/age and ensure warnings appear.
3. Verify OTP is sent and validated (using Supabase logs).
4. Test Login lockout by entering wrong credentials 3 times.
5. Check if session persists with "Remember Me".
6. Verify auto-logout after 1 minute of idle time.
