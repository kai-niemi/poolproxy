package io.cockroachdb.pool.proxy.demo.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.shell.jline.tui.table.TableModel;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import io.cockroachdb.pool.proxy.demo.domain.Customer;
import io.cockroachdb.pool.proxy.demo.domain.Order;
import io.cockroachdb.pool.proxy.demo.domain.OrderItem;
import io.cockroachdb.pool.proxy.demo.domain.OrderSystem;
import io.cockroachdb.pool.proxy.demo.domain.Product;
import io.cockroachdb.pool.proxy.demo.util.AnsiConsole;
import io.cockroachdb.pool.proxy.demo.util.StreamingUtils;
import io.cockroachdb.pool.proxy.demo.util.TableUtils;

@Component
public class DomainCommands extends AbstractShellComponent {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AnsiConsole ansiConsole;

    @Autowired
    private OrderSystem orderSystem;

    @Command(description = "Delete all orders and catalog data",
            name = {"delete all"},
            alias = "da")
    public void deleteAll() {
        orderSystem.deleteAll();
    }

    @Command(description = "Delete all orders",
            name = {"delete orders"},
            alias = "do")
    public void deleteOrders() {
        orderSystem.deleteOrders();
    }

    @Command(description = "Create products in batches",
            name = {"create products"},
            alias = "cp")
    public void createProducts(@Option(description = "total number of products", defaultValue = "512",
                                       longName = "count") Integer count,
                               @Option(description = "batch size", defaultValue = "64",
                                       longName = "batchSize") Integer batchSize) {
        StreamingUtils.chunkedStream(IntStream.rangeClosed(1, count).boxed(), batchSize)
                .forEach(chunk -> {
                    List<Product> batch = new ArrayList<>();
                    chunk.forEach(x -> batch.add(Doubles.newProduct(x)));

                    orderSystem.createProducts(batch);
                });
    }

    @Command(description = "Create customers in batches",
            name = {"create customers"},
            alias = "cc")
    public void createCustomers(@Option(description = "total number of customers", defaultValue = "512",
                                        longName = "count") Integer count,
                                @Option(description = "batch size", defaultValue = "64",
                                        longName = "batchSize") Integer batchSize) {
        StreamingUtils.chunkedStream(IntStream.rangeClosed(1, count).boxed(), batchSize)
                .forEach(chunk -> {
                    List<Customer> batch = new ArrayList<>();
                    chunk.forEach(x -> batch.add(Doubles.newCustomer(x)));

                    orderSystem.createCustomers(batch);
                });
    }

    @Command(description = "Create orders in batches",
            name = {"create orders"},
            alias = "co")
    public void placeOrders(@Option(description = "total number of orders", defaultValue = "512",
                                    longName = "count") Integer count,
                            @Option(description = "batch size", defaultValue = "64",
                                    longName = "batchSize") Integer batchSize,
                            @Option(description = "number of order items per order", defaultValue = "4",
                                    longName = "items") Integer numItems) {

        final List<Customer> customers = orderSystem.listAllCustomers(count).toList();
        final List<Product> products = orderSystem.listAllProducts(count).toList();

        Assert.state(!customers.isEmpty(), "no customers");
        Assert.state(!products.isEmpty(), "no products");

        StreamingUtils.chunkedStream(IntStream.rangeClosed(1, count).boxed(), batchSize)
                .forEach(chunk -> {
                    List<Order> batch = new ArrayList<>();
                    chunk.forEach(x -> batch.add(Doubles.newOrder(customers, products, numItems)));

                    orderSystem.createOrders(batch);
                });
    }

    @Command(description = "Print order(s)",
            name = {"print orders"},
            alias = "po")
    public void printOrders(@Option(description = "page size", defaultValue = "64",
                                    longName = "pageSize") Integer pageSize,
                            @Option(description = "include all order details", defaultValue = "false",
                                    longName = "details") Boolean details) {
        if (details) {
            orderSystem.listAllOrdersWithDetails(pageSize).forEach(this::printOrderDetailPage);
        } else {
            printOrderPage(orderSystem.listAllOrders(pageSize));
        }
    }

