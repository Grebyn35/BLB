package com.balumbo.blb.security;

import com.balumbo.blb.model.User;
import com.balumbo.blb.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


public class CustomService implements UserDetailsService {

    @Autowired
    UserRepository repository;



    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user  = repository.findByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found") ;
        }
        CustomUserDetails customUser = new CustomUserDetails(user);
        return customUser;
    }


}
