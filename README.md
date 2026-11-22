# Stripe Payment Gateway POC --- Developer Onboarding Guide

## 1. Purpose of this project

This project is a proof of concept (POC) demonstrating how a React
frontend and a Quarkus backend integrate with **Stripe Checkout** to
accept payments.

The goal is to help a new developer understand the intended payment
flow, run the application locally, make a test payment using Stripe's
test environment, and know where to look when something goes wrong.

This POC uses **Stripe-hosted Checkout**. The customer is redirected
from the React application to a Stripe-hosted payment page. The
application does not collect or store card numbers or CVVs.

### What the POC demonstrates

-   A React UI can request payment initiation from the Quarkus backend.
-   The Quarkus backend creates a Stripe Checkout Session using the
    Stripe Java SDK.
-   The backend returns the Checkout URL to React.
-   React redirects the browser to Stripe Checkout.
-   Stripe sends an event to the backend through a webhook.
-   The backend verifies the webhook signature and processes the event.

> **Important:** A browser redirect to the success page is not proof
> that a payment succeeded. The backend must use a verified Stripe event
> (and, where appropriate, retrieve the latest Stripe resource) to
> establish payment status.

------------------------------------------------------------------------

## 2. High-level architecture

``` text
+---------------------+       1. Start checkout       +----------------------+
|                     | ----------------------------> |                      |
|    React frontend   |                               |    Quarkus backend   |
|    localhost:5173   | <---------------------------- |    localhost:8080    |
|                     |       3. Checkout URL         |                      |
+---------------------+                               +----------+-----------+
          |                                                       |
          | 4. Redirect browser                                   | 2. Create
          v                                                       |    Checkout
+---------------------+                                           |    Session
|                     |                                           v
|   Stripe Checkout   | <-----------------------------------------+
|   (hosted payment)  |
|                     |
+----------+----------+
           |
           | 5. Customer completes/cancels payment
           v
+---------------------+       6. Webhook event        +----------------------+
|                     | ----------------------------> |                      |
|       Stripe        |                               |  Quarkus webhook API |
|                     |                               |  Verify + process    |
+---------------------+                               +----------------------+
```

For local development, **Stripe CLI** forwards webhook events from
Stripe to the local Quarkus endpoint.

``` text
Stripe -> Stripe CLI -> http://localhost:8080/api/payments/webhook
```
```stripe listen \
>   --events checkout.session.completed,checkout.session.expired,checkout.session.async_payment_succeeded,checkout.session.async_payment_failed \
>   --forward-to localhost:8080/api/payments/webhook
```

### Component responsibilities
  -----------------------------------------------------------------------
Component                           Responsibility
  ----------------------------------- -----------------------------------
React                               Display the payment page, call the
backend, redirect to Checkout, and
display the result

Quarkus                             Keep Stripe credentials, create
Checkout Sessions, expose
application APIs, verify and
process webhooks

Stripe Checkout                     Collect payment details and run the
hosted checkout experience

Stripe                              Process test payments and emit
payment events

Stripe CLI                          Forward Stripe webhook events to
the local machine

Database (future enhancement)       Persist orders, payment attempts,
webhook event IDs, and payment
statuses
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## 3. Repository and application structure

The exact package names and filenames may differ in your checkout. A
typical structure is:

``` text
payment-poc/
├── frontend/
│   ├── package.json
│   ├── src/
│   │   ├── App.jsx
│   │   ├── App.css
│   │   └── ...
│   └── ...
│
└── backend/
    ├── pom.xml
    └── src/
        └── main/
            ├── java/
            │   └── .../
            │       ├── resource/
            │       │   ├── PaymentResource.java
            │       │   └── StripeWebhookResource.java
            │       └── ...
            └── resources/
                └── application.properties
```

If the frontend and backend are in separate repositories, clone both and
follow the setup instructions in each repository's README.

------------------------------------------------------------------------

## 4. Prerequisites

Install or obtain access to the following:

  -----------------------------------------------------------------------
Tool / Access           Purpose                 How to verify
  ----------------------- ----------------------- -----------------------
