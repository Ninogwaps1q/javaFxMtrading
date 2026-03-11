package Model;

public class CheckoutPayment {

    private final String method;
    private final String reference;

    public CheckoutPayment(String method, String reference) {
        this.method = method;
        this.reference = reference;
    }

    public String getMethod() {
        return method;
    }

    public String getReference() {
        return reference;
    }
}
