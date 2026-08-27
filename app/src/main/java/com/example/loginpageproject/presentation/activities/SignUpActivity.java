package com.example.loginpageproject.presentation.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.loginpageproject.BaseActivity;
import com.example.loginpageproject.R;
import com.example.loginpageproject.presentation.viewmodels.SignUpViewModel;
import com.example.loginpageproject.presentation.viewmodels.ViewModelFactory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

public class SignUpActivity extends BaseActivity {

    private SignUpViewModel viewModel;
    private TextInputEditText etFullName, etBirthday, etAddress, etEmail, etMobile, etPassword, etConfirmPassword;
    private Spinner spnAccessType;
    private TextView tvStrengthIndicator;
    private ProgressBar registrationProgress;
    private int selectedYear, selectedMonth, selectedDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // activity_main is the signup layout

        viewModel = new ViewModelProvider(this, new ViewModelFactory(this)).get(SignUpViewModel.class);

        initializeViews();
        setupListeners();
        setupSecurity();
        observeViewModel();
    }

    private void initializeViews() {
        etFullName = findViewById(R.id.etFullName);
        etBirthday = findViewById(R.id.etBirthday);
        etAddress = findViewById(R.id.etAddress);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spnAccessType = findViewById(R.id.spnAccessType);
        tvStrengthIndicator = findViewById(R.id.tvStrengthIndicator);
        registrationProgress = findViewById(R.id.registrationProgress);
        MaterialButton btnInitialize = findViewById(R.id.btnInitialize);
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);

        btnInitialize.setOnClickListener(v -> {
            viewModel.validateAndRegister(
                    etFullName.getText().toString().trim(),
                    etBirthday.getText().toString().trim(),
                    etAddress.getText().toString().trim(),
                    etEmail.getText().toString().trim(),
                    etMobile.getText().toString().trim(),
                    etPassword.getText().toString(),
                    etConfirmPassword.getText().toString(),
                    spnAccessType.getSelectedItem().toString()
            );
        });

        tvLoginLink.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        etBirthday.setOnClickListener(v -> showDatePicker());

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.onPasswordChanged(s.toString());
                updateProgress();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        TextWatcher progressWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateProgress(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        etFullName.addTextChangedListener(progressWatcher);
        etEmail.addTextChangedListener(progressWatcher);
        etMobile.addTextChangedListener(progressWatcher);
        etConfirmPassword.addTextChangedListener(progressWatcher);
    }

    private void setupSecurity() {
        etConfirmPassword.setLongClickable(false);
        etConfirmPassword.setTextIsSelectable(false);
        etConfirmPassword.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) { return false; }
            @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
            @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) { return false; }
            @Override public void onDestroyActionMode(ActionMode mode) {}
        });
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, y, m, d) -> {
            selectedYear = y;
            selectedMonth = m;
            selectedDay = d;
            String date = (m + 1) + "/" + d + "/" + y;
            etBirthday.setText(date);
            updateProgress();
        }, year, month, day);
        datePickerDialog.show();
    }

    private void updateProgress() {
        int progress = 0;
        if (!etFullName.getText().toString().isEmpty()) progress += 15;
        if (!etBirthday.getText().toString().isEmpty()) progress += 15;
        if (!etEmail.getText().toString().isEmpty()) progress += 15;
        if (!etMobile.getText().toString().isEmpty()) progress += 15;
        if (!etPassword.getText().toString().isEmpty()) progress += 20;
        if (!etConfirmPassword.getText().toString().isEmpty()) progress += 20;
        registrationProgress.setProgress(Math.min(progress, 100));
    }

    private void observeViewModel() {
        viewModel.getPasswordStrength().observe(this, strength -> {
            if (strength <= 2) {
                tvStrengthIndicator.setText("🔴 WEAK: PLASMA LEAK");
                tvStrengthIndicator.setTextColor(getResources().getColor(R.color.alien_plasma_red));
            } else if (strength < 5) {
                tvStrengthIndicator.setText("🟡 STABLE: ION SHIELD");
                tvStrengthIndicator.setTextColor(getResources().getColor(R.color.alien_warning_yellow));
            } else {
                tvStrengthIndicator.setText("🟢 SECURE: QUANTUM CORE");
                tvStrengthIndicator.setTextColor(getResources().getColor(R.color.alien_neon_green));
            }
        });

        viewModel.getErrorAction().observe(this, error -> {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });

        viewModel.getNavigateToOtp().observe(this, user -> {
            Intent intent = new Intent(this, OTPActivity.class);
            intent.putExtra("name", user.getFullName());
            intent.putExtra("bday", user.getBirthday());
            intent.putExtra("address", user.getAddress());
            intent.putExtra("email", user.getEmail());
            intent.putExtra("mobile", user.getMobile());
            intent.putExtra("password", user.getPassword());
            intent.putExtra("role", user.getAccessType());
            startActivity(intent);
        });
    }
}