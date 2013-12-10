package com.janrain.android.engage;
/*
 *  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *  Copyright (c) 2011, Janrain, Inc.
 *
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification,
 *  are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *  * Neither the name of the Janrain, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 */

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.text.TextUtils;
import com.janrain.android.utils.LogUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.janrain.android.engage.JRNativeAuth.NativeProvider;
import static com.janrain.android.engage.JRNativeAuth.NativeAuthError;


public class NativeGooglePlus extends NativeProvider {
    private static Class plusClientClass;
    private static Class plusClientBuilderClass;
    private static Class connectionCallbackClass;
    private static Class connectionFailedListenerClass;
    private static Class connectionResultClass;
    private static Class playServicesUtilClass;
    private static Class googleAuthUtilClass;
    private static Class userRecoverableAuthExceptionClass;
    private static Class googleAuthExceptionClass;
    private static boolean didLoadClasses = false;

    private static final int REQUEST_CODE_RESOLVE_ERROR = 9000;
    private static final int RESULT_OK = -1;
    private static final int RESULT_SUCCESS = 0;
    private static final int SERVICE_MISSING_CONNECTION_RESULT = 1;
    private static final int SERVICE_VERSION_UPDATE_REQUIRED_CONNECTION_RESULT = 2;
    private static final int SERVICE_DISABLED_CONNECTION_RESULT = 3;

    private Object plusClient;
    private Object connectionResult;
    private String[] scopes;
    private boolean isConnecting = false;

    static {
        ClassLoader classLoader = NativeProvider.class.getClassLoader();

        try {
            plusClientClass = classLoader.loadClass("com.google.android.gms.plus.PlusClient");
            plusClientBuilderClass = classLoader.loadClass("com.google.android.gms.plus.PlusClient$Builder");
            connectionCallbackClass = classLoader.loadClass(
                    "com.google.android.gms.common.GooglePlayServicesClient$ConnectionCallbacks");
            connectionFailedListenerClass = classLoader.loadClass(
                    "com.google.android.gms.common.GooglePlayServicesClient$OnConnectionFailedListener");
            connectionResultClass = classLoader.loadClass("com.google.android.gms.common.ConnectionResult");
            playServicesUtilClass = classLoader.loadClass(
                    "com.google.android.gms.common.GooglePlayServicesUtil");
            googleAuthUtilClass = classLoader.loadClass("com.google.android.gms.auth.GoogleAuthUtil");
            userRecoverableAuthExceptionClass = classLoader.loadClass(
                    "com.google.android.gms.auth.UserRecoverableAuthException");
            googleAuthExceptionClass = classLoader.loadClass(
                    "com.google.android.gms.auth.GoogleAuthException");
            didLoadClasses = true;
        } catch (ClassNotFoundException e) {
            LogUtils.logd("Could not load Native Google+ SDK" + e);
        }
    }

    /*package*/ static boolean canHandleAuthentication() {
        return didLoadClasses;
    }

    /*package*/ NativeGooglePlus() {
        scopes = new String[] {"https://www.googleapis.com/auth/plus.login"};
    }

    @Override
    public String provider() {
        return "googleplus";
    }

