import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class BehavioralPatternsDemo {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void section(String title) {
        System.out.println("\n" + title);
    }

    // 1. Template Method. The algorithm order is fixed in the base class.
    static abstract class OrderWorkflow {
        final List<String> steps = new ArrayList<>();

        public final void process() {
            steps.add("validate");
            pay();
            deliver();
        }

        protected abstract void pay();
        protected abstract void deliver();
    }

    static final class DigitalOrderWorkflow extends OrderWorkflow {
        @Override protected void pay() { steps.add("online payment"); }
        @Override protected void deliver() { steps.add("email link"); }
    }

    static final class PickupOrderWorkflow extends OrderWorkflow {
        @Override protected void pay() { steps.add("cash on pickup"); }
        @Override protected void deliver() { steps.add("pickup point"); }
    }

    // 2. Mediator. Users exchange messages through a chat room.
    static final class ChatRoom {
        private final List<ChatUser> users = new ArrayList<>();

        void join(ChatUser user) { users.add(user); }

        void send(ChatUser sender, String message) {
            for (ChatUser user : users) {
                if (user != sender) {
                    user.receive(sender.name + ": " + message);
                }
            }
        }
    }

    static final class ChatUser {
        final String name;
        private final ChatRoom room;
        final List<String> inbox = new ArrayList<>();

        ChatUser(String name, ChatRoom room) {
            this.name = name;
            this.room = room;
            room.join(this);
        }

        void send(String message) { room.send(this, message); }
        void receive(String message) { inbox.add(message); }
    }

    // 3. Chain of Responsibility. A support ticket is routed by type.
    static abstract class SupportHandler {
        private SupportHandler next;

        SupportHandler setNext(SupportHandler handler) {
            this.next = handler;
            return handler;
        }

        String handle(String type) {
            if (supports(type)) {
                return handlerName();
            }
            return next != null ? next.handle(type) : "unhandled";
        }

        protected abstract boolean supports(String type);
        protected abstract String handlerName();
    }

    static final class GeneralSupport extends SupportHandler {
        @Override protected boolean supports(String type) { return "question".equals(type); }
        @Override protected String handlerName() { return "general support"; }
    }

    static final class PaymentSupport extends SupportHandler {
        @Override protected boolean supports(String type) { return "payment".equals(type); }
        @Override protected String handlerName() { return "payment support"; }
    }

    // 4. Observer. Subscribers are notified of inventory changes.
    interface StockObserver {
        void onStockChanged(String product, int quantity);
    }

    static final class Inventory {
        private final List<StockObserver> observers = new ArrayList<>();

        void subscribe(StockObserver observer) { observers.add(observer); }

        void setStock(String product, int quantity) {
            for (StockObserver observer : observers) {
                observer.onStockChanged(product, quantity);
            }
        }
    }

    // 5. Strategy. Delivery price is selected at runtime.
    interface DeliveryStrategy {
        int cost(int weight);
    }

    static final class PickupStrategy implements DeliveryStrategy {
        @Override public int cost(int weight) { return 0; }
    }

    static final class CourierStrategy implements DeliveryStrategy {
        @Override public int cost(int weight) { return 150 + weight * 20; }
    }

    static final class Checkout {
        private DeliveryStrategy strategy;

        Checkout(DeliveryStrategy strategy) { this.strategy = strategy; }
        void setStrategy(DeliveryStrategy strategy) { this.strategy = strategy; }
        int deliveryCost(int weight) { return strategy.cost(weight); }
    }

    // 6. Command. Each operation is represented by an object.
    interface Command {
        void execute();
    }

    static final class OrderDocument {
        String status = "NEW";
        void approve() { status = "APPROVED"; }
        void cancel() { status = "CANCELLED"; }
    }

    static final class ApproveOrder implements Command {
        private final OrderDocument order;
        ApproveOrder(OrderDocument order) { this.order = order; }
        @Override public void execute() { order.approve(); }
    }

    static final class CancelOrder implements Command {
        private final OrderDocument order;
        CancelOrder(OrderDocument order) { this.order = order; }
        @Override public void execute() { order.cancel(); }
    }

    static final class CommandInvoker {
        void run(Command command) { command.execute(); }
    }

    // 7. State. The response depends on the order's current state.
    interface OrderState {
        String name();
        OrderState next();
    }

    static final class NewState implements OrderState {
        @Override public String name() { return "NEW"; }
        @Override public OrderState next() { return new PaidState(); }
    }

    static final class PaidState implements OrderState {
        @Override public String name() { return "PAID"; }
        @Override public OrderState next() { return new ShippedState(); }
    }

    static final class ShippedState implements OrderState {
        @Override public String name() { return "SHIPPED"; }
        @Override public OrderState next() { return this; }
    }

    static final class OrderMachine {
        private OrderState state = new NewState();
        String status() { return state.name(); }
        void advance() { state = state.next(); }
    }

    // 8. Visitor. Discount calculation is separated from product classes.
    interface CartProduct {
        void accept(ProductVisitor visitor);
    }

    interface ProductVisitor {
        void visit(Book book);
        void visit(Electronics electronics);
    }

    static final class Book implements CartProduct {
        final int price;
        Book(int price) { this.price = price; }
        @Override public void accept(ProductVisitor visitor) { visitor.visit(this); }
    }

    static final class Electronics implements CartProduct {
        final int price;
        Electronics(int price) { this.price = price; }
        @Override public void accept(ProductVisitor visitor) { visitor.visit(this); }
    }

    static final class DiscountVisitor implements ProductVisitor {
        int totalDiscount;
        @Override public void visit(Book book) { totalDiscount += book.price / 10; }
        @Override public void visit(Electronics electronics) {
            totalDiscount += electronics.price / 20;
        }
    }

    // 9. Interpreter. Simple role-based access expression.
    interface RoleExpression {
        boolean interpret(String role);
    }

    static final class ExactRole implements RoleExpression {
        private final String expected;
        ExactRole(String expected) { this.expected = expected; }
        @Override public boolean interpret(String role) { return expected.equals(role); }
    }

    static final class OrRole implements RoleExpression {
        private final RoleExpression left;
        private final RoleExpression right;

        OrRole(RoleExpression left, RoleExpression right) {
            this.left = left;
            this.right = right;
        }

        @Override public boolean interpret(String role) {
            return left.interpret(role) || right.interpret(role);
        }
    }

    // 10. Iterator. A catalog exposes iteration without leaking storage.
    static final class ProductCatalog implements Iterable<String> {
        private final List<String> items = Arrays.asList("Book", "Laptop");

        @Override public Iterator<String> iterator() { return items.iterator(); }
    }

    // 11. Memento. A draft can return to a saved state.
    static final class DraftSnapshot {
        private final String text;
        DraftSnapshot(String text) { this.text = text; }
    }

    static final class DraftEditor {
        private String text;

        DraftEditor(String text) { this.text = text; }
        void edit(String nextText) { text = nextText; }
        String current() { return text; }
        DraftSnapshot save() { return new DraftSnapshot(text); }
        void restore(DraftSnapshot snapshot) { text = snapshot.text; }
    }

    public static void main(String[] args) {
        section("1. Template Method");
        OrderWorkflow digital = new DigitalOrderWorkflow();
        digital.process();
        OrderWorkflow pickup = new PickupOrderWorkflow();
        pickup.process();
        check(digital.steps.equals(Arrays.asList("validate", "online payment", "email link")),
                "Template Method");
        System.out.println(digital.steps + " / " + pickup.steps);

        section("2. Mediator");
        ChatRoom room = new ChatRoom();
        ChatUser simran = new ChatUser("Simran", room);
        ChatUser admin = new ChatUser("Admin", room);
        simran.send("Order question");
        check(admin.inbox.size() == 1 && simran.inbox.isEmpty(), "Mediator");
        System.out.println(admin.inbox);

        section("3. Chain of Responsibility");
        SupportHandler support = new GeneralSupport();
        support.setNext(new PaymentSupport());
        check("payment support".equals(support.handle("payment")), "Chain");
        check("unhandled".equals(support.handle("delivery")), "Chain unknown type");
        System.out.println(support.handle("question") + ", " + support.handle("payment"));

        section("4. Observer");
        Inventory inventory = new Inventory();
        List<String> events = new ArrayList<>();
        inventory.subscribe((product, amount) -> events.add(product + "=" + amount));
        inventory.setStock("Laptop", 5);
        check(events.equals(Arrays.asList("Laptop=5")), "Observer");
        System.out.println(events);

        section("5. Strategy");
        Checkout checkout = new Checkout(new PickupStrategy());
        check(checkout.deliveryCost(2) == 0, "Pickup strategy");
        checkout.setStrategy(new CourierStrategy());
        check(checkout.deliveryCost(2) == 190, "Courier strategy");
        System.out.println("Courier delivery: " + checkout.deliveryCost(2));

        section("6. Command");
        OrderDocument document = new OrderDocument();
        CommandInvoker invoker = new CommandInvoker();
        invoker.run(new ApproveOrder(document));
        check("APPROVED".equals(document.status), "Command approve");
        invoker.run(new CancelOrder(document));
        check("CANCELLED".equals(document.status), "Command cancel");
        System.out.println("Status: " + document.status);

        section("7. State");
        OrderMachine machine = new OrderMachine();
        check("NEW".equals(machine.status()), "State new");
        machine.advance();
        check("PAID".equals(machine.status()), "State paid");
        machine.advance();
        check("SHIPPED".equals(machine.status()), "State shipped");
        System.out.println("Status: " + machine.status());

        section("8. Visitor");
        DiscountVisitor visitor = new DiscountVisitor();
        new Book(400).accept(visitor);
        new Electronics(2000).accept(visitor);
        check(visitor.totalDiscount == 140, "Visitor");
        System.out.println("Discount: " + visitor.totalDiscount);

        section("9. Interpreter");
        RoleExpression access = new OrRole(new ExactRole("admin"), new ExactRole("manager"));
        check(access.interpret("admin") && access.interpret("manager"), "Interpreter access");
        check(!access.interpret("guest"), "Interpreter deny");
        System.out.println("Manager access: " + access.interpret("manager"));

        section("10. Iterator");
        int productCount = 0;
        for (String product : new ProductCatalog()) {
            productCount++;
            System.out.println("Product: " + product);
        }
        check(productCount == 2, "Iterator");

        section("11. Memento");
        DraftEditor editor = new DraftEditor("Original order note");
        DraftSnapshot snapshot = editor.save();
        editor.edit("Modified order note");
        editor.restore(snapshot);
        check("Original order note".equals(editor.current()), "Memento");
        System.out.println("Restored text: " + editor.current());

        System.out.println("\nAll 11 behavioral pattern checks passed");
    }
}
