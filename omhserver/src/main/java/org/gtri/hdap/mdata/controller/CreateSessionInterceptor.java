package org.gtri.hdap.mdata.controller;

import org.springframework.web.servlet.HandlerInterceptor;
//import org.springframework.web.servlet.ModelAndView;
 
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateSessionInterceptor implements HandlerInterceptor {

    private final Logger logger = LoggerFactory.getLogger(CreateSessionInterceptor.class);
    
    // This method is called before the controller FOR /authorize and /DocumentReference and /Observation and /Binary
    // *LDG created this to ensure that a session is created before sending data back when responding to a Get
    // *LDG This fixes the error trying to write a second header because the session hadn't been created yet!
    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler) throws Exception {

        HttpSession newSession = request.getSession(true);
        // The above will create a session if one doesn't already exist

        logger.debug("Hey, we created a new Session!  SessionID = " + newSession.getId() );

        return true;

    }
/*
    @Override
    public void postHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request,
            HttpServletResponse response, Object handler, Exception ex)
            throws Exception {

    }

 */
}
