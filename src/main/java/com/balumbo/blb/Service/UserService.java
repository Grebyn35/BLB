package com.balumbo.blb.Service;

import com.balumbo.blb.model.User;

import java.util.List;

public interface UserService {
    public void save(User user);
    public String enCryptedPassword(User user);
    public List<User> getAll();

}
