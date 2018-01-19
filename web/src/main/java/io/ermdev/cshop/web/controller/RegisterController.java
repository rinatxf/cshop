package io.ermdev.cshop.web.controller;

import io.ermdev.cshop.business.event.MailEvent;
import io.ermdev.cshop.business.register.RegisterEvent;
import io.ermdev.cshop.business.register.RegisterSource;
import io.ermdev.cshop.business.util.MailConstructor;
import io.ermdev.cshop.data.exception.EmailExistsException;
import io.ermdev.cshop.data.exception.EntityNotFoundException;
import io.ermdev.cshop.data.exception.UnsatisfiedEntityException;
import io.ermdev.cshop.data.service.UserService;
import io.ermdev.cshop.data.service.VerificationTokenService;
import io.ermdev.cshop.data.entity.User;
import io.ermdev.cshop.data.entity.VerificationToken;
import io.ermdev.cshop.web.dto.UserDto;
import io.ermdev.cshop.web.exception.TokenException;
import io.ermdev.mapfierj.SimpleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.UUID;

@Controller
@SessionAttributes({"cartItems"})
public class RegisterController {

    private UserService userService;
    private VerificationTokenService verificationTokenService;
    private ApplicationEventPublisher publisher;
    private MessageSource messageSource;
    private MailConstructor mailConstructor;
    private SimpleMapper mapper;

    @Autowired
    public RegisterController(UserService userService, VerificationTokenService verificationTokenService,
                              ApplicationEventPublisher publisher, MessageSource messageSource,
                              MailConstructor mailConstructor, SimpleMapper mapper) {
        this.userService = userService;
        this.verificationTokenService = verificationTokenService;
        this.publisher = publisher;
        this.messageSource = messageSource;
        this.mailConstructor = mailConstructor;
        this.mapper = mapper;
    }

    @GetMapping("register")
    public String showRegister(Model model, UserDto userDto) {
        model.addAttribute("user", userDto);
        return "register";
    }

    @PostMapping("register")
    public String registerUser(@ModelAttribute("user") @Valid UserDto userDto, BindingResult result, Model model)
            throws UnsatisfiedEntityException, EntityNotFoundException {
        if(!result.hasErrors()) {
            User user = mapper.set(userDto).mapAllTo(User.class);
            user.setUsername(userDto.getEmail().split("@")[0]);
            try {
                final String url = messageSource.getMessage("cshop.url", null, null);
                final String token = UUID.randomUUID().toString();
                final RegisterSource registerSource = new RegisterSource();
                user = userService.add(user);

                registerSource.setVerificationToken(new VerificationToken(token, user));
                registerSource.setUrl(url);

                publisher.publishEvent(new RegisterEvent(registerSource));
                model.addAttribute("userId", user.getId());
            } catch (EmailExistsException e) {
                result.rejectValue("email", "message.error");
            }
        }
        if(result.hasErrors()) {
            result.rejectValue("email","message.error");
            return showRegister(model, userDto);
        }
        return showRegisterComplete(model);
    }

    @PostMapping("register/complete")
    public String showRegisterComplete(Model model){
        return "register-complete";
    }

    @GetMapping("register/complete")
    public String showRegisterComplete(Model model, @RequestParam(value = "userId", required = false) Long userId){
        if(userId==null) {
            UserDto userDto = new UserDto();
            model.addAttribute("user", userDto);
            return "register";
        } else {
            model.addAttribute("userId", userId);
            return "register-complete";
        }
    }

    @GetMapping("register/confirmation")
    public String registerConfirmation(@RequestParam("token") String token, Model model) {
        try {
            if (token == null)
                throw new TokenException("No token found.");

            final VerificationToken verificationToken = verificationTokenService.findByToken(token);
            final Calendar calendar = Calendar.getInstance();
            final long remainingTime=verificationToken.getExpiryDate().getTime() - calendar.getTime().getTime();

            if (remainingTime <= 0) {
                throw new TokenException("Token is expired");
            } else {
                Long verificationId = verificationToken.getId();
                User user = verificationToken.getUser();
                user.setEnabled(true);

                userService.updateById(user.getId(), user);
                verificationTokenService.deleteById(verificationId);
            }
            model.addAttribute("activation", true);
            return "login";
        } catch (EntityNotFoundException | TokenException e) {
            model.addAttribute("message", e.getMessage());
            return "error/403";
        }
    }

    @PostMapping("register/resend-verification")
    public String resendVerificationToken(@RequestParam("userId") Long userId, Model model)
            throws UnsupportedEncodingException, MessagingException {
        try {
            if (userId == null)
                return "register";
            final User user = userService.findById(userId);
            final VerificationToken verificationToken = new VerificationToken();

            verificationToken.setUser(user);
            if (verificationToken.getUser().getEnabled()) {
                verificationTokenService.deleteByUserId(userId);
                throw new TokenException("Your email already registered");
            } else {
                String newToken = UUID.randomUUID().toString();
                String url = messageSource.getMessage("cshop.url", null, null);

                verificationToken.setToken(newToken);
                verificationTokenService.add(verificationToken);
                publisher.publishEvent(new MailEvent(mailConstructor.constructVerificationMail(verificationToken, url, null)));

                model.addAttribute("userId", verificationToken.getUserId());
                return showRegisterComplete(model);
            }
        } catch (EntityNotFoundException | TokenException e) {
            model.addAttribute("message", e.getMessage());
            return "error/403";
        }
    }
}