    @Command(description = "Print order total",
            name = {"print order-total"},
            alias = "pot")
    public void printOrderTotal() {
        logger.info("Total order price: {}", orderSystem.getTotalOrderPrice());
    }

    @Command(description = "Print customers",
            name = {"print customers"},
            alias = "pc")
    public void printCustomers(@Option(description = "page size",
            defaultValue = "64",
            longName = "pageSize") Integer pageSize) {
        printCustomerPage(orderSystem.listAllCustomers(pageSize));
    }

    private void printCustomerPage(Page<Customer> page) {
        ansiConsole.cyan(TableUtils.prettyPrint(
                new TableModel() {
                    @Override
                    public int getRowCount() {
                        return page.getNumberOfElements() + 1;
                    }

                    @Override
                    public int getColumnCount() {
                        return 5;
                    }

                    @Override
                    public Object getValue(int row, int column) {
                        if (row == 0) {
                            switch (column) {
                                case 0 -> {
                                    return "#";
                                }
                                case 1 -> {
                                    return "First Name";
                                }
                                case 2 -> {
                                    return "Last Name";
                                }
                                case 3 -> {
                                    return "City";
                                }
                                case 4 -> {
                                    return "Id";
                                }
                            }
                            return "??";
                        }

                        Customer customer = page.getContent().get(row - 1);
                        switch (column) {
                            case 0 -> {
                                return row;
                            }
                            case 1 -> {
                                return customer.getFirstName();
                            }
                            case 2 -> {
                                return customer.getLastName();
                            }
                            case 3 -> {
                                return customer.getAddress().getCity();
                            }
                            case 4 -> {
                                return customer.getId();
                            }
                        }
                        return "??";
                    }
                }));
    }

    private void printOrderPage(Page<Order> page) {
        ansiConsole.cyan(TableUtils.prettyPrint(
                new TableModel() {
                    @Override
                    public int getRowCount() {
                        return page.getNumberOfElements() + 1;
                    }

                    @Override
                    public int getColumnCount() {
                        return 4;
                    }

                    @Override
                    public Object getValue(int row, int column) {
                        if (row == 0) {
                            switch (column) {
                                case 0 -> {
                                    return "#";
                                }
                                case 1 -> {
                                    return "Id";
                                }
                                case 2 -> {
                                    return "Customer";
                                }
                                case 3 -> {
                                    return "Order Total";
                                }
                            }
                            return "??";
                        }

                        Order order = page.getContent().get(row - 1);
                        switch (column) {
                            case 0 -> {
                                return row;
                            }
                            case 1 -> {
                                return order.getId();
                            }
                            case 2 -> {
                                return order.getCustomer().getEmail();
                            }
                            case 3 -> {
                                return order.getTotalPrice();
                            }
                        }
                        return "??";
                    }
                }));
    }


    private void printOrderDetailPage(Order order) {
        List<OrderItem> orderItems = order.getOrderItems();

        ansiConsole.yellow("%s".formatted(order.getId())).nl();

        ansiConsole.cyan(TableUtils.prettyPrint(
                new TableModel() {
                    @Override
                    public int getRowCount() {
                        return orderItems.size() + 1;
                    }

                    @Override
                    public int getColumnCount() {
                        return 5;
                    }

                    @Override
                    public Object getValue(int row, int column) {
                        if (row == 0) {
                            switch (column) {
                                case 0 -> {
                                    return "#";
                                }
                                case 1 -> {
                                    return "Product";
                                }
                                case 2 -> {
                                    return "Unit Price";
                                }
                                case 3 -> {
                                    return "Qty";
                                }
                                case 4 -> {
                                    return "Total";
                                }
                            }
                            return "??";
                        }

                        OrderItem orderItem = orderItems.get(row - 1);

                        switch (column) {
                            case 0 -> {
                                return row;
                            }
                            case 1 -> {
                                return orderItem.getProduct().getName();
                            }
                            case 2 -> {
                                return orderItem.getUnitPrice();
                            }
                            case 3 -> {
                                return orderItem.getQuantity();
                            }
                            case 4 -> {
                                return orderItem.totalCost();
                            }
                        }
                        return "??";
                    }
                }));
    }
}
