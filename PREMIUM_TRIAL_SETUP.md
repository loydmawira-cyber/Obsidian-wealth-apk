# Premium 3-Month Trial Setup

The Android app now looks up the `premium_annual` subscription, selects a Google Play offer whose first free phase is three months (`P3M`) when one is returned for the customer, and shows the localized paid renewal price before launching Google Play Billing.

## Google Play Console prerequisite

The app cannot create a subscription offer in Play Console. Before release, configure and activate an eligible three-month free-trial offer for the existing subscription product ID **`premium_annual`**:

1. Confirm the `premium_annual` subscription has an active annual base plan and a valid paid renewal price for the intended countries.
2. Add a free-trial offer to that subscription with a **three-month** free phase, followed by the paid base-plan renewal.
3. Set the customer eligibility and countries/regions in which the offer should be available, then activate the offer.
4. Verify the offer through a Play internal-testing track with a tester account eligible under those rules.

The app reads the offer and pricing from Google Play at runtime. When Google Play does not return the trial offer (for example, because it is inactive, unavailable in the account's country, or the customer is ineligible), the app does not claim that the trial is available and instead shows the paid terms returned by Play. Google Play presents the final eligibility, billing date, and price for confirmation before purchase.

## Release validation

- Confirm the app's product ID matches the Play Console subscription ID: `premium_annual`.
- Test both an eligible new account (three months free, then the annual renewal price) and an account that is not trial-eligible (paid terms only).
- Confirm the Premium entitlement is restored on app restart for a purchased subscription and removed after Play no longer returns an active purchase.
