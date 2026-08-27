package com.example.loginpageproject.presentation.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.loginpageproject.domain.model.User;
import com.example.loginpageproject.domain.usecase.RegisterUserUseCase;

public class OTPViewModel extends ViewModel {
    private final RegisterUserUseCase registerUserUseCase;
    private final MutableLiveData<Boolean> registrationSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private String generatedOtp = "123456";

    public OTPViewModel(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
    }

    public LiveData<Boolean> getRegistrationSuccess() { return registrationSuccess; }
    public LiveData<String> getError() { return error; }

    public void verifyOtp(String enteredOtp, User user) {
        if (enteredOtp.equals(generatedOtp)) {
            if (registerUserUseCase.execute(user)) {
                registrationSuccess.setValue(true);
            } else {
                error.setValue("Database Sync Failure");
            }
        } else {
            error.setValue("Frequency mismatch. Code invalid.");
        }
    }

    public void resendOtp() {
        generatedOtp = "654321";
    }
}