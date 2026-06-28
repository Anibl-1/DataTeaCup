package com.dataplatform.api.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import org.springframework.context.annotation.Profile;

import java.util.*;

/**
 * 诊断端点 - 仅在 dev profile 下激活，用于验证各微服务控制器过滤效果
 * 访问: GET /diag/controllers
 */
@Profile("dev")
@RestController
@RequestMapping("/diag")
public class DiagnosticController {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @GetMapping("/controllers")
    public Map<String, Object> listControllers() {
        Map<RequestMappingInfo, HandlerMethod> methods = handlerMapping.getHandlerMethods();

        Set<String> controllerClasses = new TreeSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : methods.entrySet()) {
            String className = entry.getValue().getBeanType().getName();
            if (className.startsWith("com.dataplatform.")) {
                controllerClasses.add(className);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("controllerCount", controllerClasses.size());
        result.put("routeCount", methods.size());
        result.put("controllers", controllerClasses);
        return result;
    }
}
