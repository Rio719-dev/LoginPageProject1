package com.example.loginpageproject.presentation.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.loginpageproject.domain.model.User;
import com.example.loginpageproject.domain.repository.UserRepository;
import com.example.loginpageproject.domain.usecase.ResetPasswordUseCase;
import com.example.loginpageproject.validation.validators.AuthValidator;

public class ResetPasswordViewModel extends ViewModel {
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final UserRepository userRepository;

    private final MutableLiveData<Integer> passwordStrength = new MutableLiveData<>(0);
    private final MutableLiveData<String> statusAction = new MutableLiveData<>();
    private final MutableLiveData<Boolean> navigateBack = new MutableLiveData<>();

    public ResetPasswordViewModel(ResetPasswordUseCase resetPasswordUseCase, UserRepository userRepository) {
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.userRepository = userRepository;
    }

    public LiveData<Integer> getPasswordStrength() { return passwordStrength; }
    public LiveData<String> getStatusAction() { return statusAction; }
    public LiveData<Boolean> getNavigateBack() { return navigateBack; }

    public void onPasswordChanged(String password) {
        passwordStrength.setValue(AuthValidator.calculatePasswordStrength(password));
    }

    public void resetPassword(String email, String newPass, String confirm) {
        if (newPass.isEmpty() || confirm.isEmpty()) {
            statusAction.setValue("Protocol Violation: Cipher fields empty.");
            return;
        }

        if (!newPass.equals(confirm)) {
            statusAction.setValue("Ciphers do not sync");
            return;
        }

        if (AuthValidator.calculatePasswordStrength(newPass) < 5) {
            statusAction.setValue("Cipher density too low. Protocol requires QUANTUM CORE strength.");
            return;
        }

        User user = userRepository.getUserByEmail(email);
        if (user != null && user.getPasswordHistory().contains(newPass)) {
            statusAction.setValue("Security Protocol: Cannot reuse previous cipher");
            return;
        }

        if (resetPasswordUseCase.execute(email, newPass)) {
            statusAction.setValue("Cipher Reprogrammed Successfully.");
            navigateBack.setValue(true);
        } else {
            statusAction.setValue("System Failure: Cipher override failed.");
        }
    }
}