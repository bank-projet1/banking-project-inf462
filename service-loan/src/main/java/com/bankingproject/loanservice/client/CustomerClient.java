package com.bankingproject.loanservice.client;

import com.bankingproject.loanservice.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class CustomerClient {

    private final RestTemplate restTemplate;
    private final String customerServiceUrl;

    public CustomerClient(RestTemplate restTemplate,
                          @Value("${external.service.customer-url:http://localhost:8085}") String customerServiceUrl) {
        this.restTemplate = restTemplate;
        this.customerServiceUrl = customerServiceUrl;
    }

    public CustomerDTO getCustomerById(Long customerId) {
        try {
            return restTemplate.getForObject(customerServiceUrl + "/users/{id}", CustomerDTO.class, customerId);
        } catch (HttpClientErrorException.NotFound ex) {
            return null;
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Unable to reach auth-service at " + customerServiceUrl, ex);
        }
    }
}
