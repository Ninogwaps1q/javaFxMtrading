package UserController;

import Model.CheckoutPayment;
import Model.VoucherDiscount;
import config.VoucherDataUtil;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public final class PaymentDialogUtil {

    private static final String METHOD_COD = "Cash on Delivery (COD)";
    private static final String METHOD_GCASH = "GCash";
    private static final String METHOD_CARD = "Credit Card";
    private static final double LARGE_DIALOG_WIDTH = 680;
    private static final double LARGE_DIALOG_HEIGHT = 560;
    private static final double LARGE_INPUT_WIDTH = 420;
    private static final double LARGE_INPUT_HEIGHT = 40;
    private static final int GCASH_NUMBER_MAX_DIGITS = 11;
    private static final int CARD_NUMBER_MAX_DIGITS = 19;
    private static final int CVV_MAX_DIGITS = 4;
    private static final int EXPIRY_MAX_DIGITS = 4;
    private static final DateTimeFormatter REFERENCE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

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
        configureDigitInput(gcashNumberField, GCASH_NUMBER_MAX_DIGITS);

        Label voucherCodeLabel = new Label("Voucher Code");
        TextField voucherCodeField = new TextField();
        voucherCodeField.setPromptText("Optional voucher code");
        configureLargeInput(voucherCodeField);
        voucherCodeField.setTextFormatter(new javafx.scene.control.TextFormatter<String>(change -> {
            change.setText(change.getText().toUpperCase(Locale.ENGLISH));
            return change;
        }));

        Button validateVoucherBtn = new Button("Check Voucher");
        validateVoucherBtn.getStyleClass().add("btn-secondary");

        Label voucherSummaryLabel = new Label("No voucher applied.");
        voucherSummaryLabel.getStyleClass().add("hint-text");
        voucherSummaryLabel.setWrapText(true);

        Label referenceLabel = new Label("Reference No.");
        TextField referenceField = new TextField();
        referenceField.setPromptText("Auto-generated");
        configureLargeInput(referenceField);
        referenceField.setEditable(false);
        referenceField.setFocusTraversable(false);

        Label cardNameLabel = new Label("Cardholder Name");
        TextField cardNameField = new TextField();
        cardNameField.setPromptText("Name on card");
        configureLargeInput(cardNameField);

        Label cardNumberLabel = new Label("Card Number");
        TextField cardNumberField = new TextField();
        cardNumberField.setPromptText("1234 5678 9012 3456");
        configureLargeInput(cardNumberField);
        configureCardNumberInput(cardNumberField);

        Label expiryLabel = new Label("Expiry (MM/YY)");
        TextField expiryField = new TextField();
        expiryField.setPromptText("MM/YY");
        configureLargeInput(expiryField);
        configureExpiryInput(expiryField);

        Label cvvLabel = new Label("CVV");
        PasswordField cvvField = new PasswordField();
        cvvField.setPromptText("3 or 4 digits");
        configureLargeInput(cvvField);
        configureDigitInput(cvvField, CVV_MAX_DIGITS);

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

        HBox voucherBox = new HBox(8, voucherCodeField, validateVoucherBtn);
        grid.add(voucherCodeLabel, 0, row);
        grid.add(voucherBox, 1, row++);
        grid.add(voucherSummaryLabel, 0, row++, 2, 1);

        grid.add(gcashNumberLabel, 0, row);
        grid.add(gcashNumberField, 1, row++);

        grid.add(referenceLabel, 0, row);
        grid.add(referenceField, 1, row++);

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
            boolean needsReference = gcash || card;

            setVisibleManaged(gcashNumberLabel, gcash);
            setVisibleManaged(gcashNumberField, gcash);
            setVisibleManaged(referenceLabel, needsReference);
            setVisibleManaged(referenceField, needsReference);

            setVisibleManaged(cardNameLabel, card);
            setVisibleManaged(cardNameField, card);
            setVisibleManaged(cardNumberLabel, card);
            setVisibleManaged(cardNumberField, card);
            setVisibleManaged(expiryLabel, card);
            setVisibleManaged(expiryField, card);
            setVisibleManaged(cvvLabel, card);
            setVisibleManaged(cvvField, card);

            if (needsReference) {
                referenceField.setText(generateReference(method));
            } else {
                referenceField.clear();
            }
            errorLabel.setText("");
        };

        methodCombo.valueProperty().addListener((obs, oldV, newV) -> refreshVisibility.run());
        refreshVisibility.run();

        Runnable updateVoucherState = () -> {
            VoucherDiscount voucher = VoucherDataUtil.validateVoucher(voucherCodeField.getText(), totalAmount);

            if (!voucher.isValid()) {
                voucherSummaryLabel.setText(voucher.getMessage());
                return;
            }

            if (!voucher.hasVoucher()) {
                voucherSummaryLabel.setText("No voucher applied. Payable total: " + formatMoney(totalAmount));
                return;
            }

            voucherSummaryLabel.setText(String.format(
                    Locale.ENGLISH,
                    "Voucher %s applied. Discount: %s | Payable total: %s",
                    voucher.getCode(),
                    formatMoney(voucher.getDiscountAmount()),
                    formatMoney(voucher.getPayableTotal())
            ));
        };
        validateVoucherBtn.setOnAction(e -> updateVoucherState.run());
        updateVoucherState.run();

        Node payButton = dialog.getDialogPane().lookupButton(payButtonType);
        final CheckoutPayment[] paymentHolder = new CheckoutPayment[1];

        payButton.addEventFilter(ActionEvent.ACTION, event -> {
            ValidationResult validation = validate(
                    methodCombo.getValue(),
                    gcashNumberField.getText(),
                    referenceField.getText(),
                    cardNameField.getText(),
                    cardNumberField.getText(),
                    expiryField.getText(),
                    cvvField.getText(),
                    voucherCodeField.getText(),
                    totalAmount);

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

    private static void configureDigitInput(TextField field, int maxDigits) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            String digits = newValue == null ? "" : newValue.replaceAll("\\D", "");
            if (digits.length() > maxDigits) {
                digits = digits.substring(0, maxDigits);
            }

            if (!digits.equals(newValue)) {
                field.setText(digits);
                field.positionCaret(digits.length());
            }
        });
    }

    private static void configureCardNumberInput(TextField field) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            String digits = newValue == null ? "" : newValue.replaceAll("\\D", "");
            if (digits.length() > CARD_NUMBER_MAX_DIGITS) {
                digits = digits.substring(0, CARD_NUMBER_MAX_DIGITS);
            }

            String formatted = formatCardDigits(digits);
            if (!formatted.equals(newValue)) {
                field.setText(formatted);
                field.positionCaret(formatted.length());
            }
        });
    }

    private static void configureExpiryInput(TextField field) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            String digits = newValue == null ? "" : newValue.replaceAll("\\D", "");
            if (digits.length() > EXPIRY_MAX_DIGITS) {
                digits = digits.substring(0, EXPIRY_MAX_DIGITS);
            }

            String formatted;
            if (digits.length() <= 2) {
                formatted = digits;
            } else {
                formatted = digits.substring(0, 2) + "/" + digits.substring(2);
            }

            if (!formatted.equals(newValue)) {
                field.setText(formatted);
                field.positionCaret(formatted.length());
            }
        });
    }

    private static ValidationResult validate(
            String method,
            String gcashNumber,
            String paymentReference,
            String cardholder,
            String cardNumber,
            String expiry,
            String cvv,
            String voucherCode,
            double grossTotal) {

        VoucherDiscount voucher = VoucherDataUtil.validateVoucher(voucherCode, grossTotal);
        if (!voucher.isValid()) {
            return ValidationResult.error(voucher.getMessage());
        }

        if (method == null || method.trim().isEmpty()) {
            return ValidationResult.error("Please choose a payment method.");
        }

        if (METHOD_COD.equals(method)) {
            return ValidationResult.ok(new CheckoutPayment(
                    "COD",
                    "Pay on delivery",
                    voucher.getCode(),
                    voucher.getGrossTotal(),
                    voucher.getDiscountAmount(),
                    voucher.getPayableTotal()));
        }

        if (METHOD_GCASH.equals(method)) {
            String normalized = normalizeGcashNumber(gcashNumber);
            if (normalized == null) {
                return ValidationResult.error("Enter a valid GCash number with exactly 11 digits.");
            }

            String ref = normalizeReference(paymentReference, METHOD_GCASH);
            return ValidationResult.ok(new CheckoutPayment(
                    "GCash",
                    ref,
                    voucher.getCode(),
                    voucher.getGrossTotal(),
                    voucher.getDiscountAmount(),
                    voucher.getPayableTotal()));
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

        String ref = normalizeReference(paymentReference, METHOD_CARD);
        return ValidationResult.ok(new CheckoutPayment(
                "Credit Card",
                ref,
                voucher.getCode(),
                voucher.getGrossTotal(),
                voucher.getDiscountAmount(),
                voucher.getPayableTotal()));
    }

    private static String normalizeGcashNumber(String input) {
        if (input == null) return null;
        String digits = input.replaceAll("\\D", "");

        if (digits.startsWith("09") && digits.length() == 11) {
            return digits;
        }

        return null;
    }

    private static String normalizeReference(String reference, String method) {
        String cleaned = reference == null ? "" : reference.trim().toUpperCase(Locale.ENGLISH);
        if (!cleaned.isEmpty()) {
            return cleaned;
        }
        return generateReference(method);
    }

    private static String generateReference(String method) {
        String prefix = METHOD_GCASH.equals(method) ? "GC" : "CC";
        String timestamp = LocalDateTime.now().format(REFERENCE_TIME_FORMAT);
        int randomPart = ThreadLocalRandom.current().nextInt(1300, 8000);
        return prefix + "-" + timestamp + "-" + randomPart;
    }

    private static String formatMoney(double amount) {
        return String.format(Locale.ENGLISH, "PHP %.2f", amount);
    }

    private static String formatCardDigits(String digits) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(' ');
            }
            formatted.append(digits.charAt(i));
        }
        return formatted.toString();
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
