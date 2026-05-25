/*
Copyright (c) 2026, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.ds.avare.utils.RevenueCatService;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher;
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallDisplayCallback;
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult;
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler;

import java.util.Collections;
import java.util.List;

/**
 * Pro Services screen — Avare's analog of the avarex LoginScreen.
 *
 * Flow:
 *  1. If the user is not signed in to Firebase, the "Sign in / Register"
 *     button launches the FirebaseUI Auth email flow.
 *  2. After successful sign-in, the same Firebase UID is forwarded to
 *     RevenueCat so the entitlement is restored across devices.
 *  3. The "Subscribe" button then opens the RevenueCat paywall via
 *     {@link PaywallActivityLauncher}. If the user already owns the
 *     {@link RevenueCatService#ENTITLEMENT_ID} entitlement the paywall
 *     is suppressed and a "thank you" toast is shown instead.
 *
 * RevenueCat and Firebase are both optional — any missing setup degrades
 * gracefully with a toast rather than crashing.
 */
public class ProActivity extends AppCompatActivity {

    private static final List<AuthUI.IdpConfig> PROVIDERS =
            Collections.singletonList(new AuthUI.IdpConfig.EmailBuilder().build());

    private PaywallActivityLauncher mPaywallLauncher;

    private final ActivityResultLauncher<Intent> mSignInLauncher =
            registerForActivityResult(
                    new FirebaseAuthUIActivityResultContract(),
                    new androidx.activity.result.ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                        @Override
                        public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                            handleSignInResult();
                        }
                    });

    private TextView mStatus;
    private Button mSignInButton;
    private Button mSignOutButton;
    private Button mSubscribeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pro);

        mStatus = findViewById(R.id.pro_status_text);
        mSignInButton = findViewById(R.id.pro_signin_btn);
        mSignOutButton = findViewById(R.id.pro_signout_btn);
        mSubscribeButton = findViewById(R.id.pro_subscribe_btn);
        Button closeButton = findViewById(R.id.pro_close_btn);

        try {
            mPaywallLauncher = new PaywallActivityLauncher(this, new PaywallResultHandler() {
                @Override
                public void onActivityResult(PaywallResult paywallResult) {
                    refreshUI();
                }
            });
        } catch (Throwable ignored) {
            mPaywallLauncher = null;
        }

        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSignIn();
            }
        });

        mSignOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        mSubscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPaywall();
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        refreshUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    private FirebaseUser currentUser() {
        try {
            return FirebaseAuth.getInstance().getCurrentUser();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void refreshUI() {
        FirebaseUser user = currentUser();
        if (user == null) {
            mStatus.setText(getString(R.string.ProStatusSignedOut));
            mSignInButton.setVisibility(View.VISIBLE);
            mSignOutButton.setVisibility(View.GONE);
            mSubscribeButton.setEnabled(false);
            return;
        }

        String label = user.getEmail();
        if (label == null || label.isEmpty()) {
            label = user.getDisplayName();
        }
        if (label == null || label.isEmpty()) {
            label = user.getUid();
        }
        mStatus.setText(getString(R.string.ProStatusSignedIn, label));
        mSignInButton.setVisibility(View.GONE);
        mSignOutButton.setVisibility(View.VISIBLE);
        mSubscribeButton.setEnabled(true);

        // Show "already subscribed" state if RC says so.
        RevenueCatService.isProEntitled(new RevenueCatService.EntitlementCallback() {
            @Override
            public void onResult(boolean entitled) {
                if (entitled && !isFinishing()) {
                    mStatus.setText(getString(R.string.ProStatusEntitled));
                }
            }
        });
    }

    private void startSignIn() {
        try {
            Intent signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(PROVIDERS)
                    .build();
            mSignInLauncher.launch(signInIntent);
        } catch (Throwable t) {
            Toast.makeText(this,
                    getString(R.string.ProServiceUnavailable),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSignInResult() {
        FirebaseUser user = currentUser();
        if (user != null) {
            RevenueCatService.logIn(user.getUid(), user.getEmail(), user.getDisplayName());
        }
        refreshUI();
    }

    private void signOut() {
        try {
            AuthUI.getInstance().signOut(this);
        } catch (Throwable ignored) {
            // ignore
        }
        RevenueCatService.logOut();
        refreshUI();
    }

    private void launchPaywall() {
        FirebaseUser user = currentUser();
        if (user == null) {
            Toast.makeText(this,
                    getString(R.string.ProMustSignInFirst),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // make sure RC knows about this user before showing the paywall
        RevenueCatService.logIn(user.getUid(), user.getEmail(), user.getDisplayName());

        if (mPaywallLauncher == null || !RevenueCatService.isConfigured()) {
            Toast.makeText(this,
                    getString(R.string.ProServiceUnavailable),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            mPaywallLauncher.launchIfNeeded(
                    RevenueCatService.ENTITLEMENT_ID,
                    null,
                    null,
                    true,
                    false,
                    new PaywallDisplayCallback() {
                        @Override
                        public void onPaywallDisplayResult(boolean wasDisplayed) {
                            if (!wasDisplayed && !isFinishing()) {
                                Toast.makeText(ProActivity.this,
                                        getString(R.string.ProStatusEntitled),
                                        Toast.LENGTH_SHORT).show();
                                refreshUI();
                            }
                        }
                    });
        } catch (Throwable t) {
            Toast.makeText(this,
                    getString(R.string.ProServiceUnavailable),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
