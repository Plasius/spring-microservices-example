package eu.miraiworks.customer;

import eu.miraiworks.amqp.RabbitMQMessageProducer;
import eu.miraiworks.clients.fraud.FraudCheckResponse;
import eu.miraiworks.clients.fraud.FraudClient;
import eu.miraiworks.clients.notification.NotificationClient;
import eu.miraiworks.clients.notification.NotificationRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final FraudClient fraudClient;
    private final RabbitMQMessageProducer rabbitMQMessageProducer;

    public void registerCustomer(CustomerRegistrationRequest customerRegistrationRequest){
        Customer customer = Customer.builder()
                .firstname(customerRegistrationRequest.firstname())
                .lastname(customerRegistrationRequest.lastname())
                .email(customerRegistrationRequest.email())
                .build();

        //todo check if email is valid
        //todo check if email is taken
        customerRepository.saveAndFlush(customer);

        FraudCheckResponse fraudCheckResponse = fraudClient.isFraudster(customer.getId());

        if(fraudCheckResponse !=null && fraudCheckResponse.isFraudster()){
            System.out.println("Fraudster detected");
            throw new IllegalStateException("fraudster");
        }

        NotificationRequest notificationRequest =
                new NotificationRequest(
                        customer.getId(),
                        customer.getEmail(),
                        String.format("Hi %s, welcome to Amigoscode...",
                                customer.getFirstname())
                );

        rabbitMQMessageProducer.publish(
                notificationRequest,
                "internal.exchange",
                "internal.notification.routing-key"
        );



    }

}
