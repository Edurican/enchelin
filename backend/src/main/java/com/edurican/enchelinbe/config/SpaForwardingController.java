package com.edurican.enchelinbe.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardingController {

    // 확장자 없는 GET 경로 = SPA 라우트로 간주 → index.html forward
    // /api 로 시작하는 경로는 명시적으로 제외 (first segment가 "api"인 경우 매칭 안 함)
    @RequestMapping(value = {
        "/",
        "/{path:(?!api$)[^.]*}",
        "/{x:(?!api$)[^.]*}/{y:[^.]*}",
        "/{x:(?!api$)[^.]*}/{y:[^.]*}/{z:[^.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
