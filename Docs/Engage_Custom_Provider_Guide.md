#Engage Custom Provider Guide

This guide describes the process of configuring custom OpenID and custom SAML providers into the Engage
library. This guide assumes you have already completed either the `Engage_Only_Integration_Guide.md` or the
`Jump_Integration_Guide.md`.

##The Big Picture

1. Gather configuration details
2. Configure and initialize the library
3. Begin authentication

##Gather Configuration Details

For each custom provider you wish to configure gather the following configuration details

* Provider ID -- a short string which will be used to refer to the custom provider. E.g. the provider ID for
  Yahoo! is "yahoo". This is used only in the context of the Android app, and can be any arbitrary value you choose.
* Friendly name -- a string representing the user-facing name of the provider. E.g. the friendly name for
  Yahoo! is "Yahoo!".

###Custom Open ID

In addition to the configuration details above you will need:

* The OpenID identifier of your custom OpenID provider. For example
  `https://my-custom-openid-provider.com/example-openid-identifier`.
* Optionally, a custom "opx_blob" parameter for use with Janrain Identity Services' OpenID providers.

### Custom SAML

In addition to the configuration details above you will need:

* The name of the SAML implementation in Engage for your custom SAML provider.

##Configure and Initialize the Library

###Engage Only Integration

Construct a `Map<String, JRDictionary` similar to this example. The field names in the JRDictionary are
important (that is, the field for friendly name must be "friendly_name".) The keys in the map are the
"Provider ID"s.

        Map<String, JRDictionary> customProviders = new HashMap<String, JRDictionary>();

        JRDictionary openIdProvider = new JRDictionary();
        openIdProvider.put("friendly_name", "Example Custom OpenID Provider");
        openIdProvider.put("openid_identifier", "https://my-custom-openid-provider.com/example-openid-identifier");
        openIdProvider.put("opx_blob", "some_blob_for_opx"); // This is an optional field
        openIdProvider.put("icon_resource_id", R.drawable.openIdIcon);  // This is an optional field

        customProviders.put("open_id_provider", openIdProvider);

        JRDictionary samlProvider = new JRDictionary();
        samlProvider.put("friendly_name", "Example Custom SAML Provider");
        samlProvider.put("saml_provider", "the_name_of_the_engage_implementation_of_the_saml_provider");
        samlProvider.put("icon_resource_id", R.drawable.samlIcon); // This is an optional field

        customProviders.put("saml_provider", samlProvider);

Then pass the map in as an argument to `JREngage.initInstance`.

        engage = JREngage.initInstance(context, "your-engage-app-id", "your-token-url", delegate,
                                       customProviders);

###Jump Integration

`JumpConfig` provides convenience methods for adding custom providers.

        JumpConfig jumpConfig = new JumpConfig();
        ... // Your Jump configuration

        jumpConfig.addCustomOpenIdProvider("open_id_provider", "Example Custom OpenID Provider",
                                           "https://my-custom-openid-provider.com/example-openid-identifier",
                                           "some_blob_for_opx", R.drawable.openIdIcon);

        jumpConfig.addCustomSamlProvider("saml_provider", "Example Custom SAML Provider",
                                         "the_name_of_the_engage_implementation_of_the_saml_provider",
                                         R.drawable.samlIcon);

Then initialize Jump as normal.

        Jump.init(this, jumpConfig);

###Icons

You can add a 30x30 provider icon for you custom providers by passing the icon's resource id in as an
argument to `addCustomSamlProvider` or `addCustomOpenIdProvider` for Jump integrations, or in the JRDictionary
with the key `open_id_provider` for Engage only integrations.

##Begin Authentication
Begin authentication as in the Integration guides.
