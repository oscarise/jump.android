# Native Authentication Guide

This guide describes the process of integrating with native Android authentication systems. The Social Sign-in library has historically supported authentication by means of a WebView running a traditional web OAuth flow. Support is now introduced for authentication by means of native identity-provider libraries.

## Supported Providers

- Facebook
- Google+

Native authentication is supported by the Social Sign-in library, and is compatible with both the Sign-in only and User Registration deployments.

At this time native authentication is available for authentication only, and not for social-identity-resource authorization (e.g. sharing.)

The SDK is not currently able to request the same scopes that are configured in the Engage dashboard when using Native Authentication. This will be available in a future release. For the time being Facebook requests basic_info and Google+ requests plus.login.

## 10,000′ View

1. Configure the native authentication framework.
2. Start authentication.
3. The library will delegate the authentication to the native authentication framework.
4. The library delegate message will fire when native authentication completes.

## Facebook

### Configure the Native Authentication Framework

Follow the steps for creating a new Android project in the Getting Started with the Facebook SDK for Android guide at developers.facebook.com. Stop before the “A minimum viable social application” section.

Make sure that both your Android application and Social Sign-in app are configured to use the same Facebook Application App ID.

### Begin Sign-In or Authentication

Start authentication or sign-in as normal. If the Facebook Android SDK is compiled into your app, it will be used to perform all Facebook authentication.

### Release Builds

If you plan to use ProGuard on your release builds add the following to your ProGuard configuration file

    -keep class com.facebook.** {*;}
    -keepattributes Signature

## Google+

### Configure the Native Authentication Framework

Follow the Google+ Platform getting started directions up to “Initalize the Plus Client”.For native Google+ authentication to work via Social Sign-in both Janrain and the Google+ iOS SDK must be configured to use the same Google+ project in the Google Cloud Console.

### Configure JUMP

By default the Jump library will silently fail when Google Play Services is unavailable then attempt to sign in using a WebView. If you would like the SDK to present Google’s dialog suggesting that the user install or update or configure Google Play Services when the error is one of `SERVICE_MISSING`, `SERVICE_VERSION_UPDATE_REQUIRED`, or `SERVICE_DISABLED`, then set

    jumpConfig.tryWebViewAuthenticationWhenGooglePlayIsUnavailable = false;

or for an Engage Only integration

	engage.setTryWebViewAuthenticationWhenGooglePlayIsUnavailable(false);

After the dialog is dismissed the library will call your `onFailure` method. If the error is something else, then the SDK will fail silently and attempt sign in using a WebView.

### Signing out of the Google+ SDK

You should provide your users with the ability to sign out of the Google+ SDK. This will allow them to sign in again with a different account if they have multiple Google+ accounts on their Android device. To do that call

	Jump.signOutNativeGooglePlus(MainActivity.this);

or for an Engage Only integration

	engage.signOutNativeGooglePlus(MainActivity.this);

### Revoking the access token and disconnecting the app

To revoke the access token and disconnect the app from a user’s Google+ account call

    Jump.revokeAndDisconnectNativeGooglePlus(MainActivity.this);

or for an Engage Only integration

    engage.revokeAndDisconnectNativeGooglePlus(MainActivity.this);
