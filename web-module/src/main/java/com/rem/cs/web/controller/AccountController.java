package com.rem.cs.web.controller;

import com.rem.cs.data.jpa.token.Token;
import com.rem.cs.data.jpa.token.TokenRepository;
import com.rem.cs.data.jpa.user.User;
import com.rem.cs.data.jpa.user.UserService;
import com.rem.cs.exception.EntityException;
import com.rem.cs.web.dto.UserDto;
import com.rem.cs.web.event.UserEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Calendar;
import java.util.HashMap;

@SessionAttributes({"verifiedUser", "cartItems"})
@Controller
public class AccountController {

    private UserService userService;
    private TokenRepository tokenRepository;
    private MessageSource messageSource;
    private HttpServletRequest request;
    private ApplicationEventPublisher publisher;

    @Autowired
    public AccountController(UserService userService, TokenRepository tokenRepository, MessageSource
            messageSource, HttpServletRequest request, ApplicationEventPublisher publisher) {
        this.userService = userService;
        this.userService = userService;
        this.tokenRepository = tokenRepository;
        this.messageSource = messageSource;
        this.request = request;
        this.publisher = publisher;
    }

    @ModelAttribute
    public void init(Model model) {
        model.addAttribute("user", new UserDto());
        model.addAttribute("token", null);
        model.addAttribute("title", messageSource.getMessage("reg.title", null, null));
        model.addAttribute("subtitle", messageSource.getMessage("reg.subtitle", null, null));
    }

    @GetMapping("/login")
    public String getLogin() {
        return "login";
    }

    @GetMapping("/register")
    public String getRegister() {
        return "sign-up";
    }

    @PostMapping("/register")
    public String onRegister(@ModelAttribute("user") @Valid UserDto userDto, BindingResult result, Model model) {
        final HashMap<String, Object> hashMap = new HashMap<>();

        if (result.hasErrors()) {
            return "sign-up";
        } else {
            hashMap.put("do", UserEvent.CREATE_USER);
            hashMap.put("user", userDto);
            hashMap.put("baseUrl", request.getRequestURL().toString().replace(request.getRequestURI(), "/"));
            publisher.publishEvent(new UserEvent(hashMap));
            model.addAttribute("email", userDto.getEmail());
            return "validating";
        }
    }

    @GetMapping("register/activate")
    public String onActivate(@RequestParam(value = "uid", required = false) String userId,
                             @RequestParam(value = "tid", required = false) String tokenId) {
        try {
            final HashMap<String, Object> hashMap = new HashMap<>();
            final User user = userService.findById(userId);
            final Token token = tokenRepository.findById(tokenId).orElse(null);
            final Calendar calendar = Calendar.getInstance();

            if (user.isActivated()) {
                return "error/500";
            }
            if (token == null || !(token.getExpiryDate().getTime() - calendar.getTime().getTime() > 0)) {
                return "error/500";
            }
            hashMap.put("do", UserEvent.ACTIVATE_USER);
            hashMap.put("user", user);
            hashMap.put("token", token);
            publisher.publishEvent(new UserEvent(hashMap));
            return "redirect:/catalog";
        } catch (EntityException e) {
            return "error/500";
        }
    }

    @PostMapping("register/resend-confirmation")
    public String onResendConfirmationEmail(@RequestParam(value = "email") String email, Model model) {
        try {
            final HashMap<String, Object> hashMap = new HashMap<>();
            final User user = userService.findByEmail(email);

            if (!user.isActivated()) {
                hashMap.put("do", UserEvent.RESEND_CONFIRMATION_EMAIL);
                hashMap.put("user", user);
                hashMap.put("baseUrl", request.getRequestURL().toString().replace(request.getRequestURI(), "/"));
                publisher.publishEvent(new UserEvent(hashMap));
                model.addAttribute("email", email);
                return "validating";
            } else {
                return "redirect:/login";
            }
        } catch (EntityException e) {
            return "error/500";
        }
    }

    @PostMapping("register/change-email-address")
    public String onChangeEmailAddress(@RequestParam(value = "email") String email,
                                       @RequestParam(value = "new_email") String newEmail, Model model) {
//        if (!newEmail.matches("^[A-Za-z0-9]+(\\\\.[A-Za-z0-9]+)*@\" +\n" +
//                "[A-Za-z0-9]+(\\\\.[A-Za-z0-9]+)*(\\\\.[A-Za-z0-9]+)$")) {
//            return "error/500";
//        }
        try {
            final HashMap<String, Object> hashMap = new HashMap<>();
            final User user = userService.findByEmail(email);

            hashMap.put("do", UserEvent.CHANGE_EMAIL_ADDRESS);
            hashMap.put("user", user);
            hashMap.put("email", newEmail);
            hashMap.put("baseUrl", request.getRequestURL().toString().replace(request.getRequestURI(), "/"));
            publisher.publishEvent(new UserEvent(hashMap));
            model.addAttribute("email", newEmail);
            return "validating";
        } catch (EntityException e) {
            e.printStackTrace();
            return "error/500";
        }
    }


}