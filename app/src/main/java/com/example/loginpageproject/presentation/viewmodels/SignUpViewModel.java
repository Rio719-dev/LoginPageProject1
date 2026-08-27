package com.example.loginpageproject.presentation.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.loginpageproject.domain.model.User;
import com.example.loginpageproject.domain.usecase.CheckEmailUseCase;
import com.example.loginpageproject.validation.validators.AuthValidator;

public class SignUpViewModel extends ViewModel {
    private final CheckEmailUseCase checkEmailUseCase;

    private final MutableLiveData<Integer> passwordStrength = new MutableLiveData<>(0);
    private final MutableLiveData<String> errorAction = new MutableLiveData<>();
    private final MutableLiveData<User> navigateToOtp = new MutableLiveData<>();

    public SignUpViewModel(CheckEmailUseCase checkEmailUseCase) {
        this.checkEmailUseCase = checkEmailUseCase;
    }

    public LiveData<Integer> getPasswordStrength() { return passwordStrength; }
    public LiveData<String> getErrorAction() { return errorAction; }
    public LiveData<User> getNavigateToOtp() { return navigateToOtp; }

    public void onPasswordChanged(String password) {
        passwordStrength.setValue(AuthValidator.calculatePasswordStrength(password));
    }

    public void validateAndRegister(String name, String bday, String address, String email, String mobile, String pass, String confirm, String role) {
        if (name.isEmpty() || bday.isEmpty() || address.isEmpty() || email.isEmpty() || mobile.isEmpty() || pass.isEmpty()) {
            errorAction.setValue("Protocol Violation: Missing telemetry data.");
            return;
        }

        if (!AuthValidator.isOfAge(bday)) {
            errorAction.setValue("Subject must be 18+ Solar Cycles");
            return;
        }

        if (!pass.equals(confirm)) {
            errorAction.setValue("Ciphers do not sync");
            return;
        }

        if (AuthValidator.calculatePasswordStrength(pass) < 5) {
            errorAction.setValue("Cipher density too low. Protocol requires QUANTUM CORE strength.");
            return;
        }

        if (!AuthValidator.isValidPHMobile(mobile)) {
            errorAction.setValue("Invalid Philippine Link Number");
            return;
        }

        if (checkEmailUseCase.execute(email)) {
            errorAction.setValue("Frequency already occupied");
            return;
        }

        User user = new User(name, bday, address, email, mobile, pass, role);
        navigateToOtp.setValue(user);
    }
}