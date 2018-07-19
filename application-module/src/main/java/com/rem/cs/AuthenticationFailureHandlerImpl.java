package com.rem.cs;

import com.rem.cs.data.jpa.user.User;
import com.rem.cs.data.jpa.user.UserRepository;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

@Component
public class AuthenticationFailureHandlerImpl implements AuthenticationFailureHandler {

    final private UserRepository userRepository;

    public AuthenticationFailureHandlerImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

//    @Override
//    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
//                                        AuthenticationException exception) throws IOException {
//        final String username = request.getParameter("username");
//        final User user = userRepository.findByEmail(username).orElse(null);
//
//        if (user == null) {
//            redirectStrategy.sendRedirect(request, response, "/login?error");
//        } else if (!user.isActivated()) {
//            redirectStrategy.sendRedirect(request, response, "/login?error");
//        }
//        clearAuthenticationAttributes(request);
//    }
//
//    private void clearAuthenticationAttributes(HttpServletRequest request) {
//        HttpSession session = request.getSession(false);
//        if (session == null) {
//            return;
//        }
//        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
//    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        System.out.println("pak");

        response.setContentType("text/html; charset=utf-8");
        response.setStatus(500);
        response.getWriter().write("Incorrect email or password.");
        response.getWriter().flush();
    }
}