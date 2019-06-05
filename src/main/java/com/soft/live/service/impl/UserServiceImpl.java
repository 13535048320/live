package com.soft.live.service.impl;

import com.soft.live.mapper.UserMapper;
import com.soft.live.model.User;
import com.soft.live.service.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private static Logger LOGGER = LogManager.getLogger("SCHEDULED_CHECK_LOGGER");

    @Autowired
    private UserMapper userMapper;

    @Override
    public User on_public(String username) {
        User user = new User();
        user.setUsername(username);
        return userMapper.selectOne(user);
    }
}
