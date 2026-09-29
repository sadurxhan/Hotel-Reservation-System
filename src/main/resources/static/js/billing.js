document.addEventListener("DOMContentLoaded", function () {

    const paymentForm = document.getElementById("paymentForm");

    if (paymentForm) {
        setupPaymentPage();
    }

});


function setupPaymentPage() {

    const form = document.getElementById("paymentForm");

    const reservationId =
        document.getElementById("reservationId").value;

    const paymentMethod =
        document.getElementById("paymentMethod");

    const message =
        document.getElementById("paymentMessage");

    const createButton =
        document.getElementById("createPaymentButton");

    const mockSection =
        document.getElementById("mockPaymentSection");

    const mockPaymentId =
        document.getElementById("mockPaymentId");

    const successButton =
        document.getElementById("mockSuccessButton");

    const failureButton =
        document.getElementById("mockFailureButton");


    let currentPaymentId = null;


    form.addEventListener("submit", async function (event) {

        event.preventDefault();

        if (!paymentMethod.value) {

            showMessage(
                message,
                "Please select a payment method.",
                "error"
            );

            return;
        }


        createButton.disabled = true;

        createButton.textContent =
            "Creating Payment...";


        try {

            const response = await fetch(
                `/api/payments?reservationId=${encodeURIComponent(
                    reservationId
                )}&paymentMethod=${encodeURIComponent(
                    paymentMethod.value
                )}`,
                {
                    method: "POST"
                }
            );


            const data = await response.json();


            if (!response.ok) {

                throw new Error(
                    data.message ||
                    "Unable to create payment."
                );

            }


            currentPaymentId = data.paymentId;

            mockPaymentId.textContent =
                data.paymentId;


            mockSection.classList.remove("hidden");


            showMessage(
                message,
                "Payment created successfully. Status: " +
                data.paymentStatus,
                "success"
            );


            createButton.textContent =
                "Payment Created";


        } catch (error) {

            showMessage(
                message,
                error.message,
                "error"
            );

            createButton.disabled = false;

            createButton.textContent =
                "Continue to Payment";
        }

    });


    successButton.addEventListener(
        "click",
        async function () {

            if (!currentPaymentId) {
                return;
            }


            successButton.disabled = true;
            failureButton.disabled = true;


            try {

                const paymentAmount =
                    getDisplayedAmount();


                const transactionId =
                    "MOCK-" +
                    Date.now();


                const response = await fetch(
                    `/api/payments/${encodeURIComponent(
                        currentPaymentId
                    )}/paid?transactionId=${encodeURIComponent(
                        transactionId
                    )}&paidAmount=${encodeURIComponent(
                        paymentAmount
                    )}`,
                    {
                        method: "POST"
                    }
                );


                const data =
                    await response.json();


                if (!response.ok) {

                    throw new Error(
                        data.message ||
                        "Payment verification failed."
                    );

                }


                window.location.href =
                    `/payment/result?paymentId=${encodeURIComponent(
                        data.paymentId
                    )}`;


            } catch (error) {

                showMessage(
                    document.getElementById(
                        "paymentMessage"
                    ),
                    error.message,
                    "error"
                );

                successButton.disabled = false;
                failureButton.disabled = false;
            }

        }
    );


    failureButton.addEventListener(
        "click",
        async function () {

            if (!currentPaymentId) {
                return;
            }


            successButton.disabled = true;
            failureButton.disabled = true;


            try {

                const response = await fetch(
                    `/api/payments/${encodeURIComponent(
                        currentPaymentId
                    )}/failed`,
                    {
                        method: "POST"
                    }
                );


                const data =
                    await response.json();


                if (!response.ok) {

                    throw new Error(
                        data.message ||
                        "Unable to mark payment as failed."
                    );

                }


                window.location.href =
                    `/payment/result?paymentId=${encodeURIComponent(
                        data.paymentId
                    )}`;


            } catch (error) {

                showMessage(
                    document.getElementById(
                        "paymentMessage"
                    ),
                    error.message,
                    "error"
                );

                successButton.disabled = false;
                failureButton.disabled = false;
            }

        }
    );

}


/*
 * Reads the amount displayed on the payment page.
 *
 * This is ONLY for the temporary mock gateway.
 * PayHere will provide the verified payment amount
 * once PayHere Sandbox is integrated.
 */
function getDisplayedAmount() {

    const amountElements =
        document.querySelectorAll(
            ".payment-amount strong span"
        );


    if (amountElements.length === 0) {
        throw new Error(
            "Payment amount could not be found."
        );
    }


    const amountText =
        amountElements[0].textContent
            .trim()
            .replace(/,/g, "");


    const amount =
        parseFloat(amountText);


    if (Number.isNaN(amount)) {
        throw new Error(
            "Invalid payment amount."
        );
    }


    return amount.toFixed(2);
}


function showMessage(
    element,
    text,
    type
) {

    element.textContent = text;

    element.classList.remove(
        "hidden",
        "success",
        "error"
    );

    element.classList.add(type);

}