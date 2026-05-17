package io.quarkiverse.ironjacamar.reactive.it;

public class OrderEvent {

    public String orderId;
    public int quantity;

    public OrderEvent() {
    }

    public OrderEvent(String orderId, int quantity) {
        this.orderId = orderId;
        this.quantity = quantity;
    }
}
