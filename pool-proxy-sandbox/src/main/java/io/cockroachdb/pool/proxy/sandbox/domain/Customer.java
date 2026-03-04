package io.cockroachdb.pool.proxy.sandbox.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
@NamedQueries({
        @NamedQuery(
                name = "Customer.findByUserName",
                query = "from Customer u where u.userName = :userName"
        )
})
public class Customer extends AbstractEntity<UUID> {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String userName;

        private String firstName;

        private String lastName;

        private String email;

        private Address address;

        private Builder() {
        }

        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder withEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder withAddress(Address address) {
            this.address = address;
            return this;
        }

        public Customer build() {
            Customer customer = new Customer();
            customer.userName = this.userName;
            customer.firstName = this.firstName;
            customer.lastName = this.lastName;
            customer.email = this.email;
            customer.address = this.address;
            return customer;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_name", length = 15, nullable = false, unique = true)
    private String userName;

    @Column(name = "first_name", length = 45)
    private String firstName;

    @Column(name = "last_name", length = 45)
    private String lastName;

    @Column(length = 128)
    private String email;

    @Embedded
    private Address address;

    @Override
    public UUID getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Address getAddress() {
        return address;
    }

    public String getEmail() {
        return email;
    }
}
