/*
Copyright (c) 2026, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;

import android.content.Context;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.LogLevel;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesConfiguration;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.LogInCallback;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;

/**
 * Thin wrapper around the RevenueCat Android SDK. Mirrors the avarex
 * {@code RevenueCatService} so the two apps share the same entitlement model.
 *
 * RevenueCat is treated as an OPTIONAL service in Avare. Initialization
 * failures (no Google Play, no network, missing api key, etc.) are swallowed
 * and the app keeps working — non-subscribers simply see the start-up
 * "use count" nag dialog while subscribers see no extra dialogs at all.
 */
public final class RevenueCatService {

    /**
     * Public key, replaced at build time (matches avarex placeholder).
     * If left as the placeholder string the SDK will simply fail to fetch
     * customer info and {@link #isProEntitled(EntitlementCallback)} returns
     * false — which is the desired "not subscribed" fallback.
     */
    public static final String ANDROID_API_KEY = "@@___revenuecat_android_api_key__@@";

    /** Entitlement identifier configured in the RevenueCat dashboard. */
    public static final String ENTITLEMENT_ID = "Pro";

    private static boolean sConfigured = false;

    private RevenueCatService() { }

    /**
     * Initialize the SDK. Safe to call multiple times; no-op after the first
     * successful call. Never throws.
     */
    public static synchronized void init(Context context) {
        if (sConfigured) {
            return;
        }
        if (context == null) {
            return;
        }
        // If the api-key placeholder was not substituted at build time, do
        // not attempt to configure. RevenueCat would reject an invalid key
        // and log noisily on every launch.
        if (ANDROID_API_KEY == null
                || ANDROID_API_KEY.isEmpty()
                || ANDROID_API_KEY.startsWith("@@")) {
            return;
        }
        try {
            Purchases.setLogLevel(LogLevel.INFO);
            PurchasesConfiguration cfg = new PurchasesConfiguration.Builder(
                    context.getApplicationContext(), ANDROID_API_KEY).build();
            Purchases.configure(cfg);
            sConfigured = true;
        } catch (Throwable ignored) {
            // RevenueCat is optional — swallow any init failure
        }
    }

    /** @return true once {@link #init(Context)} has succeeded. */
    public static synchronized boolean isConfigured() {
        return sConfigured;
    }

    /** Callback for {@link #isProEntitled(EntitlementCallback)}. */
    public interface EntitlementCallback {
        /** Always invoked on the main thread. */
        void onResult(boolean entitled);
    }

    /**
     * Log the current Firebase user into RevenueCat so their entitlement is
     * synced across devices. Safe to call repeatedly. Mirrors avarex
     * {@code RevenueCatService.logIn}. All errors swallowed.
     */
    public static void logIn(String userId, String email, String displayName) {
        if (!isConfigured() || userId == null || userId.isEmpty()) {
            return;
        }
        try {
            Purchases.getSharedInstance().logIn(userId, new LogInCallback() {
                @Override
                public void onReceived(CustomerInfo customerInfo, boolean created) {
                    // ignore
                }

                @Override
                public void onError(PurchasesError error) {
                    // ignore
                }
            });
            if (email != null && !email.isEmpty()) {
                try {
                    Purchases.getSharedInstance().setEmail(email);
                } catch (Throwable ignored) { /* ignore */ }
            }
            if (displayName != null && !displayName.isEmpty()) {
                try {
                    Purchases.getSharedInstance().setDisplayName(displayName);
                } catch (Throwable ignored) { /* ignore */ }
            }
        } catch (Throwable ignored) {
            // optional service
        }
    }

    /** Log out of RevenueCat (anonymous identity going forward). */
    public static void logOut() {
        if (!isConfigured()) {
            return;
        }
        try {
            Purchases.getSharedInstance().logOut(new ReceiveCustomerInfoCallback() {
                @Override
                public void onReceived(CustomerInfo customerInfo) { /* ignore */ }

                @Override
                public void onError(PurchasesError error) { /* ignore */ }
            });
        } catch (Throwable ignored) {
            // optional service
        }
    }

    /**
     * Asynchronously check whether the current user owns the {@link
     * #ENTITLEMENT_ID} entitlement. If RevenueCat is not configured, or any
     * error occurs (network down, etc.), the callback receives {@code false}.
     */
    public static void isProEntitled(final EntitlementCallback cb) {
        if (cb == null) {
            return;
        }
        if (!isConfigured()) {
            cb.onResult(false);
            return;
        }
        try {
            Purchases.getSharedInstance().getCustomerInfo(new ReceiveCustomerInfoCallback() {
                @Override
                public void onReceived(CustomerInfo customerInfo) {
                    boolean entitled = false;
                    try {
                        EntitlementInfo ent = customerInfo
                                .getEntitlements()
                                .getAll()
                                .get(ENTITLEMENT_ID);
                        entitled = ent != null && ent.isActive();
                    } catch (Throwable ignored) {
                        entitled = false;
                    }
                    cb.onResult(entitled);
                }

                @Override
                public void onError(PurchasesError error) {
                    cb.onResult(false);
                }
            });
        } catch (Throwable t) {
            cb.onResult(false);
        }
    }
}
