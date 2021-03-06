package com.rem.cs.web.dto;

import com.rem.cs.web.validation.annotation.Email;
import com.rem.cs.web.validation.annotation.Name;
import com.rem.cs.web.validation.annotation.Password;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private String id;

    @Name
    private String name;

    @Email
    private String email;

    @Password
    private String password;
}
