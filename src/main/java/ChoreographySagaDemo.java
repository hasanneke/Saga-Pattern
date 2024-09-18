import java.util.*;

// Event class
class Event {
    String type;
    String orderId;
    boolean success;

    Event(String type, String orderId, boolean success) {
        this.type = type;
        this.orderId = orderId;
        this.success = success;
    }
}

// EventListener interface
interface EventListener {
    void onEvent(Event event);
}

// EventBus class
class EventBus {
    private Map<String, List<EventListener>> listeners = new HashMap<>();

    public void subscribe(String eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public void publish(Event event) {
        List<EventListener> eventListeners = listeners.get(event.type);
        if (eventListeners != null) {
            for (EventListener listener : eventListeners) {
                listener.onEvent(event);
            }
        }
    }
}

// Order class
class Order {
    String id;
    String status;

    Order() {
        this.id = UUID.randomUUID().toString();
        this.status = "CREATED";
    }
}

// OrderService
class OrderService implements EventListener {
    private EventBus eventBus;

    OrderService(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.subscribe("PAYMENT_PROCESSED", this);
        this.eventBus.subscribe("INVENTORY_RESERVED", this);
        this.eventBus.subscribe("SHIPPING_SCHEDULED", this);
    }

    void createOrder(Order order) {
        System.out.println("Creating order: " + order.id);
        order.status = "PENDING";
        eventBus.publish(new Event("ORDER_CREATED", order.id, true));
    }

    @Override
    public void onEvent(Event event) {
        if (!event.success) {
            System.out.println("Cancelling order due to failed " + event.type + ": " + event.orderId);
            eventBus.publish(new Event("ORDER_CANCELLED", event.orderId, true));
        } else if (event.type.equals("SHIPPING_SCHEDULED")) {
            System.out.println("Order completed: " + event.orderId);
        }
    }
}

// PaymentService
class PaymentService implements EventListener {
    private EventBus eventBus;

    PaymentService(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.subscribe("ORDER_CREATED", this);
        this.eventBus.subscribe("ORDER_CANCELLED", this);
    }

    @Override
    public void onEvent(Event event) {
        if (event.type.equals("ORDER_CREATED")) {
            processPayment(event.orderId);
        } else if (event.type.equals("ORDER_CANCELLED")) {
            refundPayment(event.orderId);
        }
    }

    private void processPayment(String orderId) {
        System.out.println("Processing payment for order: " + orderId);
        boolean paymentSuccess = Math.random() < 0.8; // 80% success rate
        eventBus.publish(new Event("PAYMENT_PROCESSED", orderId, paymentSuccess));
    }

    private void refundPayment(String orderId) {
        System.out.println("Refunding payment for order: " + orderId);
    }
}

// InventoryService
class InventoryService implements EventListener {
    private EventBus eventBus;

    InventoryService(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.subscribe("PAYMENT_PROCESSED", this);
        this.eventBus.subscribe("ORDER_CANCELLED", this);
    }

    @Override
    public void onEvent(Event event) {
        if (event.type.equals("PAYMENT_PROCESSED") && event.success) {
            reserveInventory(event.orderId);
        } else if (event.type.equals("ORDER_CANCELLED")) {
            releaseInventory(event.orderId);
        }
    }

    private void reserveInventory(String orderId) {
        System.out.println("Reserving inventory for order: " + orderId);
        boolean inventorySuccess = Math.random() < 0.9; // 90% success rate
        eventBus.publish(new Event("INVENTORY_RESERVED", orderId, inventorySuccess));
    }

    private void releaseInventory(String orderId) {
        System.out.println("Releasing inventory for order: " + orderId);
    }
}

// ShippingService
class ShippingService implements EventListener {
    private EventBus eventBus;

    ShippingService(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.subscribe("INVENTORY_RESERVED", this);
        this.eventBus.subscribe("ORDER_CANCELLED", this);
    }

    @Override
    public void onEvent(Event event) {
        if (event.type.equals("INVENTORY_RESERVED") && event.success) {
            scheduleShipping(event.orderId);
        } else if (event.type.equals("ORDER_CANCELLED")) {
            cancelShipping(event.orderId);
        }
    }

    private void scheduleShipping(String orderId) {
        System.out.println("Scheduling shipping for order: " + orderId);
        boolean shippingSuccess = Math.random() < 0.95; // 95% success rate
        eventBus.publish(new Event("SHIPPING_SCHEDULED", orderId, shippingSuccess));
    }

    private void cancelShipping(String orderId) {
        System.out.println("Cancelling shipping for order: " + orderId);
    }
}

// Choreography-based SAGA demo
public class ChoreographySagaDemo {
    public static void main(String[] args) {
        EventBus eventBus = new EventBus();

        OrderService orderService = new OrderService(eventBus);
        PaymentService paymentService = new PaymentService(eventBus);
        InventoryService inventoryService = new InventoryService(eventBus);
        ShippingService shippingService = new ShippingService(eventBus);

        Order order = new Order();
        orderService.createOrder(order);

        // Wait for async operations to complete
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}