Git                     Clone the source code   `git --version`

Java 21 (or the version Run Quarkus             `java -version`
specified by the                                
project)

Maven wrapper           Build/run the backend   `./mvnw -version`

Node.js and npm         Run React               `node --version` and
`npm --version`

Stripe account with     Obtain test API         Sign in to Stripe
test-mode access        credentials and inspect Dashboard
test transactions

Stripe CLI              Forward webhook events  `stripe --version`
locally
  -----------------------------------------------------------------------

Use the Java and Quarkus versions declared by the checked-in `pom.xml`
and project configuration as the source of truth. If this project uses
Quarkus 3.34.x, use a compatible JDK (for example, Java 21) and do not
independently change the Quarkus version while setting up.

### Stripe access

Ask the project owner for access to the team's Stripe test/sandbox
environment. Do not share secret keys in chat, source control,
screenshots, or documentation.

In the Stripe Dashboard, ensure you are working in **test mode / the
intended sandbox**. Locate the API keys under the Developers section.

The backend requires the Stripe **test secret key** (`sk_test_...`). A
publishable key (`pk_test_...`) is not required for this hosted Checkout
redirect flow because React receives the Checkout URL from the backend.

------------------------------------------------------------------------

## 5. Configure the backend

### 5.1 Configure application properties

The backend needs the Stripe secret key, the webhook signing secret, and
the frontend origin. Keep secrets outside source control.

Example `src/main/resources/application.properties`:

``` properties
# Stripe credentials are supplied through environment variables.
stripe.secret-key=${STRIPE_SECRET_KEY}
stripe.webhook-secret=${STRIPE_WEBHOOK_SECRET}

# React development server origin.
app.frontend.url=${FRONTEND_URL:http://localhost:5173}

# Local development CORS. Adjust if the frontend uses another port.
quarkus.http.cors.enabled=true
quarkus.http.cors.origins=http://localhost:5173
quarkus.http.cors.methods=GET,POST,OPTIONS
quarkus.http.cors.headers=Content-Type,Authorization
```

The code must use the same configuration property names. If the existing
project uses different names, keep the existing convention and update
this guide accordingly.

### 5.2 Set environment variables on Linux

In the terminal used to start Quarkus:

``` bash
export STRIPE_SECRET_KEY='sk_test_replace_with_your_test_secret'
export FRONTEND_URL='http://localhost:5173'
```

The webhook signing secret is obtained in section 7 after starting
Stripe CLI:

``` bash
export STRIPE_WEBHOOK_SECRET='whsec_replace_with_cli_signing_secret'
```

These `export` commands apply only to the current shell session. Set
them again in a new terminal, or use an approved local secrets manager /
ignored environment file. Never commit a file containing real
credentials.

### 5.3 Stripe Java SDK dependency

The backend should declare the Stripe Java SDK in `pom.xml`. The Quarkus
platform BOM manages Quarkus platform dependencies; it does not
automatically manage the Stripe SDK version.

Example:

``` xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>${stripe.version}</version>
</dependency>
```

Ensure the `stripe.version` property is defined and that the version is
the one approved for this project. Avoid copying an SDK version from an
old example without checking the project's current `pom.xml`.

### 5.4 Build the backend

From the backend project directory:

``` bash
./mvnw clean package
```

To run in development mode:

``` bash
./mvnw quarkus:dev
```

The application should be available at:

``` text
http://localhost:8080
```

If the project uses a different port or Maven profile, use the
checked-in configuration.

------------------------------------------------------------------------

## 6. Configure and run the React frontend

From the frontend project directory:

``` bash
npm install
npm run dev
```

The Vite development server commonly starts at:

``` text
http://localhost:5173
```

Use the URL printed by the development server if it selects another
port.

### Frontend-to-backend connectivity

The React payment action should call the Quarkus Checkout API. The
backend responds with a Checkout Session ID and hosted Checkout URL.
React then redirects the browser:

``` javascript
window.location.href = data.checkoutUrl;
```

The frontend must not contain or receive the Stripe secret key. It must
not determine or override the authoritative amount charged; the backend
should calculate the amount from trusted order/product data.

