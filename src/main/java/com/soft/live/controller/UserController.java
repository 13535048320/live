package com.soft.live.controller;

import com.soft.live.model.User;
import com.soft.live.service.UserService;
import com.soft.live.util.ResultMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/live")
public class UserController {
    private Logger LOGGER = LogManager.getLogger("SCHEDULED_CHECK_LOGGER");

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserService userService;

    @PostMapping("/on_publish")
    public @ResponseBody
    ResultMessage on_publish(HttpServletRequest request, HttpServletResponse response, String username, String password) {
//        Enumeration<String> enu = request.getParameterNames();
//        while(enu.hasMoreElements()){
//            String paraName=(String)enu.nextElement();
//            System.out.println(paraName+": "+request.getParameter(paraName));
//        }
        LOGGER.info("on_publish : username = {} , password = {}", username, password);
        ResultMessage resultMessage;
        User record = userService.on_public(username);
        if (record == null) {
            resultMessage = ResultMessage.build(500, "auth error");
            response.setHeader("liveAuth", "authCode error");
            response.setStatus(500);
        } else if (record.getPassword().equals(password)) {
            resultMessage = ResultMessage.ok(record);
        } else {
            resultMessage = ResultMessage.build(500, "auth error");
            response.setHeader("liveAuth", "authCode error");
            response.setStatus(500);
        }
        return resultMessage;
    }

    @PostMapping("/on_publish_done")
    public @ResponseBody
    ResultMessage on_publish_done(HttpServletRequest request, HttpServletResponse response) {
        LOGGER.info("on_publish_done : ");
        ResultMessage resultMessage = null;
        resultMessage = ResultMessage.ok(null);
        return resultMessage;
    }

    @PostMapping("/on_record_done")
    public @ResponseBody
    ResultMessage on_record_done(HttpServletRequest request, HttpServletResponse response) {
        LOGGER.info("on_record_done : ");
        ResultMessage resultMessage = null;
        resultMessage = ResultMessage.ok(null);
        return resultMessage;
    }
}
