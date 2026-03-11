package UserController;

import Model.CheckoutPayment;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public final class PaymentDialogUtil {

    private static final String METHOD_COD = "Cash on Delivery (COD)";
    private static final String METHOD_GCASH = "GCash";
    private static final String METHOD_CARD = "Credit Card";
    private static final double LARGE_DIALOG_WIDTH = 680;
    private static final double LARGE_DIALOG_HEIGHT = 560;
    private static final double LARGE_INPUT_WIDTH = 420;
    private static final double LARGE_INPUT_HEIGHT = 40;

    private PaymentDialogUtil() {
    }

    public static CheckoutPayment showPaymentDialog(double totalAmount) {
        Dialog<CheckoutPayment> dialog = new Dialog<>();
        dialog.setTitle("Payment Details");
        dialog.setHeaderText("Total Amount: PHP " + String.format("%.2f", totalAmount));
        dialog.getDialogPane().getStylesheets().add(
                PaymentDialogUtil.class.getResource("/css/user.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("payment-dialog-pane");
        dialog.getDialogPane().setPrefSize(LARGE_DIALOG_WIDTH, LARGE_DIALOG_HEIGHT);

        ButtonType payButtonType = new ButtonType("Pay Now", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(payButtonType, ButtonType.CANCEL);

        ComboBox<String> methodCombo = new ComboBox<>(FXCollections.observableArrayList(
                METHOD_COD, METHOD_GCASH, METHOD_CARD));
        methodCombo.setPrefWidth(LARGE_INPUT_WIDTH);
        methodCombo.setPrefHeight(LARGE_INPUT_HEIGHT);
        methodCombo.getStyleClass().add("payment-dialog-input");
        methodCombo.setValue(METHOD_COD);

        Label gcashNumberLabel = new Label("GCash Number");
        TextField gcashNumberField = new TextField();
        gcashNumberField.setPromptText("09123456789");
        configureLargeInput(gcashNumberField);

        Label gcashRefLabel = new Label("Reference No.");
        TextField gcashRefField = new TextField();
        gcashRefField.setPromptText("Transaction reference");
        configureLargeInput(gcashRefField);

        Label cardNameLabel = new Label("Cardholder Name");
        TextField cardNameField = new TextField();
        cardNameField.setPromptText("Name on card");
        configureLargeInput(cardNameField);

        Label cardNumberLabel = new Label("Card Number");
        TextField cardNumberField = new TextField();
        cardNumberField.setPromptText("1234 5678 9012 3456");
        configureLargeInput(cardNumberField);

        Label expiryLabel = new Label("Expiry (MM/YY)");
        TextField expiryField = new TextField();
        expiryField.setPromptText("MM/YY");
        configureLargeInput(expiryField);

        Label cvvLabel = new Label("CVV");
        PasswordField cvvField = new PasswordField();
        cvvField.setPromptText("3 or 4 digits");
        configureLargeInput(cvvField);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("payment-error");
        errorLabel.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(10, 0, 0, 0));

        int row = 0;
        grid.add(new Label("Method"), 0, row);
        grid.add(methodCombo, 1, row++);

        grid.add(gcashNumberLabel, 0, row);
        grid.add(gcashNumberField, 1, row++);

        grid.add(gcashRefLabel, 0, row);
        grid.add(gcashRefField, 1, row++);

        grid.add(cardNameLabel, 0, row);
        grid.add(cardNameField, 1, row++);

        grid.add(cardNumberLabel, 0, row);
        grid.add(cardNumberField, 1, row++);

        grid.add(expiryLabel, 0, row);
        grid.add(expiryField, 1, row++);

        grid.add(cvvLabel, 0, row);
        grid.add(cvvField, 1, row++);

        grid.add(errorLabel, 0, row, 2, 1);

        dialog.getDialogPane().setContent(grid);

        Runnable refreshVisibility = () -> {
            String method = methodCombo.getValue();
            boolean gcash = METHOD_GCASH.equals(method);
            boolean card = METHOD_CARD.equals(method);

            setVisibleManaged(gcashNumberLabel, gcash);
            setVisibleManaged(gcashNumberField, gcash);
            setVisibleManaged(gcashRefLabel, gcash);
            setVisibleManaged(gcashRefField, gcash);

            setVisibleManaged(cardNameLabel, card);
            setVisibleManaged(cardNameField, card);
            setVisibleManaged(cardNumberLabel, card);
            setVisibleManaged(cardNumberField, card);
            setVisibleManaged(expiryLabel, card);
            setVisibleManaged(expiryField, card);
            setVisibleManaged(cvvLabel, card);
            setVisibleManaged(cvvField, card);

            errorLabel.setText("");
        };

        methodCombo.valueProperty().addListener((obs, oldV, newV) -> refreshVisibility.run());
        refreshVisibility.run();

        Node payButton = dialog.getDialogPane().lookupButton(payButtonType);
        final CheckoutPayment[] paymentHolder = new CheckoutPayment[1];

        payButton.addEventFilter(ActionEvent.ACTION, event -> {
            ValidationResult validation = validate(
                    methodCombo.getValue(),
                    gcashNumberField.getText(),
                    gcashRefField.getText(),
                    cardNameField.getText(),
                    cardNumberField.getText(),
                    expiryField.getText(),
                    cvvField.getText());

            if (!validation.isValid()) {
                errorLabel.setText(validation.errorMessage);
                event.consume();
                return;
            }

            errorLabel.setText("");
            paymentHolder[0] = validation.payment;
        });

        dialog.setResultConverter(button -> button == payButtonType ? paymentHolder[0] : null);

        Optional<CheckoutPayment> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private static void setVisibleManaged(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private static void configureLargeInput(TextField field) {
        field.setPrefWidth(LARGE_INPUT_WIDTH);
        field.setPrefHeight(LARGE_INPUT_HEIGHT);
        field.getStyleClass().add("payment-dialog-input");
    }

    private static ValidationResult validate(
            String method,
            String gcashNumber,
            String gcashReference,
            String cardholder,
            String cardNumber,
            String expiry,
            String cvv) {

        if (method == null || method.trim().isEmpty()) {
            return ValidationResult.error("Please choose a payment method.");
        }

        if (METHOD_COD.equals(method)) {
            return ValidationResult.ok(new CheckoutPayment("COD", "Pay on delivery"));
        }

        if (METHOD_GCASH.equals(method)) {
            String normalized = normalizeGcashNumber(gcashNumber);
            if (normalized == null) {
                return ValidationResult.error("Enter a valid GCash number (example: 09123456789).");
            }

            String ref = gcashReference == null ? "" : gcashReference.trim();
            if (!ref.matches("^[A-Za-z0-9]{6,24}$")) {
                return ValidationResult.error("Enter a valid GCash reference (6-24 letters/numbers).");
            }

            String masked = normalized.substring(0, 4) + "****" + normalized.substring(8);
            return ValidationResult.ok(new CheckoutPayment("GCash", masked + " | Ref " + ref.toUpperCase(Locale.ENGLISH)));
        }

        String cleanName = cardholder == null ? "" : cardholder.trim();
        if (cleanName.length() < 3) {
            return ValidationResult.error("Cardholder name must be at least 3 characters.");
        }

        String digits = cardNumber == null ? "" : cardNumber.replaceAll("\\D", "");
        if (digits.length() < 13 || digits.length() > 19 || !passesLuhn(digits)) {
            return ValidationResult.error("Enter a valid credit card number.");
        }

        String cleanExpiry = expiry == null ? "" : expiry.trim();
        Matcher matcher = Pattern.compile("^(0[1-9]|1[0-2])\\s*/\\s*(\\d{2})$").matcher(cleanExpiry);
        if (!matcher.matches()) {
            return ValidationResult.error("Expiry must be in MM/YY format.");
        }

        int month = Integer.parseInt(matcher.group(1));
        int year = 2000 + Integer.parseInt(matcher.group(2));
        YearMonth expiryMonth = YearMonth.of(year, month);
        if (expiryMonth.isBefore(YearMonth.now())) {
            return ValidationResult.error("Card expiry date is already past.");
        }

        String cleanCvv = cvv == null ? "" : cvv.trim();
        if (!cleanCvv.matches("^\\d{3,4}$")) {
            return ValidationResult.error("CVV must be 3 or 4 digits.");
        }

        String maskedCard = "**** **** **** " + digits.substring(digits.length() - 4);
        return ValidationResult.ok(new CheckoutPayment("Credit Card", maskedCard));
    }

    private static String normalizeGcashNumber(String input) {
        if (input == null) return null;
        String digits = input.replaceAll("\\D", "");

        if (digits.startsWith("639") && digits.length() == 12) {
            digits = "0" + digits.substring(2);
        }

        if (digits.startsWith("09") && digits.length() == 11) {
            return digits;
        }

        return null;
    }

    private static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean alternate = false;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }

    private static final class ValidationResult {

        private final CheckoutPayment payment;
        private final String errorMessage;

        private ValidationResult(CheckoutPayment payment, String errorMessage) {
            this.payment = payment;
            this.errorMessage = errorMessage;
        }

        private static ValidationResult ok(CheckoutPayment payment) {
            return new ValidationResult(payment, null);
        }

        private static ValidationResult error(String message) {
            return new ValidationResult(null, message);
        }

        private boolean isValid() {
            return payment != null;
        }
    }
}
