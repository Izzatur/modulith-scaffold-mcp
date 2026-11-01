package com.example.scaffoldmcp.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Example code (and its tests) for each design pattern, written in a Spring-friendly style.
 * Everything is generated into @@MODULE@@/internal/<pattern>, so Spring Modulith keeps it private to the module.
 */
final class PatternTemplates {

    private PatternTemplates() {
    }

    static List<GeneratedFile> files(DesignPattern pattern, String basePackage, String module) {
        String pkg = basePackage + "." + module + ".internal." + pattern.folder();
        String dir = pkg.replace('.', '/');
        Map<String, String> tokens = Map.of("PKG", pkg, "MODULE", module);

        List<GeneratedFile> out = new ArrayList<>();
        for (Map.Entry<String, String> source : sources(pattern).entrySet()) {
            boolean isTest = source.getKey().endsWith("Test.java");
            String root = isTest ? "src/test/java/" : "src/main/java/";
            out.add(new GeneratedFile(root + dir + "/" + source.getKey(),
                    ProjectGenerator.render(source.getValue(), tokens)));
        }
        return out;
    }

    private static Map<String, String> sources(DesignPattern pattern) {
        return switch (pattern) {
            case STRATEGY -> strategy();
            case FACTORY -> factory();
            case OBSERVER -> observer();
            case DECORATOR -> decorator();
        };
    }

    // ---------------------------------------------------------------- Strategy

