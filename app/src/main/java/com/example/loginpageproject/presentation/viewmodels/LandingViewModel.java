package com.example.loginpageproject.presentation.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.loginpageproject.domain.model.User;
import com.example.loginpageproject.domain.repository.UserRepository;

public class LandingViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();

    public LandingViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<User> getCurrentUser() { return currentUser; }

    public void loadUser(String email) {
        currentUser.setValue(userRepository.getUserByEmail(email));
    }
}