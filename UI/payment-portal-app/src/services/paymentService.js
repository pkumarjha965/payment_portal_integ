const API_URL = import.meta.env.VITE_API_BASE_URL;

export const createPaymentOrder = async () => {

  const response = await fetch(
    `${API_URL}/api/v1/payments/orders`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        invoiceId: "INV-1001"
      })
    }
  );

  if (!response.ok) {
    throw new Error("Unable to create payment order");
  }

  return response.json();
};


export const verifyPayment = async (paymentResponse, transactionId) => {

  const response = await fetch(
    `${API_URL}/api/v1/payments/verify`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        transactionId,
        razorpay_order_id: paymentResponse.razorpay_order_id,
        razorpay_payment_id: paymentResponse.razorpay_payment_id,
        razorpay_signature: paymentResponse.razorpay_signature
      })
    }
  );

  if (!response.ok) {
    throw new Error("Payment verification failed");
  }

  return response.json();
};