package io.cockroachdb.poolproxy.test.shell;

import java.util.List;
import java.util.stream.IntStream;

import io.cockroachdb.poolproxy.test.domain.Address;
import io.cockroachdb.poolproxy.test.domain.Customer;
import io.cockroachdb.poolproxy.test.domain.Order;
import io.cockroachdb.poolproxy.test.domain.Product;
import io.cockroachdb.poolproxy.test.shell.util.RandomData;

public abstract class EntityDoubles {
    private EntityDoubles() {
    }

    public static Product newProduct(int x) {
        return Product.builder()
                .withName("CockroachDB Unleashed - Edition " + x)
                .withPrice(RandomData.randomMoneyBetween(20, 500, 2))
                .withSku(RandomData.randomWord(12))
                .withQuantity(RandomData.randomInt(3000, 9000))
                .build();
    }

    public static Customer newCustomer(int x) {
        String fn = RandomData.randomFirstName();
        String ln = RandomData.randomLastName();

        String un = fn.toLowerCase() + "." + ln.toLowerCase() + x;
        String email = un + "@example.com";

        return Customer.builder()
                .withFirstName(fn)
                .withLastName(ln)
                .withUserName(un)
                .withEmail(email)
                .withAddress(newAddress())
                .build();
    }

    public static Address newAddress() {
        return Address.builder()
                .setAddress1(RandomData.randomWord(15))
                .setAddress2(RandomData.randomWord(15))
                .setCity(RandomData.randomCity())
                .setPostcode(RandomData.randomZipCode())
                .setCountry(RandomData.randomCountry())
                .build();
    }

    public static Order newOrder(List<Customer> customers,
                                 List<Product> products,
                                 int numItems) {

        Order.Builder ob = Order.builder()
                .withCustomer(RandomData.selectRandom(customers));

        IntStream.rangeClosed(1, numItems).forEach(value -> {
            Product product = RandomData.selectRandom(products);
            ob.andOrderItem()
                    .withProduct(product)
                    .withUnitPrice(product.getPrice())
                    .withQuantity(RandomData.randomInt(2, 10))
                    .then();
        });

        return ob.build();
    }

}
