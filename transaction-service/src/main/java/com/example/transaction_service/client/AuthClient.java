package com.example.transaction_service.client;

import com.example.transaction_service.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "auth-service", url = "${external.service.auth-url:http://localhost:8084}")
public interface AuthClient {

    @GetMapping("/users/search")
    UserResponse findUserByName(@RequestParam("name") String name);

    @GetMapping("/users/admins")
    List<UserResponse> findAdministrators();
}