If the browser reports a CORS error, check:

1.  The exact React origin shown in the browser address bar.
2.  The `quarkus.http.cors.origins` value.
3.  That the Quarkus application was restarted after configuration
    changes.
4.  That the request is being made to the expected backend URL.

------------------------------------------------------------------------

## 7. Set up Stripe CLI and webhook forwarding

Stripe needs a publicly reachable endpoint to deliver webhooks. During
local development, Stripe CLI provides a secure forwarding tunnel to the
local Quarkus endpoint.

### 7.1 Install Stripe CLI

Install Stripe CLI using the official instructions for your operating
system:

<https://docs.stripe.com/stripe-cli>

Verify installation:

``` bash
stripe --version
```

### 7.2 Authenticate

``` bash
stripe login
```

Complete the browser-based authentication using the intended Stripe
account.

### 7.3 Start forwarding snapshot events

For the current Checkout POC, use the snapshot-event webhook handler:

``` bash
stripe listen \
  --events checkout.session.completed,checkout.session.expired,checkout.session.async_payment_succeeded,checkout.session.async_payment_failed \
  --forward-to localhost:8080/api/payments/webhook
```

Stripe CLI prints a webhook signing secret beginning with `whsec_`. Copy
it into the `STRIPE_WEBHOOK_SECRET` environment variable used by the
Quarkus process.

Keep the Stripe CLI process running while testing. If you stop and
restart the listener, check whether it has provided a new signing secret
and update the backend environment if necessary.

### 7.4 Why snapshot events?

Snapshot events include a snapshot of the Stripe object in the event
payload. The current POC's traditional webhook handling uses Stripe's
Java SDK webhook signature verification and deserializes the event's
data object.

Thin events have a different payload and processing model. Do not
forward thin events to this snapshot handler. If the project later
adopts thin events, implement the corresponding thin-event parsing and
retrieval flow separately.

### 7.5 Webhook endpoint responsibilities

The webhook endpoint should:

1.  Read the **raw request body** without modifying or re-serializing
    it.
2.  Read the `Stripe-Signature` request header.
3.  Verify the signature using the webhook signing secret.
4.  Identify the event type.
5.  Extract the relevant Checkout Session / PaymentIntent details.
6.  Correlate the event to the internal order.
7.  Apply an idempotent payment/order state transition.
8.  Return a successful HTTP response after the event has been durably
    handled (or durably accepted for asynchronous processing).

Do not trust an unverified webhook payload. Do not mark an order as paid
solely because the customer was redirected to the success URL.

------------------------------------------------------------------------

## 8. Run the complete local setup

Use three terminals.

### Terminal 1 --- Quarkus backend

``` bash
cd backend

export STRIPE_SECRET_KEY='sk_test_replace_with_your_test_secret'
export STRIPE_WEBHOOK_SECRET='whsec_replace_with_cli_signing_secret'
export FRONTEND_URL='http://localhost:5173'

./mvnw quarkus:dev
```

The webhook signing secret must first be obtained by starting Stripe
CLI. You can start the CLI in Terminal 3, copy the secret, then start or
restart Quarkus with the value set.

### Terminal 2 --- React frontend

``` bash
cd frontend
npm install
npm run dev
```

Open the local URL printed by Vite, typically `http://localhost:5173`.

### Terminal 3 --- Stripe CLI

``` bash
stripe listen \
  --events checkout.session.completed,checkout.session.expired,checkout.session.async_payment_succeeded,checkout.session.async_payment_failed \
  --forward-to localhost:8080/api/payments/webhook
```

Ensure the `whsec_...` value printed by this command is the same value
supplied to the running Quarkus process.

### Test a successful payment

1.  Open the React application.

2.  Select the product / click **Pay Now**.

3.  Confirm that the browser is redirected to Stripe Checkout.

4.  Use a Stripe test card, for example:

    Field               Test value
      ------------------- -----------------------
    Card number         `4242 4242 4242 4242`
    Expiry              Any future date
    CVC                 Any three digits
    ZIP / postal code   Any valid test value

