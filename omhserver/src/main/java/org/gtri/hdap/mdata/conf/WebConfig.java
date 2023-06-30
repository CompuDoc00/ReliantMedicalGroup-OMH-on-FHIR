package org.gtri.hdap.mdata.conf;

import org.gtri.hdap.mdata.controller.CreateSessionInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@Configuration
//@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
/*
    @Bean
    CreateSessionInterceptor createSession() {

        return new CreateSessionInterceptor();
    }
*/
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CreateSessionInterceptor())
            .addPathPatterns("/DocumentReference", "/Observation", "/authorize/**", "/Binary/**", "/Binary")
            .excludePathPatterns("/resources/**", "/login", "/shimmerAuthentication", "/");
        // in case there are static files with /resources/ mapping
        // and a pre-login page is served with /login mapping
        // ? matches one character
        //* matches zero or more characters
        //** matches zero or more directories in a path
    }

}

