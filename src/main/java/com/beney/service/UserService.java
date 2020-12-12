package com.beney.service;

import com.beney.framework.annotations.Component;
import com.beney.framework.annotations.Lazy;

@Component("userService")
@Lazy
public class UserService {

    public String getUserName() {
        return "Beney";
    }
}
