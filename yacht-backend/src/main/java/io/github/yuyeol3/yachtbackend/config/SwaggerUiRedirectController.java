package io.github.yuyeol3.yachtbackend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("api-swagger")
public class SwaggerUiRedirectController {
    @GetMapping("/swagger-ui")
    public String redirectSwaggerUi() {
        return "redirect:/swagger-ui.html";
    }
}