    private static Map<String, String> strategy() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("DiscountStrategy.java", """
                package @@PKG@@;

                import java.math.BigDecimal;

                /** Strategy: one interface, several interchangeable algorithms. */
                public interface DiscountStrategy {

                    /** The key callers use to choose this strategy. */
                    String name();

                    BigDecimal apply(BigDecimal amount);
                }
                """);
        m.put("NoDiscount.java", """
                package @@PKG@@;

                import java.math.BigDecimal;

                import org.springframework.stereotype.Component;

                @Component("@@MODULE@@NoDiscount")
                public class NoDiscount implements DiscountStrategy {

                    @Override
                    public String name() {
                        return "none";
                    }

                    @Override
                    public BigDecimal apply(BigDecimal amount) {
                        return amount;
                    }
                }
                """);
        m.put("TenPercentDiscount.java", """
                package @@PKG@@;

                import java.math.BigDecimal;
                import java.math.RoundingMode;

                import org.springframework.stereotype.Component;

                @Component("@@MODULE@@TenPercentDiscount")
                public class TenPercentDiscount implements DiscountStrategy {

                    @Override
                    public String name() {
                        return "ten-percent";
                    }

                    @Override
                    public BigDecimal apply(BigDecimal amount) {
                        return amount.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP);
                    }
                }
                """);
        m.put("DiscountService.java", """
                package @@PKG@@;

                import java.math.BigDecimal;
                import java.util.List;
                import java.util.Map;
                import java.util.function.Function;
                import java.util.stream.Collectors;

                import org.springframework.stereotype.Service;

                /** Context: picks a strategy by name. Spring injects every DiscountStrategy bean. */
                @Service("@@MODULE@@DiscountService")
                public class DiscountService {

                    private final Map<String, DiscountStrategy> strategies;

                    public DiscountService(List<DiscountStrategy> strategies) {
                        this.strategies = strategies.stream()
                                .collect(Collectors.toUnmodifiableMap(DiscountStrategy::name, Function.identity()));
                    }

                    public BigDecimal price(String strategyName, BigDecimal amount) {
                        DiscountStrategy strategy = strategies.get(strategyName);
                        if (strategy == null) {
                            throw new IllegalArgumentException("Unknown discount strategy: " + strategyName);
                        }
                        return strategy.apply(amount);
                    }
                }
                """);
        m.put("DiscountServiceTest.java", """
                package @@PKG@@;

                import static org.junit.jupiter.api.Assertions.assertEquals;
                import static org.junit.jupiter.api.Assertions.assertThrows;

                import java.math.BigDecimal;
                import java.util.List;

                import org.junit.jupiter.api.Test;

                class DiscountServiceTest {

                    private final DiscountService service =
                            new DiscountService(List.of(new NoDiscount(), new TenPercentDiscount()));

                    @Test
                    void noDiscountKeepsTheAmount() {
                        assertEquals(new BigDecimal("100.00"), service.price("none", new BigDecimal("100.00")));
                    }

                    @Test
                    void tenPercentTakesTenPercentOff() {
                        assertEquals(new BigDecimal("90.00"), service.price("ten-percent", new BigDecimal("100.00")));
                    }

                    @Test
                    void unknownStrategyIsRejected() {
                        assertThrows(IllegalArgumentException.class, () -> service.price("nope", BigDecimal.TEN));
                    }
                }
                """);
        return m;
    }

    // ----------------------------------------------------------------- Factory

    private static Map<String, String> factory() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("ChannelType.java", """
                package @@PKG@@;

                public enum ChannelType {
                    EMAIL, SMS
                }
                """);
        m.put("NotificationChannel.java", """
                package @@PKG@@;

                /** Product: what every channel can do. */
                public interface NotificationChannel {

                    String send(String message);
                }
                """);
        m.put("EmailChannel.java", """
                package @@PKG@@;

                public class EmailChannel implements NotificationChannel {

                    @Override
                    public String send(String message) {
                        return "email:" + message;
                    }
                }
                """);
        m.put("SmsChannel.java", """
                package @@PKG@@;

                public class SmsChannel implements NotificationChannel {

                    @Override
                    public String send(String message) {
                        return "sms:" + message;
                    }
                }
                """);
        m.put("ChannelFactory.java", """
                package @@PKG@@;

                import java.util.Objects;

                import org.springframework.stereotype.Component;

                /** Factory: callers ask for a type and never name the concrete class. */
                @Component("@@MODULE@@ChannelFactory")
                public class ChannelFactory {

                    public NotificationChannel create(ChannelType type) {
                        Objects.requireNonNull(type, "type must not be null");
                        return switch (type) {
                            case EMAIL -> new EmailChannel();
                            case SMS -> new SmsChannel();
                        };
                    }
                }
                """);
        m.put("ChannelFactoryTest.java", """
                package @@PKG@@;

                import static org.junit.jupiter.api.Assertions.assertEquals;
                import static org.junit.jupiter.api.Assertions.assertThrows;
                import static org.junit.jupiter.api.Assertions.assertTrue;

                import org.junit.jupiter.api.Test;

                class ChannelFactoryTest {

                    private final ChannelFactory factory = new ChannelFactory();

                    @Test
                    void createsAnEmailChannel() {
                        NotificationChannel channel = factory.create(ChannelType.EMAIL);
                        assertTrue(channel instanceof EmailChannel);
                        assertEquals("email:hello", channel.send("hello"));
                    }

                    @Test
                    void createsASmsChannel() {
                        NotificationChannel channel = factory.create(ChannelType.SMS);
                        assertTrue(channel instanceof SmsChannel);
                        assertEquals("sms:hello", channel.send("hello"));
                    }

                    @Test
                    void nullTypeIsRejected() {
                        assertThrows(NullPointerException.class, () -> factory.create(null));
                    }
                }
                """);
        return m;
    }

    // ---------------------------------------------------------------- Observer

    private static Map<String, String> observer() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("ItemChangedEvent.java", """
                package @@PKG@@;

                /** The event observers react to. */
                public record ItemChangedEvent(String itemId, String change) {
                }
                """);
        m.put("ChangeNotifier.java", """
                package @@PKG@@;

                import org.springframework.context.ApplicationEventPublisher;
                import org.springframework.stereotype.Service;

                /** Subject: publishes events and does not know who listens. */
                @Service("@@MODULE@@ChangeNotifier")
                public class ChangeNotifier {

                    private final ApplicationEventPublisher events;

                    public ChangeNotifier(ApplicationEventPublisher events) {
                        this.events = events;
                    }

                    public void itemChanged(String itemId, String change) {
                        events.publishEvent(new ItemChangedEvent(itemId, change));
                    }
                }
                """);
        m.put("AuditListener.java", """
                package @@PKG@@;

                import java.util.List;
                import java.util.concurrent.CopyOnWriteArrayList;

                import org.springframework.context.event.EventListener;
                import org.springframework.stereotype.Component;

                /** Observer: reacts to ItemChangedEvent. Add more listeners without touching the notifier. */
                @Component("@@MODULE@@AuditListener")
                public class AuditListener {

                    private final List<String> entries = new CopyOnWriteArrayList<>();

                    @EventListener
                    public void on(ItemChangedEvent event) {
                        entries.add(event.itemId() + ":" + event.change());
                    }

                    public List<String> entries() {
                        return List.copyOf(entries);
                    }
                }
                """);
        m.put("ChangeNotifierTest.java", """
                package @@PKG@@;

                import static org.junit.jupiter.api.Assertions.assertEquals;

                import java.util.ArrayList;
                import java.util.List;

                import org.junit.jupiter.api.Test;
                import org.springframework.context.ApplicationEventPublisher;

                class ChangeNotifierTest {

                    @Test
                    void publishesAnEventWithTheChange() {
                        List<Object> published = new ArrayList<>();
                        ApplicationEventPublisher publisher = published::add;

                        new ChangeNotifier(publisher).itemChanged("item-1", "renamed");

                        assertEquals(List.of(new ItemChangedEvent("item-1", "renamed")), published);
                    }

                    @Test
                    void listenerRecordsWhatItObserves() {
                        AuditListener listener = new AuditListener();

                        listener.on(new ItemChangedEvent("item-1", "renamed"));

                        assertEquals(List.of("item-1:renamed"), listener.entries());
                    }
                }
                """);
        return m;
    }

    // --------------------------------------------------------------- Decorator

    private static Map<String, String> decorator() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("TextProcessor.java", """
                package @@PKG@@;

                /** Component: the interface both the real object and the decorators share. */
                public interface TextProcessor {

                    String process(String input);
                }
                """);
        m.put("PlainText.java", """
                package @@PKG@@;

                /** The base object. It returns the input unchanged. */
                public class PlainText implements TextProcessor {

                    @Override
                    public String process(String input) {
                        return input;
                    }
                }
                """);
        m.put("TextProcessorDecorator.java", """
                package @@PKG@@;

                /** Base decorator: holds the wrapped object. */
                public abstract class TextProcessorDecorator implements TextProcessor {

                    protected final TextProcessor delegate;

                    protected TextProcessorDecorator(TextProcessor delegate) {
                        this.delegate = delegate;
                    }
                }
                """);
        m.put("TrimDecorator.java", """
                package @@PKG@@;

                public class TrimDecorator extends TextProcessorDecorator {

                    public TrimDecorator(TextProcessor delegate) {
                        super(delegate);
                    }

                    @Override
                    public String process(String input) {
                        return delegate.process(input).trim();
                    }
                }
                """);
        m.put("UpperCaseDecorator.java", """
                package @@PKG@@;

                import java.util.Locale;

                public class UpperCaseDecorator extends TextProcessorDecorator {

                    public UpperCaseDecorator(TextProcessor delegate) {
                        super(delegate);
                    }

                    @Override
                    public String process(String input) {
                        return delegate.process(input).toUpperCase(Locale.ROOT);
                    }
                }
                """);
        m.put("TextProcessorTest.java", """
                package @@PKG@@;

                import static org.junit.jupiter.api.Assertions.assertEquals;

                import org.junit.jupiter.api.Test;

                class TextProcessorTest {

                    @Test
                    void plainTextReturnsTheInput() {
                        assertEquals("  hi ", new PlainText().process("  hi "));
                    }

                    @Test
                    void decoratorsCanBeCombined() {
                        TextProcessor processor = new UpperCaseDecorator(new TrimDecorator(new PlainText()));

                        assertEquals("HI", processor.process("  hi "));
                    }

                    @Test
                    void eachDecoratorWorksAlone() {
                        assertEquals("hi", new TrimDecorator(new PlainText()).process("  hi "));
                        assertEquals("  HI ", new UpperCaseDecorator(new PlainText()).process("  hi "));
                    }
                }
                """);
        return m;
    }
}
