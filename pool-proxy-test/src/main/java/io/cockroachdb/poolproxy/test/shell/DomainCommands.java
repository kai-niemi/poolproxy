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
import org.springframework.util.Assert;

import io.cockroachdb.poolproxy.test.domain.Customer;
import io.cockroachdb.poolproxy.test.domain.Order;
import io.cockroachdb.poolproxy.test.domain.OrderItem;
import io.cockroachdb.poolproxy.test.domain.OrderSystem;
import io.cockroachdb.poolproxy.test.domain.Product;
import io.cockroachdb.poolproxy.test.util.AnsiConsole;
import io.cockroachdb.poolproxy.test.util.StreamingUtils;
import io.cockroachdb.poolproxy.test.util.TableUtils;

@ShellComponent
@ShellCommandGroup(CommandGroups.DOMAIN)
public class DomainCommands extends AbstractShellComponent {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AnsiConsole ansiConsole;

    @Autowired
    private OrderSystem orderSystem;

    @ShellMethod(value = "Delete all orders and catalog data", key = {"delete-all", "da"})
    public void deleteAll() {
        orderSystem.deleteAll();
    }

    @ShellMethod(value = "Delete all orders", key = {"delete-orders", "do"})
    public void deleteOrders() {
        orderSystem.deleteOrders();
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

    @ShellMethod(value = "Create orders in batches", key = {"create-orders", "co"})
    public void placeOrders(@ShellOption(help = "total number of orders", defaultValue = "512") int count,
                            @ShellOption(help = "batch size", defaultValue = "64") int batchSize,
                            @ShellOption(help = "number of order items per order", defaultValue = "4") int numItems) {

        final List<Customer> customers = orderSystem.listAllCustomers(count).toList();
        final List<Product> products = orderSystem.listAllProducts(count).toList();

        Assert.state(!customers.isEmpty(), "no customers");
        Assert.state(!products.isEmpty(), "no products");

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
            orderSystem.listAllOrdersWithDetails(pageSize).forEach(this::printOrderDetailPage);
        } else {
            printOrderPage(orderSystem.listAllOrders(pageSize));
        }
    }

    @ShellMethod(value = "Print order total", key = {"print-order-total", "pot"})
    public void printOrderTotal() {
        logger.info("Total order price: {}", orderSystem.getTotalOrderPrice());
    }

    @ShellMethod(value = "Print customers", key = {"print-customers", "pc"})
    public void printCustomers(@ShellOption(help = "page size", defaultValue = "64") int pageSize) {
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
