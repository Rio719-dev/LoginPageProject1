package com.example.loginpageproject.presentation.viewmodels;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.loginpageproject.data.local.AuthDatabase;
import com.example.loginpageproject.data.repository.UserRepositoryImpl;
import com.example.loginpageproject.domain.repository.UserRepository;
import com.example.loginpageproject.domain.usecase.CheckEmailUseCase;
import com.example.loginpageproject.domain.usecase.LoginUseCase;
import com.example.loginpageproject.domain.usecase.RegisterUserUseCase;
import com.example.loginpageproject.domain.usecase.ResetPasswordUseCase;

public class ViewModelFactory implements ViewModelProvider.Factory {
    private final UserRepository userRepository;

    public ViewModelFactory(Context context) {
        AuthDatabase database = new AuthDatabase(context.getApplicationContext());
        this.userRepository = new UserRepositoryImpl(database);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SignUpViewModel.class)) {
            return (T) new SignUpViewModel(new CheckEmailUseCase(userRepository));
        } else if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(new LoginUseCase(userRepository));
        } else if (modelClass.isAssignableFrom(OTPViewModel.class)) {
            return (T) new OTPViewModel(new RegisterUserUseCase(userRepository));
        } else if (modelClass.isAssignableFrom(ResetPasswordViewModel.class)) {
            return (T) new ResetPasswordViewModel(new ResetPasswordUseCase(userRepository), userRepository);
        } else if (modelClass.isAssignableFrom(ForgotPasswordViewModel.class)) {
            return (T) new ForgotPasswordViewModel(new CheckEmailUseCase(userRepository));
        } else if (modelClass.isAssignableFrom(LandingViewModel.class)) {
            return (T) new LandingViewModel(userRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}