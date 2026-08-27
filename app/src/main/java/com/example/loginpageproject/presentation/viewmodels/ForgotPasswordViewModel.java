package com.example.loginpageproject.presentation.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.loginpageproject.domain.usecase.CheckEmailUseCase;

public class ForgotPasswordViewModel extends ViewModel {
    private final CheckEmailUseCase checkEmailUseCase;
    private final MutableLiveData<String> statusAction = new MutableLiveData<>();
    private final MutableLiveData<String> navigateToReset = new MutableLiveData<>();

    public ForgotPasswordViewModel(CheckEmailUseCase checkEmailUseCase) {
        this.checkEmailUseCase = checkEmailUseCase;
    }

    public LiveData<String> getStatusAction() { return statusAction; }
    public LiveData<String> getNavigateToReset() { return navigateToReset; }

    public void sendResetCode(String email) {
        if (email.isEmpty()) {
            statusAction.setValue("Provide a frequency, entity.");
            return;
        }

        if (checkEmailUseCase.execute(email)) {
            navigateToReset.setValue(email);
        } else {
            statusAction.setValue("Frequency not found in the network.");
        }
    }
}