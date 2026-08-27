package com.example.loginpageproject.presentation.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.loginpageproject.domain.model.User;
import com.example.loginpageproject.domain.usecase.LoginUseCase;

public class LoginViewModel extends ViewModel {
    private final LoginUseCase loginUseCase;

    private final MutableLiveData<User> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> loginError = new MutableLiveData<>();

    public LoginViewModel(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    public LiveData<User> getLoginSuccess() { return loginSuccess; }
    public LiveData<String> getLoginError() { return loginError; }

    public void login(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            loginError.setValue("Identify yourself, entity.");
            return;
        }

        User user = loginUseCase.execute(email, password);
        if (user != null) {
            loginSuccess.setValue(user);
        } else {
            loginError.setValue("Access Denied: Credentials Invalid");
        }
    }
}