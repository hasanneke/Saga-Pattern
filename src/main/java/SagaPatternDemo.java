import java.util.ArrayList;
import java.util.List;

// Order class
class OrOrder {
    private int id;
    private List<String> items;
    private String status;

    public OrOrder(int id, List<String> items) {
        this.id = id;
        this.items = items;
        this.status = "CREATED";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Order{id=" + id + ", items=" + items + ", status='" + status + "'}";
    }
}

// Interface for SAGA steps
interface SagaStep<T> {
    boolean execute(T context);
    void compensate(T context);
}

// Order Service
class OrOrderService implements SagaStep<OrOrder> {
    @Override
    public boolean execute(OrOrder order) {
        System.out.println("Creating order: " + order);
        order.setStatus("PENDING");
        return true;
    }

    @Override
    public void compensate(OrOrder order) {
        System.out.println("Cancelling order: " + order);
        order.setStatus("CANCELLED");
    }
}

// Inventory Service
class OrInventoryService implements SagaStep<OrOrder> {
    @Override
    public boolean execute(OrOrder order) {
        System.out.println("Reserving inventory for order: " + order);
        // Simulate inventory check
        return Math.random() < 0.95;
    }

    @Override
    public void compensate(OrOrder order) {
        System.out.println("Releasing inventory for order: " + order);
    }
}

// Payment Service
class OrPaymentService implements SagaStep<OrOrder> {
    @Override
    public boolean execute(OrOrder order) {
        System.out.println("Processing payment for order: " + order);
        // Simulate payment processing
        return Math.random() < 0.95;
    }

    @Override
    public void compensate(OrOrder order) {
        System.out.println("Refunding payment for order: " + order);
    }
}

// Shipping Service
class OrShippingService implements SagaStep<OrOrder> {
    @Override
    public boolean execute(OrOrder order) {
        System.out.println("Shipping order: " + order);
        order.setStatus("SHIPPED");
        return true;
    }

    @Override
    public void compensate(OrOrder order) {
        System.out.println("Cancelling shipment for order: " + order);
        order.setStatus("SHIPPING_CANCELLED");
    }
}

// SAGA Orchestrator
class OrderSaga {
    private List<SagaStep<OrOrder>> steps;
    private List<SagaStep<OrOrder>> executedSteps;

    public OrderSaga() {
        this.steps = new ArrayList<>();
        this.executedSteps = new ArrayList<>();

        // Add steps in order
        steps.add(new OrOrderService());
        steps.add(new OrInventoryService());
        steps.add(new OrPaymentService());
        steps.add(new OrShippingService());
    }

    public boolean execute(OrOrder order) {
        for (SagaStep<OrOrder> step : steps) {
            if (step.execute(order)) {
                executedSteps.add(step);
            } else {
                compensate(order);
                return false;
            }
        }
        return true;
    }

    private void compensate(OrOrder order) {
        for (int i = executedSteps.size() - 1; i >= 0; i--) {
            executedSteps.get(i).compensate(order);
        }
    }
}

// Main class to demonstrate the SAGA pattern
public class SagaPatternDemo {
    public static void main(String[] args) {
        List<String> items = List.of("Item1", "Item2");
        OrOrder order = new OrOrder(1, items);

        OrderSaga saga = new OrderSaga();
        boolean result = saga.execute(order);

        if (result) {
            System.out.println("Order processed successfully: " + order);
        } else {
            System.out.println("Order processing failed: " + order);
        }
    }
}