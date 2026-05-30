package io.github.yuyeol3.yachtbackend.user;

import io.github.yuyeol3.yachtbackend.GenericDataResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<GenericDataResponse<Long>> create(@RequestBody @Valid UserCreateRequest userCreateRequest) {
        Long id = userService.create(userCreateRequest);
        return new ResponseEntity<>(new GenericDataResponse<>(id), HttpStatus.CREATED);
    }

    @GetMapping("/{nickname}")
    public ResponseEntity<UserResponse> get(@PathVariable @Valid @NotBlank String nickname) {
        UserResponse u = userService.findByNickname(nickname);
        return new ResponseEntity<>(u, HttpStatus.OK);
    }


    @DeleteMapping("/me")
    public ResponseEntity<GenericDataResponse<Void>> delete(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        userService.delete(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