    @Override
    public void startAuthentication(Activity activity, JRNativeAuth.NativeAuthCallback callback) {
        super.startAuthentication(activity, callback);

        int isGooglePlayAvailable = 0;
        try {
            Method isGooglePlayServicesAvailable =
                    playServicesUtilClass.getMethod("isGooglePlayServicesAvailable", Context.class);
            Object isAvailable = isGooglePlayServicesAvailable.invoke(playServicesUtilClass, fromActivity);
            isGooglePlayAvailable = ((Integer)isAvailable).intValue();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if (isGooglePlayAvailable != RESULT_SUCCESS) {
            if (shouldShowUnavailableDialog(isGooglePlayAvailable)) {
                showGooglePlayUnavailableDialog(isGooglePlayAvailable);
            } else {
                completion.onFailure("Google Play unavailable", NativeAuthError.GOOGLE_PLAY_UNAVAILABLE, true);
            }
        } else {
            if (getPlusClient() == null) {
                completion.onFailure("Could not instantiate Google Plus Client",
                                     NativeAuthError.CANNOT_INSTANTIATE_GOOGLE_PLAY_CLIENT);
                return;
            }

            plusClientConnect();

            if (!isPlusClientConnected()) {
                if (connectionResult == null) {
                    isConnecting = true;
                } else {
                    startResolutionForResult();
                }

            }
        }
    }

    public void signOut() {
        if (isPlusClientConnected()) {
            try {
                Method clearDefaultAccount = plusClientClass.getMethod("clearDefaultAccount");
                clearDefaultAccount.invoke(getPlusClient());

                Method disconnect = plusClientClass.getMethod("disconnect");
                disconnect.invoke(getPlusClient());
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        // FIXME This should get called when the fromActivity's onActivityRequest gets called.
        if (requestCode == REQUEST_CODE_RESOLVE_ERROR && responseCode == RESULT_OK) {
            connectionResult = null;
            plusClientConnect();
        }
    }

    private void plusClientConnect() {
        LogUtils.logd("plusClientConnect");
        try {
            Method connect = plusClientClass.getMethod("connect");
            connect.invoke(getPlusClient());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean shouldShowUnavailableDialog(int googlePlayAvailabilityStatus) {
        return !JREngage.shouldTryWebViewAuthenticationWhenGooglePlayIsUnavailable()
            && (googlePlayAvailabilityStatus == SERVICE_MISSING_CONNECTION_RESULT
                || googlePlayAvailabilityStatus == SERVICE_VERSION_UPDATE_REQUIRED_CONNECTION_RESULT
                || googlePlayAvailabilityStatus == SERVICE_DISABLED_CONNECTION_RESULT);
    }

    private void showGooglePlayUnavailableDialog(int googlePlayAvailabilityStatus) {
        Dialog dialog = null;
        try {
            Method getErrorDialog = playServicesUtilClass.getMethod("getErrorDialog", int.class,
                    Activity.class, int.class);
            dialog = (Dialog)getErrorDialog.invoke(playServicesUtilClass, googlePlayAvailabilityStatus,
                    fromActivity, REQUEST_CODE_RESOLVE_ERROR);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if (dialog != null) {
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    completion.onFailure("Google Play unavailable", NativeAuthError.GOOGLE_PLAY_UNAVAILABLE);
                }
            });
            dialog.show();
        } else {
            throw new RuntimeException("Unable to instantiate Google Play error dialog");
        }
    }

    private Object getPlusClient() {
        if (plusClient == null) {

            try {
                Constructor constructor = plusClientBuilderClass.getConstructor(Context.class,
                                                                                connectionCallbackClass,
                                                                                connectionFailedListenerClass);
                Object builder = constructor.newInstance(fromActivity, getConnectionCallback(),
                                                         getOnConnectFailedListener());

                Method setScopes = plusClientBuilderClass.getMethod("setScopes", String[].class);
                setScopes.invoke(builder, new Object[] {scopes});

                Method build = plusClientBuilderClass.getMethod("build");
                plusClient = build.invoke(builder);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        return plusClient;
    }

    private Boolean isPlusClientConnected() {
        if (getPlusClient() == null) return false;

        Object isConnected;

        try {
            Method isClientConnected = plusClientClass.getMethod("isConnected");
            isConnected = isClientConnected.invoke(getPlusClient());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return (Boolean)isConnected;
    }

    private String getPlusAccountName() {

        Object accountName;

        try {
            Method getAccountName = plusClientClass.getMethod("getAccountName");
            accountName = getAccountName.invoke(getPlusClient());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return (String)accountName;
    }

    private Object getConnectionCallback() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                LogUtils.logd("Method Name: " + method.getName());
                if (method.getName().equals("onConnected")) {
                    // onConnected(Bundle connectionHint)
                    LogUtils.logd("onConnected");
                    isConnecting = false;
                    new GetAccessTokenTask().execute();
                } else if (method.getName().equals("onDisconnected")) {
                    // onDisconnected()
                    LogUtils.logd("onDisconnected");
                }
                return null;
            }
        };

        return Proxy.newProxyInstance(
                connectionCallbackClass.getClassLoader(),
                new Class[]{connectionCallbackClass}, handler);
    }

    private Object getOnConnectFailedListener() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                LogUtils.logd("Method: " + method.getName());
                if (method.getName().equals("onConnectionFailed")) {
                    // onConnectionFailed(ConnectionResult result)
                    connectionResult = objects[0];

                    if (isConnecting) {
                        if (connectionResultHasResolution()) {
                            startResolutionForResult();
                        }
                    }
                } else if (method.getName().equals("equals")) {
                    return (o == objects[0]);
                }
                return null;
            }
        };
        return Proxy.newProxyInstance(
                connectionFailedListenerClass.getClassLoader(),
                new Class[]{connectionFailedListenerClass}, handler);
    }

    private Boolean connectionResultHasResolution() {
        Object hasResolution = false;

        try {
            Method resultHasResolution = connectionResultClass.getMethod("hasResolution");
            hasResolution = resultHasResolution.invoke(connectionResult);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return (Boolean)hasResolution;
    }

    private void startResolutionForResult() {

        try {
            Method startResolution = connectionResultClass.getMethod("startResolutionForResult",
                    Activity.class, int.class);
            startResolution.invoke(connectionResult, fromActivity, REQUEST_CODE_RESOLVE_ERROR);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IntentSender.SendIntentException) {
                // Try connecting again
                connectionResult = null;
                plusClientConnect();
            } else {
                throw new RuntimeException(e);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private class GetAccessTokenTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            Object token = null;

            try {
                Method getToken = googleAuthUtilClass.getMethod("getToken", Context.class, String.class,
                                                                String.class);
                token = getToken.invoke(googleAuthUtilClass, fromActivity, getPlusAccountName(),
                                        "oauth2:" + TextUtils.join(" ", scopes));
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof  IOException) {
                    completion.onFailure("Could not get Google+ Access Token",
                                         NativeAuthError.CANNOT_GET_GOOGLE_PLUS_ACCESS_TOKEN, e);
                    return null;
                } else if (userRecoverableAuthExceptionClass.isInstance(e.getCause())) {
                    LogUtils.logd("UserRecoverableAuthException");
                    token = null;
                    handleUserRecoverableAuthException(e);
                } else if (googleAuthExceptionClass.isInstance(e.getCause())) {
                    throw new RuntimeException(e);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            LogUtils.logd("token: " + (String)token);
            return (String)token;
        }

        @Override
        protected void onPostExecute(String token) {
            LogUtils.logd("Got the token: " + token);
        }
    }

    private void handleUserRecoverableAuthException(InvocationTargetException exception) {
        Intent intent = null;

        try {
            Method getIntent = userRecoverableAuthExceptionClass.getMethod("getIntent");
            intent = (Intent)getIntent.invoke(userRecoverableAuthExceptionClass);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        fromActivity.startActivityForResult(intent, REQUEST_CODE_RESOLVE_ERROR);
    }
}