5.  Complete the payment.

6.  Confirm the browser returns to the configured success URL.

7.  Check the Stripe CLI terminal for the forwarded webhook event.

8.  Check the Quarkus logs for successful signature verification and
    event processing.

9.  Inspect the payment in the Stripe Dashboard while in test mode.

Stripe test cards and scenario-specific test data are documented here:

<https://docs.stripe.com/testing>

Use only Stripe test card numbers in test mode. Never use real card
details for this POC.

------------------------------------------------------------------------

## 9. Important Stripe concepts

A new developer should understand these identifiers and how they relate:

  -----------------------------------------------------------------------
Stripe object                       Purpose
  ----------------------------------- -----------------------------------
Checkout Session (`cs_...`)         Represents the hosted Checkout
interaction

PaymentIntent (`pi_...`)            Represents the payment lifecycle
and payment attempt

Event (`evt_...`)                   Represents an event Stripe sends to
a webhook

Customer (`cus_...`)                Represents a Stripe customer, if
customer management is used
  -----------------------------------------------------------------------

A Checkout Session can reference a PaymentIntent. Store the internal
order ID in Stripe metadata when creating the Checkout Session so the
backend can correlate webhook events to the correct order.

Do not confuse the Checkout Session ID with the PaymentIntent ID. They
are different Stripe objects and identifiers.

### Relevant event types

  --------------------------------------------------------------------------------
Event                                        Meaning / handling
  -------------------------------------------- -----------------------------------
`checkout.session.completed`                 Checkout completed. Inspect
`payment_status`; completion does
not always mean funds are paid for
delayed payment methods.

`checkout.session.expired`                   Checkout Session expired before
completion.

`checkout.session.async_payment_succeeded`   A delayed payment method later
succeeded.

`checkout.session.async_payment_failed`      A delayed payment method later
failed.

`payment_intent.payment_failed`              A PaymentIntent payment attempt
failed.
  --------------------------------------------------------------------------------

Choose and document a consistent state model. For example, an order can
remain `PENDING` while a payment is processing; a payment attempt can be
`FAILED` while the order remains eligible for another attempt.

------------------------------------------------------------------------

## 10. Troubleshooting

  -----------------------------------------------------------------------
Symptom                             Things to check
  ----------------------------------- -----------------------------------
Quarkus fails to start because      Confirm `STRIPE_SECRET_KEY` is set
Stripe key is missing               in the same shell/process that
starts Quarkus.

Stripe returns an authentication    Confirm the key is a test secret
error                               key from the intended Stripe
account and has not been copied
with extra characters.

Checkout API returns an error       Check Quarkus logs, Stripe SDK
dependency resolution, request
parameters, amount and currency.

React cannot call Quarkus           Check backend availability, API
URL, CORS origin and port.

Browser does not redirect           Inspect the Checkout API response
and confirm `checkoutUrl` is
present.

Stripe CLI cannot authenticate      Run `stripe login` and verify the
intended account.

Webhook is not received             Keep `stripe listen` running and
check the forwarding URL and port.

Webhook signature verification      Confirm the backend uses the
fails                               `whsec_` secret printed by the
active listener, not the Stripe API
secret key.

Webhook handler cannot deserialize  Confirm the endpoint is receiving
the event                           snapshot events and that the Stripe
Java SDK supports the event/API
version.

Success page appears but backend    Check the webhook delivery,
still says pending                  signature verification, event
processing and payment status. The
redirect alone does not update
payment status.

Duplicate event delivery            This is expected to be possible.
Implement idempotency using the
Stripe event ID and transactional
state updates.
  -----------------------------------------------------------------------

Useful commands:

``` bash
# Check Java
java -version

# Check Node and npm
node --version
npm --version

# Build Quarkus
./mvnw clean package

# Inspect Stripe SDK dependency
./mvnw dependency:tree -Dincludes=com.stripe:stripe-java

# Check Stripe CLI
stripe --version

# View recent Stripe events (CLI command availability may depend on CLI version)
stripe events list
```

------------------------------------------------------------------------
