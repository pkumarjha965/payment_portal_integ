import React from "react";
import "./App.css";

function Payment() {

    const initiatePayment = async () => {
        try {
            const response = await fetch(
                "http://localhost:8080/api/payments/checkout",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    }
                }
            );

            if (!response.ok) {
                throw new Error("Payment initialization failed");
            }

            const data = await response.json();

            window.location.href = data.checkoutUrl;

        } catch (error) {
            console.error(error);
            alert("Unable to initiate payment");
        }
    };

    return (
        <div className="app-container">
            <div className="payment-card">

                <div className="payment-header">
                    <h1>Payment Portal</h1>
                    <p>Secure checkout powered by Stripe</p>
                </div>

                <div className="product-section">
                    <div className="product-name">
                        POC Product
                    </div>

                    <div className="product-description">
                        Sample product for payment gateway integration.
                    </div>

                    <div className="price-row">
                        <span className="price-label">Total Amount</span>
                        <span className="price-value">$100.00</span>
                    </div>
                </div>

                <button
                    className="pay-button"
                    onClick={initiatePayment}
                >
                    Pay Now
                </button>

                <div className="secure-note">
                    🔒 Secure payment processed by Stripe
                </div>

            </div>
        </div>
    );
}

export default Payment;