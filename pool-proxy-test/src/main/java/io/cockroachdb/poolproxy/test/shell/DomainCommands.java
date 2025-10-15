package io.cockroachdb.poolproxy.test.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.TableModel;

import io.cockroachdb.poolproxy.test.domain.Customer;
import io.cockroachdb.poolproxy.test.domain.Order;
import io.cockroachdb.poolproxy.test.domain.OrderSystem;
import io.cockroachdb.poolproxy.test.domain.Product;
import io.cockroachdb.poolproxy.test.shell.util.AnsiConsole;
import io.cockroachdb.poolproxy.test.shell.util.StreamingUtils;
import io.cockroachdb.poolproxy.test.shell.util.TableUtils;

@ShellComponent
@ShellCommandGroup(CommandGroups.DOMAIN)
public class DomainCommands extends AbstractShellComponent {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AnsiConsole ansiConsole;

    @Autowired
    private OrderSystem orderSystem;

    @ShellMethod(value = "Delete all orders and catalog data", key = {"clear-all", "ca"})
    public void clearAll() {
        orderSystem.clearAll();
    }

    @ShellMethod(value = "Create products in batches", key = {"create-products", "cp"})
    public void createProducts(@ShellOption(help = "total number of products", defaultValue = "512") int count,
                               @ShellOption(help = "batch size", defaultValue = "64") int batchSize) {
        StreamingUtils.chunkedStream(IntStream.rangeClosed(1, count).boxed(), batchSize)
                .forEach(chunk -> {
                    List<Product> batch = new ArrayList<>();
                    chunk.forEach(x -> batch.add(EntityDoubles.newProduct(x)));
                    orderSystem.createProducts(batch);
                });
    }

    @ShellMethod(value = "Create customers in batches", key = {"create-customers", "cc"})
    public void createCustomers(@ShellOption(help = "total number of customers", defaultValue = "512") int count,
                                @ShellOption(help = "batch size", defaultValue = "64") int batchSize) {
        StreamingUtils.chunkedStream(IntStream.rangeClosed(1, count).boxed(), batchSize)
                .forEach(chunk -> {
                    List<Customer> batch = new ArrayList<>();
                    chunk.forEach(x -> batch.add(EntityDoubles.newCustomer(x)));
                    orderSystem.createCustomers(batch);
                });
    }

    @ShellMethod(value = "Place orders in batches", key = {"create-order", "co"})
    public void placeOrders(@ShellOption(help = "total number of orders", defaultValue = "512") int count,
                            @ShellOption(help = "batch size", defaultValue = "64") int batchSize,
                            @ShellOption(help = "number of order items per order", defaultValue = "4") int numItems) {

        final List<Customer> customers = orderSystem.listAllCustomers(count).toList();
        final List<Product> products = orderSystem.listAllProducts(count).toList();

        StreamingUtils.chunkedStream(IntStream.rangeClosed(1, count).boxed(), batchSize)
                .forEach(chunk -> {
                    List<Order> batch = new ArrayList<>();
                    chunk.forEach(x -> batch.add(EntityDoubles.newOrder(customers, products, numItems)));
                    orderSystem.createOrders(batch);
                });
    }

    @ShellMethod(value = "Print order(s)", key = {"print-orders", "po"})
    public void printOrders(@ShellOption(help = "page size", defaultValue = "64") int pageSize,
                            @ShellOption(help = "include all order details", defaultValue = "false") boolean details) {
        if (details) {
            orderSystem.listAllOrdersWithDetails(pageSize).forEach(this::printWithDetails);
        } else {
            orderSystem.listAllOrders(pageSize).forEach(this::print);
        }
    }

    @ShellMethod(value = "Print order total", key = {"print-order-total", "pot"})
    public void printOrderTotal() {
        logger.info("Total order price: {}", orderSystem.getTotalOrderPrice());
    }

    @ShellMethod(value = "Print customers", key = {"print-customers", "pc"})
    public void printCustomers(@ShellOption(help = "page size", defaultValue = "64") int pageSize) {
        printPage(orderSystem.listAllCustomers(pageSize));
    }

    private void print(Order order) {
        Customer c = order.getCustomer();
        logger.info("""
                Order placed by: %s
                     Total cost: %s
                """.formatted(c.getUserName(), order.getTotalPrice()));
    }

    private void printWithDetails(Order order) {
        order.getOrderItems().forEach(orderItem -> {
            Product p = orderItem.getProduct();

            logger.info("""
                     Product name: %s
                    Product price: %s
                      Product sku: %s
                         Item qty: %s
                       Unit price: %s
                       Total cost: %s
                    """.formatted(
                    p.getName(),
                    p.getPrice(),
                    p.getSku(),
                    orderItem.getQuantity(),
                    orderItem.getUnitPrice(),
                    orderItem.totalCost()));
        });
    }

    private void printPage(Page<Customer> page) {
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

/*
    private void printPage(Page<Order> page) {
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
*/

}
